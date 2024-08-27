package org.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class RuntimeDetail {

    long timestamp; // 时间戳

    double cpuUsage;

    double memoryUsage;

    double diskUsage;

    double networkUpload;

    double networkDownload;

    double diskRead; // 磁盘读写速度

    double diskWrite;

}
