package org.example.entity.vo.response;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class SshSettingVO {
    String ip;
    int port = 22;
    @NotNull
    @Length(min = 1)
    String username;
    @NotNull
    @Length(min = 1)
    String password;
}
