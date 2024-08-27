package org.example.entity.vo.response;

import lombok.Data;

@Data
public class ClientPreviewVO {
    int id; // 服务器id
    boolean online; // 运行的状态
    String name;
    String location;
    String osName;
    String osVersion;
    String ip;
    String cpuName;
    int cpuCore;
    double memory;
    double cpuUsage;
    double memoryUsage;
    double networkUpload;
    double networkDownload;
}
