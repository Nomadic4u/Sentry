package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.example.entity.RestBean;
import org.example.entity.vo.request.ChangePasswordVO;
import org.example.entity.vo.request.CreateSubAccountVO;
import org.example.entity.vo.request.ModifyEmailVO;
import org.example.entity.vo.response.SubAccountVO;
import org.example.service.AccountService;
import org.example.utils.Const;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户模块
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    AccountService service;

    @PostMapping("/change-password")
    public RestBean<Void> changePassword(@RequestBody @Valid ChangePasswordVO vo,
                                         @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        System.out.println("VO :" + vo);
        System.out.println("userId: " + userId);
        return service.changePassword(userId, vo.getPassword(), vo.getNew_password()) ?
                RestBean.success() : RestBean.failure(401, "原密码输入错误");
    }

    @PostMapping("/modify-email")
    public RestBean<Void> modify_email(@RequestAttribute(Const.ATTR_USER_ID) int userId,
                                       @RequestBody @Valid ModifyEmailVO vo) {
        String result = service.modifyEmail(userId, vo);
        if(result != null)
            return RestBean.failure(401, result);
        return RestBean.success();
    }

    /**
     * 创建子用户
     * @param vo 创建用户实体类
     * @return 是否创建成功
     */
    @PostMapping("/sub/create")
    public RestBean<Void> createSubAccount(@RequestBody @Valid CreateSubAccountVO vo) {
        service.createSubAccount(vo);
        return RestBean.success();
    }

    /**
     * 删除子用户
     */
    @GetMapping("/sub/delete")
    public RestBean<Void> deleteSubAccount(int uid,
                                           @RequestAttribute(Const.ATTR_USER_ID) int userId) {
        if (uid == userId) // 防止自己删自己
            return RestBean.failure(401, "非法参数");
        service.deleteSubAccount(uid);
        return RestBean.success();
    }

    /**
     * 获取当前账户的主机列表
     */
    @GetMapping("/sub/list")
    public RestBean<List<SubAccountVO>> subAccountList() {
        return RestBean.success(service.listSubAccount());
    }

}
