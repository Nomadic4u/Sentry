package org.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RuntimeDetailVO {
    @NotNull
    long timestamp; // 时间戳

    @NotNull
    double cpuUsage;

    @NotNull
    double memoryUsage;

    @NotNull
    double diskUsage;

    @NotNull
    double networkUpload;

    @NotNull
    double networkDownload;

    @NotNull
    double diskRead; // 磁盘读写速度

    @NotNull
    double diskWrite;
}
