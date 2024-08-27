package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 客户端细节实体类
 */
@Data
@TableName("db_client_detail")
public class ClientDetail {
    @TableId
    Integer id;
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
