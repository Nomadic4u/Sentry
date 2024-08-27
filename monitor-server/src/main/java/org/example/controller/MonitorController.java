package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.RenameClientVO;
import org.example.entity.vo.request.RenameNodeVO;
import org.example.entity.vo.request.RuntimeDetailVO;
import org.example.entity.vo.request.SshConnectionVO;
import org.example.entity.vo.response.*;
import org.example.service.AccountService;
import org.example.service.ClientService;
import org.example.utils.Const;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用来与前端交互
 */
@RestController
@RequestMapping("/api/monitor")
public class MonitorController {

    @Resource
    ClientService service;

    @Resource
    AccountService accountService;

    /**
     * 首页服务器卡片展示
     *
     * @param userId   用户的id
     * @param userRole 用户权限
     * @return 服务器卡片信息列表
     */
    @GetMapping("/list")
    public RestBean<List<ClientPreviewVO>> listAllClient(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        List<ClientPreviewVO> list = service.listAllClient(); // 先获取所有的客户端
        if (this.isAdminAccount(userRole))
            return RestBean.success(list);
        List<Integer> ids = this.accountAccessClient(userId); // 获取该用户能访问的客户机列表
        return RestBean.success(list.stream()
                .filter(vo -> ids.contains(vo.getId())) // 这里采用过滤器, 去除用户没有权限去访问的主机
                .toList());
    }

    /**
     * 用于新建子用户分配主机时展示主机列表
     * @param userRole 用户权限
     * @return 可用主机列表
     */
    @GetMapping("/simple-list")
    public RestBean<List<ClientSimpleVO>> simpleClientList(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole))
            return RestBean.success(service.listSimpleList());
        return RestBean.noPermission();
    }

    /**
     * 用于修改客户端名称
     * @param vo 需要修改的客户端实体
     * @param userID 当前的用户id
     * @param userRole 当前的用户角色
     * @return 是否修改成功
     */
    @PostMapping("/rename")
    public RestBean<Void> renameClient(@RequestBody @Valid RenameClientVO vo,
                                       @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
//        先判断是否具有权限
        if (this.permissionCheck(userID, userRole, vo.getId())) {
            service.renameClient(vo);
            return RestBean.success();
        }
        return RestBean.noPermission();
    }

    /**
     * 客户端节点重命名
     * @param vo 重命名节点实体
     * @param userID 用户id
     * @param userRole 角色用户
     * @return 重命名是否成功
     */
    @PostMapping("/node")
    public RestBean<Void> renameNode(@RequestBody @Valid RenameNodeVO vo,
                                     @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                     @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userID, userRole, vo.getId())) {
            service.renameNode(vo);
            return RestBean.success();
        }
        return RestBean.noPermission();
    }


    /**
     * 用于在卡片内展示客户端详细信息
     * @param clientId 客户端id
     * @param userID 用户id
     * @param userRole 用户角色
     * @return 客户端详细信息
     */
    @GetMapping("/details")
    public RestBean<ClientDetailsVO> details(int clientId,
                                             @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                             @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userID, userRole, clientId)) {
            return RestBean.success(service.clientDetails(clientId));
        }
        return RestBean.noPermission();
    }

    /**
     * 用于卡片内展示客户端历史运行信息
     * @param clientId 客户端id
     * @param userId 用户id
     * @param userRole 用户角色
     * @return 客户端历史运行信息
     */
    @GetMapping("/runtime-history")
    public RestBean<RuntimeHistoryVO> runtimeDetailsHistory(int clientId,
                                                            @RequestAttribute(Const.ATTR_USER_ID) int userId,
                                                            @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        System.out.println("userId: " + userId + " userRole: " + userRole + " clientId: " + clientId);
        if (this.permissionCheck(userId, userRole, clientId)) {
            return RestBean.success(service.clientRuntimeDetailsHistory(clientId));
        }
        return RestBean.noPermission();
    }

    /**
     * 用于卡片内展示客户端当前运行信息
     * @param clientId 客户端id
     * @param userID 用户id
     * @param userRole 用户角色
     * @return 客户端当前运行信息
     */
    @GetMapping("/runtime-now")
    public RestBean<RuntimeDetailVO> runtimeDetailsNow(int clientId,
                                                       @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                                       @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userID, userRole, clientId)) {
            return RestBean.success(service.clientRuntimeDetailsNow(clientId));
        }
        return RestBean.noPermission();
    }

    /**
     * 用于添加新主机页面, 显示token
     * @param userRole
     * @return
     */
    @GetMapping("/register")
    public RestBean<String> registerToken(@RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole))
            return RestBean.success(service.registerToken());
        return RestBean.noPermission();
    }

    /**
     * 删除指定的客户端
     * @param clientId 客户端id
     * @param userRole 用户角色
     * @return 是否删除成功
     */
    @GetMapping("/delete")
    public RestBean<String> deleteClient(int clientId,
                                         @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.isAdminAccount(userRole)) {
            service.deleteClient(clientId);
            return RestBean.success();
        }
        return RestBean.noPermission();
    }

    /**
     * 保存ssh链接信息, 下次无需再次输入
     * @param vo ssh连接实体
     * @param userID 用户id
     * @param userRole 用户角色
     * @return 是否成功
     */
    @PostMapping("/ssh-save")
    public RestBean<Void> saveSshConnection(@RequestBody @Valid SshConnectionVO vo,
                                            @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                            @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userID, userRole, vo.getId())) {
            service.saveClientSshConnection(vo);
            return RestBean.success();
        }
        return RestBean.noPermission();
    }

    /**
     * 获取ssh连接信息
     * @param clientId 客户端id
     * @param userID 用户id
     * @param userRole 用户角色
     * @return ssh实体
     */
    @GetMapping("/ssh")
    public RestBean<SshSettingVO> sshSettings(int clientId,
                                              @RequestAttribute(Const.ATTR_USER_ID) int userID,
                                              @RequestAttribute(Const.ATTR_USER_ROLE) String userRole) {
        if (this.permissionCheck(userID, userRole, clientId)) {
            return RestBean.success(service.sshSettings(clientId));
        }
        return RestBean.noPermission();
    }


    /**
     * 获取该用户能访问的服务器ID列表
     *
     * @param uid 用户id
     * @return 服务器id列表
     */
    private List<Integer> accountAccessClient(int uid) {
        Account account = accountService.getById(uid);
        return account.getClientList();
    }

    /**
     * 判断是否为管理员账户
     */
    private boolean isAdminAccount(String role) {
        // Spring的role一般带有前缀"ROLE_", 这里要先去掉
        role = role.substring(5);
        return Const.ROLE_ADMIN.equals(role);
    }

    /**
     * 判断是否具有权限
     *
     * @param uid
     * @param role
     * @param clientId
     * @return
     */
    private boolean permissionCheck(int uid, String role, int clientId) {
        if (this.isAdminAccount(role))
            return true;
        return this.accountAccessClient(uid).contains(clientId);
    }

}


















