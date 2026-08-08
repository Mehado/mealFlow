package com.sky.websocket;

import com.sky.constant.JwtClaimsConstant;
import com.sky.properties.JwtProperties;
import com.sky.utils.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket服务
 */
@Slf4j
@Component
@ServerEndpoint("/ws/{token}")
public class WebSocketServer {

    //存放会话对象
    private static Map<String, Session> sessionMap = new HashMap<String, Session>();

    // JwtProperties 不是 Spring 注入的（@ServerEndpoint 对象由容器创建），用 static 桥接
    private static JwtProperties jwtProperties;

    @Autowired
    public void setJwtProperties(JwtProperties jwtProperties) {
        WebSocketServer.jwtProperties = jwtProperties;
    }

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        try {
            //解析校验token，能解出来说明是合法员工
            Claims claims = JwtUtil.parseJWT(jwtProperties.getAdminSecretKey(), token);
            Long empId = Long.valueOf(claims.get(JwtClaimsConstant.EMP_ID).toString());
            //把员工id存到会话属性里面，onClose时用它从map移除
            session.getUserProperties().put("empId", empId);
            sessionMap.put(String.valueOf(empId), session);
            log.info("客服端：{}建立连接",empId);
        } catch (Exception ex) {
            //token无效或者过期，直接关掉连接
            log.warn("WebSocket鉴权失败，拒绝连接！！！！");
            try {
                session.close();
                } catch (IOException e) {
                log.error("关闭连接失败", e);
            }
        }

    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message) {
        log.info("收到客户端消息：{}",message);
    }

    /**
     * 连接关闭调用的方法,从map移除
     */
    @OnClose
    public void onClose(Session session) {
        Object empId = session.getUserProperties().get("empId");
        if (empId != null) {
            sessionMap.remove(String.valueOf(empId));
            log.info("客服端：{}断开连接",empId);
        }
    }

    /**
     * 群发
     *
     * @param message
     */
    public void sendToAllClient(String message) {
        Collection<Session> sessions = sessionMap.values();
        for (Session session : sessions) {
            try {
                //服务器向客户端发送消息
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                log.error("群发消息失败", e);
            }
        }
    }

}
