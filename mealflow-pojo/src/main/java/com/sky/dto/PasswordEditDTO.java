package com.sky.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

@Data
public class PasswordEditDTO implements Serializable {

    //员工id
    @NotNull(message = "员工id不能为空")
    private Long empId;

    //旧密码
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    //新密码
    @NotBlank(message = "新密码不能为空")
    @Size(min=8,max=16, message = "密码长度在8到16位之间")
    private String newPassword;

}
