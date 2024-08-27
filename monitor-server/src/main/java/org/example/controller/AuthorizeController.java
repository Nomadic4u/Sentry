package org.example.controller;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import org.example.entity.RestBean;
import org.example.entity.vo.request.ConfirmResetVO;
import org.example.entity.vo.request.EmailResetVO;
import org.example.service.AccountService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.function.Supplier;

@Validated
@RestController
@RequestMapping("/api/auth")
public class AuthorizeController {
    @Resource
    AccountService accountService;

    /**
     * 请求邮箱验证码
     *
     * @param email   请求邮件
     * @param type    类型
     * @param request 请求
     * @return 是否请求成功
     */
    @GetMapping("ask-code")
    public RestBean<Void> askVerifyCode(@RequestParam @Email String email, // 参数校验
                                        @RequestParam @Pattern(regexp = "(reset|modify)") String type, // type代表这是重置密码的邮件还是重置邮件的
                                        HttpServletRequest request) { // 取IP地址

        return this.messageHandle(() -> accountService.registerEmailVerifyCode(type, email, request.getRemoteAddr())); // 这里是直接将返回结果作为参数

    }


    /**
     * 执行密码重置确认, 检查验证码是否正确
     *
     * @param vo 密码重置信息
     * @return 是否操作成功
     */
    @PostMapping("/reset-confirm")
    public RestBean<Void> resetConfirm(@RequestBody @Valid ConfirmResetVO vo) {
        return this.messageHandle(() -> accountService.resetConfirm(vo));
    }

    /**
     * 重置密码操作
     *
     * @param vo 密码重置信息
     * @return 是否操作成功
     */
    @PostMapping("/reset-password")
    public RestBean<Void> resetPassword(@RequestBody @Valid EmailResetVO vo) {
        System.out.println(vo);
        return this.messageHandle(() -> accountService.resetEmailAccountPassword(vo));
    }


//    private <T> RestBean<Void> messageHandle(T vo, Function<T, String> function) {
//        return messageHandle(() -> function.apply(vo));
//    }


    /**
     * 针对于返回值为String作为错误信息的方法进行统一处理
     *
     * @param action 具体操作
     * @param <T>    响应结果类型
     * @return 响应结果
     */
    private <T> RestBean<T> messageHandle(Supplier<String> action) {
        String message = action.get();
        return message == null ? RestBean.success() : RestBean.failure(400, message);
    }


}
