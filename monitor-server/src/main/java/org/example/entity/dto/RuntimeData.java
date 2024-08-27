package org.example.entity.dto;

import com.influxdb.annotations.Column;
import com.influxdb.annotations.Measurement;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

/**
 * 运行时数据实体类
 */
@Data
@Measurement(name = "runtime")
public class RuntimeData {

    @Column(tag = true)
    int clientId;

    @Column(timestamp = true)
    Instant timestamp; // 时间戳

    @Column
    double cpuUsage;

    @Column
    double memoryUsage;

    @Column
    double diskUsage;

    @Column
    double networkUpload;

    @Column
    double networkDownload;

    @Column
    double diskRead; // 磁盘读写速度

    @Column
    double diskWrite;
}
