package com.mealflow.controller.user;

import com.mealflow.constant.JwtClaimsConstant;
import com.mealflow.dto.UserLoginDTO;
import com.mealflow.entity.User;
import com.mealflow.properties.JwtProperties;
import com.mealflow.result.Result;
import com.mealflow.service.UserService;
import com.mealflow.utils.JwtUtil;
import com.mealflow.vo.UserLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/user/user")
@Tag(name = "用户管理", description = "用户管理相关接口")
public class UserController {


    private final UserService userService;

    private final JwtProperties jwtProperties;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @Operation(summary = "微信登录")
    public Result<UserLoginVO>  login(@Valid @RequestBody UserLoginDTO userLoginDTO) {
        log.info("userLoginDTO:{}",userLoginDTO);
        //微信登录
        User user=userService.wxLogin(userLoginDTO);

        //为微信用户生成jwt令牌
        Map<String,Object> claims=new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID,user.getId());
        String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(),jwtProperties.getUserTtl(), claims);

        UserLoginVO userLoginVO=UserLoginVO.builder()
                .id(user.getId()).openid(user.getOpenid()).token(token).build();

        return Result.success(userLoginVO);
    }
}
