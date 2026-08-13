package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;


@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {


    private final WeChatProperties weChatProperties;
    //微信服务期接口地址
    public static final String WX_LOGIN="https://api.weixin.qq.com/sns/jscode2session?";

    private final UserMapper userMapper;
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    public User wxLogin(UserLoginDTO userLoginDTO) {
        //调用微信接口服务，获得当前微信用户的openid
        String openid = getOpenid(userLoginDTO.getCode());

        //判断当前openid是否为空，如果为空表示登录失败，抛出业务异常
        if(openid==null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //根据openid查询数据库，判断当前用户是否为新用户
        User user = userMapper.getByOpenid(openid);
        //判断当前用户是否为新用户
        //如果为新用户，则创建用户记录
        if(user==null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }
        //返回这个用户对象
        return user;
    }

    //调用微信接口服务，获得当前微信用户的openid
//    private String getOpenid(String code) {
//        Map<String, String> map = new HashMap<>();
//        map.put("appid", weChatProperties.getAppid());
//        map.put("secret", weChatProperties.getSecret());
//        map.put("js_code", code);
//        map.put("grant_type", "authorization_code");
//        String json = HttpClientUtil.doGet(WX_LOGIN, map);
//        JSONObject jsonObject = JSON.parseObject(json);
//        String openid = jsonObject.getString("openid");
//        return openid;
    //}
    private String getOpenid(String code) {
        // 本地调试：跳过真实微信校验，所有用户归到同一个固定 openid
        // 真实环境：注释掉下面这段，改回调微信 code2session 接口
        log.info("mock微信登录，code={}，使用固定openid", code);
        return "mock_openid_fixed";
    }

}
