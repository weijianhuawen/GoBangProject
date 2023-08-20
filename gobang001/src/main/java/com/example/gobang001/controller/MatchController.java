package com.example.gobang001.controller;

import com.example.gobang001.game.Matcher;
import com.example.gobang001.game.OnlineUserManager;
import com.example.gobang001.mode.MatchRequest;
import com.example.gobang001.mode.MatchResponse;
import com.example.gobang001.mode.User;
import com.example.gobang001.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Controller
@Slf4j
public class MatchController extends TextWebSocketHandler {
    //java对象与json格式转换
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OnlineUserManager onlineUserManager;

    //数据库service
    @Autowired
    private UserService userService;

    //匹配管理
    @Autowired
    private Matcher matcher;

    //不妨将游戏上线大厅所做的准备工作封装起来
    private void online(WebSocketSession session) throws IOException{
        //获取玩家的用户信息
        try {
            User player = (User) session.getAttributes().get("user");
            //判定当前用户是否已经登录
            WebSocketSession tmpSession = onlineUserManager.getWebsocketSessionForHall(player.getUid());
            //多开检测
            if (tmpSession != null) {
                //告知客户端重复登录
                MatchResponse response = new MatchResponse();
                response.setReason("用户在多处登录，无法登录！");
                response.setMessage("repeatConnection");
                //多开检测是我们意料之中的事情，可以将响应是否成功设置为成功
                response.setIsOk(true);
                TextMessage message = new TextMessage(objectMapper.writeValueAsString(response));
                session.sendMessage(message);
                //关闭连接
                session.close();
            }
            //将玩家放入上线列表当中
            onlineUserManager.addOnlineUserForHall(player.getUid(), session);
        } catch (NullPointerException e) {
            //无法获取到用户信息，说明用户并没有登陆
            e.printStackTrace();
            MatchResponse response = new MatchResponse();
            response.setIsOk(false);
            response.setReason("[websocket请求处理：匹配阶段] 用户未登录！禁止参与后续匹配！请重新登录！");
            //将返回的响应体转换为json字符串，并封装到websocket消息体中
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(response));
            session.sendMessage(message);
        }
    }

    //连接成功后所做的事情， 即玩家在游戏大厅上线时所需要做的准备工作
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("[websocket请求处理：匹配阶段] websocket建立连接！");
        //将玩家设置为上线状态
        //获取玩家的用户信息
        try {
            online(session);
        } catch (IOException e) {
            //检测到IOException，大概率是因为json字符串与Java对象互转出现格式造成的
            log.info("[匹配模块] websocket建立连接阶段出现请求或响应格式转换错误！");
            e.printStackTrace();
        }
    }
    //收到请求的时候需要做的事情
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        log.info("[websocket请求处理：匹配阶段] 处理websocket的文本请求！");

        //处理客户端发来的请求
        //约定好的两种请求：startMatch/ stopMatch

        //第一步，获取用户信息和更新用户最新的战绩信息
        User user = (User) session.getAttributes().get("user");
        user = userService.findUserByUsername(user.getUsername());
        //获取websocket响应，解析message
        String payload = message.getPayload();
        //将获取到的请求载荷转换为Java对象
        MatchRequest request = objectMapper.readValue(payload, MatchRequest.class);
        MatchResponse response = new MatchResponse();
        //解析请求
        if ("startMatch".equals(request.getMessage())) {
            //客户端发起匹配请求
            log.info("[websocket请求处理：匹配阶段] startMatch 客户端匹配请求处理！");
            //将用户放入匹配队列
            matcher.add(user);

            //返回websocket响应
            //设置响应数据
            response.setIsOk(true);
            response.setMessage("startMatch");
            response.setReason("已开始匹配！");

        } else if ("stopMatch".equals(request.getMessage())) {
            //客户端发起停止匹配请求
            log.info("[websocket请求处理：匹配阶段] stopMatch请求处理！");
            //将用户从匹配队列中删除
            matcher.remove(user);

            //返回响应到客户端
            //设置websocket响应体参数
            response.setIsOk(true);
            response.setReason("玩家已退出匹配队列！");
            response.setMessage("stopMatch");
        } else {
            //其他的请求均为非法参数
            log.info("[websocket请求处理：匹配阶段] 玩家上线准备时 非法请求参数");
            response.setMessage("badRequest");
            response.setIsOk(false);
            response.setReason("非法参数请求！");
        }
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    //由于websocket出现异常和关闭的操作逻辑差不多，我们可以将其封装在下线逻辑当中，遇到websocket关闭或出现异常调用即可
    private void offline(WebSocketSession session) throws IOException {
        try {
            //获取用户的身份信息
            User player = (User) session.getAttributes().get("user");
            //获取当前用户的session（与大厅的会话）
            WebSocketSession tmpSession = onlineUserManager.getWebsocketSessionForHall(player.getUid());

            //获取的session与当前在线玩家的session一致，则删除当前玩家，表示该玩家下线, 否则该玩家为多开状态，
            //我们处理多开的情况时，是保留最早登录的记录，后续登录记录删除，下线操作是关闭最早玩家的连接，因此如果遇到多开，不需要进行处理
            //否则不进行操作，仅做会话关闭处理，此时用户在多个浏览器进行登录，采取后续无法登录原则，保留最开始的用户
            if (session == tmpSession) {
                //进行玩家下线操作
                onlineUserManager.removeOnlineUserForHall(player.getUid());
            }

        } catch (NullPointerException e) {
            //获取到空指针异常标明用户没有登录
            e.printStackTrace();
            MatchResponse response = new MatchResponse();
            response.setIsOk(false);
            response.setReason("[websocket请求处理：匹配阶段] 用户未登录！请登录！");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(response));
            session.sendMessage(message);
        }
    }

    //发生异常或者错误的时候需要做的事情
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
       log.info("[websocket请求处理：匹配阶段] websocket出现异常！");
        try {
            offline(session);
        } catch (IOException e) {
            //遇到IOException表示json与Java对象格式转换出现问题
            log.info("[websocket请求处理：匹配阶段]  websocket 连接异常状态 格式转换错误");
            e.printStackTrace();
        }
    }
    //连接关闭前所需要做的事情
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status)  {
        log.info("[websocket请求处理：匹配阶段] websocket关闭处理！");

        try {
            offline(session);
        } catch (IOException e) {
            //遇到IOException表示json与Java对象格式转换出现问题
            log.info("[websocket请求处理：匹配阶段]  websocket关闭连接阶段出现 格式转换错误");
            e.printStackTrace();
        }
    }
}
