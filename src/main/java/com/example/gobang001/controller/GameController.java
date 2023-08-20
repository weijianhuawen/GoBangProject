package com.example.gobang001.controller;

import com.example.gobang001.game.OnlineUserManager;
import com.example.gobang001.game.RoomManager;
import com.example.gobang001.mode.*;
import com.example.gobang001.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Objects;

@Controller
@Slf4j
public class GameController extends TextWebSocketHandler {
    //房间管理器
    @Autowired
    private RoomManager roomManager;
    //json字符串与Java对象之间的转换
    @Autowired
    private ObjectMapper objectMapper;
    //大厅与房间玩家在线管理器
    @Autowired
    private OnlineUserManager onlineUserManager;
    //用户与战绩表查询
    @Autowired
    private UserService userService;

    //上线操作，当玩家跳转到游戏对战页面，我们需要建立两位玩家的会话连接
    private void online(WebSocketSession session) throws IOException  {
        //确认玩家是否登录（会话是否存在，如果会话不存在，后续游戏无法进行下去）
        GameReadyResponse response = new GameReadyResponse();
        User user = null;
        try {
            //获取玩家信息
            user = (User) session.getAttributes().get("user");
            //获取玩家所分配的房间
            Room room = roomManager.getRoomByUid(user.getUid());

            if (room == null) {
                //表示该玩家没有进行匹配就进入游戏大厅，需要将该玩家跳转到游戏大厅页面
                log.info("[websocket请求与响应：游戏对战阶段] 玩家：" + user.getUsername() + "未匹配到游戏房间，请重新匹配！");
                response.setReason("玩家未进行匹配，请重新匹配！");
                response.setIsOk(false);
                TextMessage message = new TextMessage(objectMapper.writeValueAsString(response));
                session.sendMessage(message);
                return;
            }
            //检测多开问题
            //当用户进入游戏房间页面的时候已经退出了游戏大厅页面，因此游戏大厅是离线的，而进入游戏房间时，此处还没将玩家上线
            //所以此时玩家在游戏大厅是离线的，并且在房间中也是离线的
            //因此此时只要游戏大厅中玩家在线或者游戏房间该玩家在线，那么该玩家该会话一定存在多个，处于多开状态
            if (onlineUserManager.getWebsocketSessionForHall(user.getUid()) != null
                    || onlineUserManager.getWebSocketSessionForRoom(user.getUid()) != null) {
                log.info("[websocket请求与响应：游戏对战阶段] 用户多处登录！");
                //返回给客户端重复登录的响应
                response.setIsOk(true);
                response.setMessage("repeatConnection");
                response.setReason("用户在多处地方登录！");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                //将该次等登录的会话关闭
                session.close();
                return;
            }
            //将用户设置为在线状态
            onlineUserManager.addOnlineUsersForRoom(user.getUid(), session);
            //玩家已匹配到房间，则将玩家正式放入到匹配房间的名单当中
            //尝试放入房间第一个为位置
            //可能存在多个线程进行玩家进入房间，存在线程安全问题，修改对象为room，所以以room为加锁对象
            //对于同一个房间的room的玩家才有互斥性，每一个room对象加一把锁即可
            synchronized (room) {
                if (room.getPlayers()[0] == null) {
                    //房间第一个玩家为空
                    room.add(0, user);
                    //取首个进入房间的玩家为先手
                    room.setPriorityUid(user.getUid());
                    log.info("[websocket请求与响应：游戏对战阶段] 玩家：" + user.getUsername() + "进入游戏房间：" + room.getRid() + "的第一个玩家！");
                    return;
                }

                //尝试放入第二个玩家
                if (room.getPlayers()[1] == null) {
                    //房间第二个玩家为空
                    room.add(1, user);
                    log.info("[websocket请求与响应：游戏对战阶段] 玩家：" + user.getUsername() + "进入游戏房间：" + room.getRid() + "的第二个玩家！");
                    //此时房间的成员已满，游戏就绪，此时需要通知两位玩家的对手是谁
                    noticePlayerEnterGame(room, room.getPlayers()[0], room.getPlayers()[1]);
                    noticePlayerEnterGame(room, room.getPlayers()[1], room.getPlayers()[0]);
                    return;
                }
            }

            //执行到此处，说明房间已经满了，返回房间已满的响应
            response.setIsOk(false);
            response.setMessage("full");
            response.setReason("房间人数已满，请重新匹配！");
            log.info("[websocket请求与响应：游戏对战阶段] 房间：" + room.getRid() + "已经满了！");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } catch (NullPointerException e) {
            //出现空指针异常表示用户未登录（session成功获取前提下） 返回响应表示用户未登录，需要重新进行登录
            log.info("[websocket请求与响应：游戏对战阶段] 建立房间连接时，用户未登录，请让用户重新登录或检查会话获取是否正常！");
            e.printStackTrace();
            //返回响应给客户端
            response.setIsOk(false);
            response.setReason("用户未登陆，请重新登录一下吧！");
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(response));
            session.sendMessage(message);
        }

    }

    //通知玩家游戏就绪，并告知对手信息
    private void noticePlayerEnterGame(Room room, User thisPlayer, User thatPlayer) {
        GameReadyResponse response = new GameReadyResponse();
        response.setIsOk(true);
        response.setPriorityPlayerUid(room.getPriorityUid());
        response.setThisPlayerUid(thisPlayer.getUid());
        response.setThatPlayerUid(thatPlayer.getUid());
        response.setMessage("gameReady");
        response.setWhite(Objects.equals(room.getPriorityUid(), thisPlayer.getUid()));
        response.setRid(room.getRid());
        response.setThisPlayerName(thisPlayer.getUsername());
        response.setThatPlayerName(thatPlayer.getUsername());
        response.setReason("房间玩家已满，可以开始进行游戏了！");
        log.info("[websocket请求与响应：游戏对战阶段] 房间： " +  room.getRid() + "玩家就绪，准备开始游戏！");
        //获取玩家的session
        WebSocketSession session = onlineUserManager.getWebSocketSessionForRoom(thisPlayer.getUid());
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            //格式转换出现异常
            log.info("[websocket请求与响应：游戏对战阶段] 通知玩家时，响应体格式转换出现问题！");
            e.printStackTrace();
        }
    }
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        //玩家进入房间，所需准备做的事情（正式游戏前做的事情）
        log.info("[websocket请求与响应：游戏对战阶段] 玩家进入对战页面，进行玩家websocket连接准备工作！");
        try {
            online(session);
        } catch (IOException e) {
            //格式转换异常
            log.info("[websocket请求与响应：游戏对战阶段] websocket连接准备阶段出现格式转换异常！");
            e.printStackTrace();
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        //处理客户端落子请求
        //验证用户身份信息
        try {
            User user = (User) session.getAttributes().get("user");
            //获取用户所在的游戏房间
            Room room = roomManager.getRoomByUid(user.getUid());
            //获取客户端请求的数据载荷
            String payload = message.getPayload();
            //将json格式字符串转换为请求体对象
            GameRequest request = objectMapper.readValue(payload, GameRequest.class);
            //判断请求是否为落子请求
            if (request == null || !"putChess".equals(request.getMessage())) {
                //请求格式非法，不做处理，返回响应到客户端
                log.info("[websocket请求与响应：游戏对战阶段] 非法的落子请求！");
                GameResponse response = new GameResponse();
                response.setMessage("illegalFormatRequest");
                response.setIsOk(false);
                response.setReason("非法落子请求格式，拒绝访问！");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                return;
            }
            //执行下棋操作
            room.putChess(request, onlineUserManager, roomManager, userService);
        } catch (NullPointerException e) {
            log.info("[websocket请求与响应：游戏对战阶段] 落子请求处理时，用户信息获取失败，大概率用户未登录，也有可能是session获取异常！");
        }
    }

    //房间玩家下线操作
    private User offline(WebSocketSession session) {
        //下线操作
        User user = null;
        try {
            user = (User) session.getAttributes().get("user");
            //检测是否为多开情况，多开情况不必下线原来的连接
            WebSocketSession tmpSession = onlineUserManager.getWebSocketSessionForRoom(user.getUid());
            if (session == tmpSession) {
                //断开玩家与游戏房间的连接
                onlineUserManager.removeOnlineUserForRoom(user.getUid());
                log.info("[websocket请求与响应：游戏对战阶段]  玩家：" + user.getUsername() + "断开房间连接");
            }
        } catch (NullPointerException e) {
            log.info("[websocket请求与响应：游戏对战阶段] 下线阶段，用户登录状态失效！");
        }
        return user;
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.info("[websocket请求与响应：游戏对战阶段] 玩家出现异常，websocket连接异常！");
        User thisPlayer = offline(session);
        //当一方玩家正在下棋，但是另一方玩家因为未知情况掉线了，此时后端是无法立即判断胜负的，需要等到这个玩家下棋后才能结算结果
        //但是正常情况，如果检测到一位玩家突然下线了，应该立即通知另外一位玩家，告知它已经获胜了，并结算分数
        //当玩家掉线时会调用handleTransportError或者afterConnectionClosed方法，所以在这两个方法里面加入通知另外一方玩家获胜的操作即可
        noticeOtherPlayerWinner(thisPlayer);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("[websocket请求与响应：游戏对战阶段] 玩家退出房间，websocket连接断开！");
        User thisPlayer = offline(session);
        noticeOtherPlayerWinner(thisPlayer);
    }

    private void noticeOtherPlayerWinner(User thisPlayer) {
        if (thisPlayer == null) {
            //玩家信息无效
            log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，传入的玩家信息无效，无法进行通知！");
            return;
        }
        //如果某位玩家因为意外掉线了，那么当时的游戏房间一定还没有销毁，所以先获取玩家所在房间，如果获取不到就说明是正常结算，不需要通知另外一位玩家获胜
        //反之，如果能够获取到游戏房间，就说明至少一位玩家已经下线了
        //获取当前下线玩家所在的游戏房间
        Room room = roomManager.getRoomByUid(thisPlayer.getUid());
        if (room == null) {
            //房间已经被销毁，属于正常结算，无需通知另外一位玩家获胜
            log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，房间已经被销毁，无需通知另外一位玩家获胜！");
            return;
        }
        //获取另外一位玩家的对象信息
        User thatPlayer = Objects.equals(thisPlayer.getUid(), room.getPlayers()[0].getUid()) ? room.getPlayers()[1] : room.getPlayers()[0];
        if (thatPlayer == null) {
            //另外一位玩家身份无效，无法通知其获胜
            log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，另外一位玩家对象获取异常，无法进行通知！");
            return;
        }
        //获取另外一位玩家的session
        WebSocketSession session = onlineUserManager.getWebSocketSessionForRoom(thatPlayer.getUid());
        if (session == null) {
            //另外一位玩家会话已经关闭，无法通知
            log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，被通知玩家会话已被删除，无法进行通知！");
            //按照平手的规则进行结算
            userService.updateDrawScore(thisPlayer);
            userService.updateDrawScore(thatPlayer);
            return;
        }
        //通知另外一位玩家已经获胜
        //构造响应
        GameResponse response = new GameResponse();
        response.setIsOk(true);
        response.setWinner(thatPlayer.getUid());
        response.setWinnerName(thatPlayer.getUsername());
        response.setReason("另外一位玩家掉线，恭喜您获得胜利！");
        response.setUid(thatPlayer.getUid());
        response.setMessage("putChess");
        response.setRow(-1);
        response.setCol(-1);
        //结算分数
        room.handlerScore(thatPlayer, thisPlayer, userService);
        //发送响应
        try {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
        } catch (IOException e) {
            log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，响应格式转换异常！");
            e.printStackTrace();
        }
        log.info("[websocket请求与响应：游戏对战阶段] 意外下线通知玩家获胜阶段，已经通知玩家：" + thatPlayer.getUsername() + "获胜！");
        //销毁游戏房间
        roomManager.remove(room);
    }
}
