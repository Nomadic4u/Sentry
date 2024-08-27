package org.example.entity.vo.request;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

@Data
public class ChangePasswordVO {

    @Length(min = 6, max = 6)
    String password;

    @Length(min = 6, max = 20)
    String new_password;

}
