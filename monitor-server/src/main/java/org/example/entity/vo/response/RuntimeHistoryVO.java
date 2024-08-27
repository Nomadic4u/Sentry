package org.example.entity.vo.response;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class RuntimeHistoryVO {

    double disk; // 磁盘总容量

    double memory; // 内存总容量

    List<JSONObject> list = new LinkedList<>();

}
