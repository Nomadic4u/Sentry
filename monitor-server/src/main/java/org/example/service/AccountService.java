package org.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.entity.dto.Account;
import org.example.entity.vo.request.ConfirmResetVO;
import org.example.entity.vo.request.CreateSubAccountVO;
import org.example.entity.vo.request.EmailResetVO;
import org.example.entity.vo.request.ModifyEmailVO;
import org.example.entity.vo.response.SubAccountVO;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface AccountService extends IService<Account>, UserDetailsService {
    Account findAccountByNameOrEmail(String text);

    String registerEmailVerifyCode(String type, String email, String ip); // IP地址限制请求频率


    String resetConfirm(ConfirmResetVO vo);

    String resetEmailAccountPassword(EmailResetVO vo);

    boolean changePassword(int id, String oldPassword, String newPassword);

    void createSubAccount(CreateSubAccountVO vo);

    void deleteSubAccount(int uid);

    List<SubAccountVO> listSubAccount();

    String modifyEmail(int id, ModifyEmailVO vo);
}


