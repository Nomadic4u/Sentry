package org.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.ConfirmResetVO;
import org.example.entity.vo.request.CreateSubAccountVO;
import org.example.entity.vo.request.EmailResetVO;
import org.example.entity.vo.request.ModifyEmailVO;
import org.example.entity.vo.response.SubAccountVO;
import org.example.mapper.AccountMapper;
import org.example.service.AccountService;
import org.example.utils.Const;
import org.example.utils.FlowUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;


@Service
public class AccountServiceImpl extends ServiceImpl<AccountMapper, Account> implements AccountService {


    @Resource
    FlowUtils utils;

    @Resource
    AmqpTemplate amqpTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;

    @Resource
    PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Account account = this.findAccountByNameOrEmail(username);
        if (account == null)
            throw new UsernameNotFoundException("用户名或密码错误");
        return User
                .withUsername(username)
                .password(account.getPassword())
                .roles(account.getRole())
                .build();
    }

    @Override
    public Account findAccountByNameOrEmail(String text) {
        return this.query()
                .eq("username", text).or()
                .eq("email", text)
                .one();
    }

    // 邮箱发送验证码
    @Override
    public String registerEmailVerifyCode(String type, String email, String ip) {
        synchronized (ip.intern()) { // 这里用到了字符串池, String.intern()方法会先看字符串池中是否有这个String对象的字符串, 有的话直接返回, 没有的话就将这个对象添加到字符串池中, 并返回引用. 这个主要是为了确保在多个地方使用ip锁的时候是共用同一个对象
            if (!this.verifyLimit(ip)) {
                return "请求频繁, 请稍后再试";
            }
            // 生成验证码
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type", type, "email", email, "code", code);
            amqpTemplate.convertAndSend("mail", data); // 放入mq队列
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES); // 放入redis
            return null;
        }

    }


    // 重置验证码确认
    @Override
    public String resetConfirm(ConfirmResetVO vo) {
        String email = vo.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERIFY_EMAIL_DATA + email);
        if(code == null)
            return "请先获取验证码";
        if(!code.equals(vo.getCode()))
            return "验证码输入错误, 请重新输入";
        return null;
    }

    // 登录页面用户重置密码
    @Override
    public String resetEmailAccountPassword(EmailResetVO vo) {
        String email = vo.getEmail();
        String verify = this.resetConfirm(new ConfirmResetVO(email, vo.getCode()));
        if(verify != null) {
            return verify;
        }
        String password = passwordEncoder.encode(vo.getPassword());
        boolean update = this.update().eq("email", email).set("password", password).update();
        if(update) {
            stringRedisTemplate.delete(Const.VERIFY_EMAIL_DATA + email);
        }
        return null;
    }

    // 安全页面修改密码
    @Override
    public boolean changePassword(int id, String oldPassword, String newPassword) {
        Account account = this.getById(id);
        String password = account.getPassword();
        System.out.println("account: " + account);
        System.out.println("pass: " + password);
        if(!passwordEncoder.matches(oldPassword, password)) {
            return false;
        }

        this.update(Wrappers.<Account>update().eq("id", id)
                .set("password", passwordEncoder.encode(newPassword)));
        return true;
    }

    /**
     * 创建子用户
     * @param vo 子用户实体类
     */
    @Override
    public void createSubAccount(CreateSubAccountVO vo) {
//        System.out.println("vo: " + vo);
        Account account = this.findAccountByNameOrEmail(vo.getEmail());
        if(account != null)
            throw new IllegalArgumentException("该电子邮件被注册");
        account = this.findAccountByNameOrEmail(vo.getUsername());
        if(account != null)
            throw new IllegalArgumentException("该用户名被注册");
        account = new Account(null, vo.getUsername(), passwordEncoder.encode(vo.getPassword()), vo.getEmail(),
                Const.ROLE_NORMAL, new Date(), JSONArray.copyOf(vo.getClients()).toJSONString());
        this.save(account);
    }

    @Override
    public void deleteSubAccount(int uid) {
        this.removeById(uid);
    }

    /**
     * 获取当前账户的主机列表
     * @return 主机列表
     */
    @Override
    public List<SubAccountVO> listSubAccount() {
        return this.list(Wrappers.<Account>query().eq("role", Const.ROLE_NORMAL))
                .stream().map(account -> {
                    SubAccountVO vo = account.asViewObject(SubAccountVO.class);
                    vo.setClientList(JSONArray.parse(account.getClients()));
                    return vo;
                }).toList();
    }

    /**
     * 更改用户邮箱
     * @param id 用户id
     * @param vo 更改邮箱实体类
     * @return 是否更改成功
     */
    @Override
    public String modifyEmail(int id, ModifyEmailVO vo) {
        String code = getEmailVerifyCode(vo.getEmail());
        if(code == null)
            return "请先获取验证码";
        if(!code.equals(vo.getCode()))
            return "验证码错误, 请重新输入";
        this.deleteEmailVerifyCode(vo.getEmail());
        Account account = this.findAccountByNameOrEmail(vo.getEmail());
        if(account != null && account.getId() != id)
            return "该邮箱已被其他账号绑定, 无法完成操作";
        this.update()
                .eq("id", id)
                .set("email", vo.getEmail())
                .update();
        return null;
    }


    /**
     * 移除Redis中存储的邮件验证码
     * @param email 电邮
     */
    private void deleteEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        stringRedisTemplate.delete(key);
    }


    /**
     * 针对IP地址进行邮件验证码获取限流
     * @param ip 地址
     * @return 是否通过验证
     */
    private boolean verifyLimit(String ip) {
        String key = Const.VERIFY_EMAIL_LIMIT + ip; // 根据ip限制;
        return utils.limitOnceCheck(key, 60);
    }

    /**
     * 获取Redis中存储的邮件验证码
     * @param email 电邮
     * @return 验证码
     */
    private String getEmailVerifyCode(String email) {
        String key = Const.VERIFY_EMAIL_DATA + email;
        return stringRedisTemplate.opsForValue().get(key);
    }
}
