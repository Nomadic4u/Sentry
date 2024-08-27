package org.example.entity.dto;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.BaseData;

import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * 用户实体类
 */
@Data
@TableName("db_account")
@AllArgsConstructor
@NoArgsConstructor
public class Account implements BaseData {
    @TableId(type = IdType.AUTO) // 自动递增
    Integer id;
    String username;
    String password;
    String email;
    String role;
    Date registerTime;
    String clients;

    public List<Integer> getClientList() {
        if(clients == null)
            return Collections.emptyList();
        return JSONArray.parse(clients).toJavaList(Integer.class);
    }
}
