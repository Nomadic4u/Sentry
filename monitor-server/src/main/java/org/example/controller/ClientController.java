package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.dto.Client;
import org.example.entity.vo.request.ClientDetailVO;
import org.example.entity.vo.request.RuntimeDetailVO;
import org.example.service.ClientService;
import org.example.utils.Const;
import org.springframework.web.bind.annotation.*;

/**
 * 用于客户机发送HTTP请求
 */
@RestController
@RequestMapping("/monitor")
public class ClientController {

    @Resource
    ClientService clientService;

    /**
     * 用户客户端注册
     * @param token 服务器上的token
     * @return 注册是否成功, token是否有效
     */

    @GetMapping("/register")
    public RestBean<Void> registerClient(@RequestHeader("Authorization") String token) {
        return clientService.verifyAndRegister(token) ?
                RestBean.success() : RestBean.failure(401, "token无效, 注册失败");
    }

    /**
     * 用户客户端给服务端发送详细信息
     * @param client 客户端实体
     * @param vo 客户端详细信息
     * @return 是否发送成功
     */
    @PostMapping("/detail")
    public RestBean<Void> updateClientDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                              @RequestBody @Valid ClientDetailVO vo) {
        clientService.updateClientDetails(vo, client);
        return RestBean.success();
    }

    /**
     * 用于客户端给服务端发送运行时信息
     * @param client 客户端实体
     * @param vo 运行时信息
     * @return 是否发送成功
     */
    @PostMapping("/runtime")
    public RestBean<Void> updateRuntimeDetails(@RequestAttribute(Const.ATTR_CLIENT) Client client,
                                               @RequestBody @Valid RuntimeDetailVO vo) {
        clientService.updateRuntimeDetails(vo, client);
        return RestBean.success();
    }

}
