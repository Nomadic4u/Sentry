package org.example.utils;


import com.alibaba.fastjson2.JSONObject;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.BaseDetail;
import org.example.entity.ConnectionConfig;
import org.example.entity.Response;
import org.example.entity.RuntimeDetail;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * 网络工具, 主要用来发请求
 */

@Slf4j
@Component
public class NetUtils {

    @Lazy // 用的时候再去取, 避免循环引用
    @Resource
    ConnectionConfig config;

    // jdk11提供的网络工具
    private final HttpClient client = HttpClient.newHttpClient();

    public boolean registerToServer(String address, String token) {
        log.info("正在向服务端注册, 请稍后");
        Response response = this.doGet("/register", address, token);
        if (response.sucess()) {
            log.info("客户端注册已完成! ");
        } else {
            log.error("客户端注册失败: {}", response.message());
        }
        return response.sucess();
    }

    private Response doGet(String url) {
        return this.doGet(url, config.getAdress(), config.getToken());
    }


    // 给服务端发送请求
    private Response doGet(String url, String address, String token) {
        try {
            HttpRequest request = HttpRequest.newBuilder().GET()
                    .uri(new URI(address + "/monitor" + url))
                    .header("Authorization", token)
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("TTT: "+ response.body());
            return JSONObject.parseObject(response.body()).to(Response.class);
        } catch (Exception e) {
            log.error("在发起服务端请求时出现错误", e);
            return Response.errorResponse(e);
        }
    }

    public void updateBaseDetails(BaseDetail detail) {
        Response response = this.doPost("/detail", detail);
        System.out.println("request: : "+ detail);
        System.out.println("response:  "+ response);
        if(response.sucess()) {
            log.info("系统基本信息已更新完成");
        } else {
            log.error("系统基本信息更新失败, {}", response.message());
        }
    }


    public void updateBaseDetails(RuntimeDetail detail) {
        Response response = this.doPost("/runtime", detail);
        if(!response.sucess()) {
            log.error("更新运行时状态, 接收到服务端的异常响应内容: {}", response.message());
        }
    }


    private Response doPost(String url, Object data) {
        try {
            String rawData = JSONObject.from(data).toJSONString();
            HttpRequest request = HttpRequest.newBuilder().POST(HttpRequest.BodyPublishers.ofString(rawData))
                    .uri(new URI(config.getAdress() + "/monitor" + url))
                    .header("Authorization", config.getToken())
                    .header("Content-Type", "application/json")
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return JSONObject.parseObject(response.body()).to(Response.class);
        } catch (Exception e) {
            log.error("在发起服务端请求时出现错误", e);
            return Response.errorResponse(e);
        }
    }
}
