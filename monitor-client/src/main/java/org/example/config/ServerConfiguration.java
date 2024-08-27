package org.example.config;

import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ConnectionConfig;
import org.example.utils.MonitorUtils;
import org.example.utils.NetUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

@Slf4j
@Configuration
public class ServerConfiguration implements ApplicationRunner {

    @Resource
    NetUtils netUtils;

    @Resource
    MonitorUtils monitor;

    @Bean
        // 启动的时候将连接的配置注册为bean
    ConnectionConfig connectionConfig() {
        log.info("正在加载服务端连接配置...");
        ConnectionConfig config = this.readConfigurationFromFile();
        if (config == null)
            config = this.registerToServer();
//        System.out.println(monitor.monitorBaseDetail());
        return config; //将值返回到Spring容器进行管理
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("正在向服务端更新基本系统信息...");
        netUtils.updateBaseDetails(monitor.monitorBaseDetail());
    }

    // 输入信息 客户端注册服务连接配置
    private ConnectionConfig registerToServer() {
        Scanner scanner = new Scanner(System.in);
        String token, address;
        do {
            log.info("请输入需要注册的服务端访问的地址. 地址类似于 'http://192.168.0.22:8080'这种写法");
            address = scanner.nextLine();
            log.info("请输入服务端生成的用户注册客户端的Token秘钥:");
            token = scanner.nextLine();
        } while (!netUtils.registerToServer(address, token));
        ConnectionConfig config = new ConnectionConfig(address, token);
        this.saveConnectionConfig(config);
        return config;
    }

    // 将配置保存在本地文件中
    private void saveConnectionConfig(ConnectionConfig config) {
        File dir = new File("config");
        if (!dir.exists() && dir.mkdir()) {
            log.info("正在创建用于保存服务端连接信息的目录已完成");
        }
        File file = new File("config/server.json");
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(JSONObject.from(config).toJSONString());
        } catch (IOException e) {
            log.error("保存配置文件时出错", e);
        }
        log.info("服务器连接配置已保存成功 ");
    }

    // 从文件加载配置
    private ConnectionConfig readConfigurationFromFile() {
        File configFile = new File("config/server.json");
        if (configFile.exists()) {
            try (FileInputStream stream = new FileInputStream(configFile)) {
                String raw = new String(stream.readAllBytes(), StandardCharsets.UTF_8); // 读取文件
                return JSONObject.parseObject(raw).to(ConnectionConfig.class);
            } catch (IOException e) {
                log.error("读取配置文件时出错", e);
            }
        }
        return null;
    }

}
