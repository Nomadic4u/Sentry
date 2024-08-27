package org.example.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true) // 链式调用打开
public class BaseDetail {
    String osArch; //操作系统架构
    String osName; //操作系统名称
    String osVersion; // 操作系统版本
    int osBit; // 系统位数
    String cpuName;
    int cpuCore;
    double memory;
    double disk; // 硬盘容量
    String ip;
}
