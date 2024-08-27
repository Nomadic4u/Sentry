package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.example.entity.BaseData;

/**
 * 客户端ssh连接实体类
 */
@Data
@TableName("db_client_ssh")
public class ClientSsh implements BaseData {
    @TableId
    Integer id;

    Integer port;

    String username;

    String password;
}
