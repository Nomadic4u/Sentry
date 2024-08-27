package org.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.example.entity.BaseDetail;
import org.example.entity.RuntimeDetail;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HWDiskStore;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;
import oshi.software.os.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Properties;

@Slf4j
@Component
public class MonitorUtils {

    private final SystemInfo info = new SystemInfo(); // 获取硬件信息
    private final Properties properties = System.getProperties();


    public BaseDetail monitorBaseDetail() {
        OperatingSystem os = info.getOperatingSystem(); // 获取操作系统信息
        HardwareAbstractionLayer hardware = info.getHardware();
        double memory = hardware.getMemory().getTotal() / 1024.0 / 1024 / 1024; //返回的是字节
        double diskSize = Arrays.stream(File.listRoots()).mapToLong(File::getTotalSpace).sum() / 1024.0 / 1024 / 1024; // 使用JDK8的流API计算磁盘大小
//        String ip = Objects.requireNonNull(this.findNetworkInterface(hardware)).getIPv4addr()[0];
        // 获取公网ip
        String[] cmds={"curl","icanhazip.com"};//必须分开写，不能有空格

        String ip = IPUtils.execCurl(cmds).trim();
        return new BaseDetail()
                .setOsArch(properties.getProperty("os.arch"))
                .setOsName(os.getFamily())
                .setOsVersion(os.getVersionInfo().getVersion())
                .setOsBit(os.getBitness())
                .setCpuName(hardware.getProcessor().getProcessorIdentifier().getName())
                .setCpuCore(hardware.getProcessor().getLogicalProcessorCount()) //逻辑核心数
                .setMemory(memory)
                .setDisk(diskSize)
                .setIp(ip);
    }

    // 获取运行时的数据
    public RuntimeDetail monitorRuntimeDetail() {
        double statisticTime = 0.5; // 统计时间, 由于oshi框架进度不是很够, 返回的数据其实是一段时间的平均值, 所以要给出统计时间
        try {
            HardwareAbstractionLayer hardware = info.getHardware(); // 获取硬件信息
            NetworkIF networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware)); // 获取网络信息
            CentralProcessor processor = hardware.getProcessor(); // 获取CPU信息
            double upload = networkInterface.getBytesSent(), download = networkInterface.getBytesRecv(); // 获取网络上传和下载速度
            double read = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum();
            double write = hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum();
            long[] ticks = processor.getSystemCpuLoadTicks(); // 获取CPU时钟信息

            Thread.sleep((long) (statisticTime * 1000)); // 睡眠0.5秒后再去获取

            networkInterface = Objects.requireNonNull(this.findNetworkInterface(hardware));
            upload = (networkInterface.getBytesSent() - upload) / statisticTime;
            download = (networkInterface.getBytesRecv() - download) / statisticTime;
            read = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getReadBytes).sum() - read) / statisticTime;
            write = (hardware.getDiskStores().stream().mapToLong(HWDiskStore::getWriteBytes).sum() - write) / statisticTime;

            double memory = (hardware.getMemory().getTotal() - hardware.getMemory().getAvailable()) / 1024.0 / 1024 / 1024;
            double disk = Arrays.stream(File.listRoots())
                    .mapToLong(file -> file.getTotalSpace() - file.getFreeSpace()).sum() / 1024.0 / 1024 / 1024;
            return new RuntimeDetail()
                    .setCpuUsage(this.calculateCpuUsage(processor, ticks))
                    .setMemoryUsage(memory)
                    .setDiskUsage(disk)
                    .setNetworkUpload(upload / 1024)
                    .setNetworkDownload(download / 1024)
                    .setDiskRead(read / 1024 / 1024)
                    .setDiskWrite(write / 1024 / 1024)
                    .setTimestamp(new Date().getTime());
        } catch (Exception e) {
            log.error("读取运行时数据出现问题", e);
        }
        return null;

    }

    // 获取CPU情况
    private double calculateCpuUsage(CentralProcessor processor, long[] prevTicks) {
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[CentralProcessor.TickType.NICE.getIndex()]
                - prevTicks[CentralProcessor.TickType.NICE.getIndex()];
        long irq = ticks[CentralProcessor.TickType.IRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.IRQ.getIndex()];
        long softIrq = ticks[CentralProcessor.TickType.SOFTIRQ.getIndex()]
                - prevTicks[CentralProcessor.TickType.SOFTIRQ.getIndex()];
        long steal = ticks[CentralProcessor.TickType.STEAL.getIndex()]
                - prevTicks[CentralProcessor.TickType.STEAL.getIndex()];
        long cSys = ticks[CentralProcessor.TickType.SYSTEM.getIndex()]
                - prevTicks[CentralProcessor.TickType.SYSTEM.getIndex()];
        long cUser = ticks[CentralProcessor.TickType.USER.getIndex()]
                - prevTicks[CentralProcessor.TickType.USER.getIndex()];
        long ioWait = ticks[CentralProcessor.TickType.IOWAIT.getIndex()]
                - prevTicks[CentralProcessor.TickType.IOWAIT.getIndex()];
        long idle = ticks[CentralProcessor.TickType.IDLE.getIndex()]
                - prevTicks[CentralProcessor.TickType.IDLE.getIndex()];
        long totalCpu = cUser + nice + cSys + idle + ioWait + irq + softIrq + steal;
        return (cSys + cUser) * 1.0 / totalCpu;
    }


    // 获取网卡信息, IP
    private NetworkIF findNetworkInterface(HardwareAbstractionLayer hardware) {
        try {
            for (NetworkIF network : hardware.getNetworkIFs()) {
                String[] ipv4Addr = network.getIPv4addr();
                NetworkInterface ni = network.queryNetworkInterface();
                if (!ni.isLoopback() && !ni.isPointToPoint() && ni.isUp() && !ni.isVirtual()
                        && (ni.getName().startsWith("eth") || ni.getName().startsWith("en"))
                        && ipv4Addr.length > 0) {
                    return network;
                }
            }
        } catch (IOException e) {
            log.error("读取网络接口信息时出错", e);
        }
        return null;
    }

}









