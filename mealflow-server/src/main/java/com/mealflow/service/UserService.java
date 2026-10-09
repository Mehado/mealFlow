package com.mealflow.service;

import com.mealflow.dto.UserLoginDTO;
import com.mealflow.entity.User;

public interface UserService {
    /**
     * 微信登录
     * @Param userLoginDTO
     * @return
     */
    User wxLogin(UserLoginDTO userLoginDTO);
}
