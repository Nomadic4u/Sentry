package org.example.entity.vo.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientDetailVO {
    @NotNull
    String osArch; //操作系统架构

    @NotNull
    String osName; //操作系统名称

    @NotNull
    String osVersion; // 操作系统版本

    @NotNull
    int osBit; // 系统位数

    @NotNull
    String cpuName;

    @NotNull
    int cpuCore;

    @NotNull
    double memory;

    @NotNull
    double disk; // 硬盘容量

    @NotNull
    String ip;
}
