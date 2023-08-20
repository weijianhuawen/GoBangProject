package com.example.gobang001.config;

import com.example.gobang001.controller.GameController;
import com.example.gobang001.controller.MatchController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

import javax.annotation.Resource;
@Configuration
@EnableWebSocket
public class WebsocketConfig implements WebSocketConfigurer {
    @Resource
    private MatchController matchController;
    @Resource
    private GameController gameController;
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        //websocket拦截器
        registry.addHandler(matchController, "/findMatch")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
        //注册路径为/game的路径，用于玩家进入房间进行游戏对局
        registry.addHandler(gameController, "/game")
                .addInterceptors(new HttpSessionHandshakeInterceptor());
    }
}
