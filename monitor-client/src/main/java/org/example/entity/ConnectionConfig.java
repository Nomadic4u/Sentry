package org.example.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConnectionConfig {
    String adress; // 服务端的地址
    String token;
}
