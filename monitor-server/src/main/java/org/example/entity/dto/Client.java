package org.example.entity.dto;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.example.entity.BaseData;
import java.util.Date;

/**
 * 客户端基础信息实体类
 */
@Data
@TableName("db_client")
@AllArgsConstructor
public class Client implements BaseData {
    @TableId
    Integer id;

    String name;

    String token;

    String location;

    String node;

    Date registerTime;
}
