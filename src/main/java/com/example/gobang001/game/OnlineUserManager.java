package com.example.gobang001.game;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OnlineUserManager {
    //管理用户的在线状态（游戏大厅）
    //基于哈希表
    //private final HashMap<Integer, WebSocketSession> onlineUsers = new HashMap<>();
    //考虑线程安全问题,使用ConcurrentHashMap
    private final ConcurrentHashMap<Integer, WebSocketSession> onlineUsersForHall = new ConcurrentHashMap<>();
    //维护在游戏房间用户的在线情况
    private final ConcurrentHashMap<Integer, WebSocketSession> onlineUsersForRoom = new ConcurrentHashMap<>();

    //添加游戏大厅中的在线用户
    public boolean addOnlineUserForHall(Integer uid, WebSocketSession session) {
        onlineUsersForHall.put(uid, session);
        return true;
    }


    //用户从游戏大厅离线
    public boolean removeOnlineUserForHall(Integer uid) {
        onlineUsersForHall.remove(uid);
        return true;
    }

    //获取用户所在的会话（是否处在五子棋匹配大厅中）
    public WebSocketSession getWebsocketSessionForHall(Integer uid) {
        return onlineUsersForHall.getOrDefault(uid, null);
    }
    //添加游戏房间中的在线用户
    public boolean addOnlineUsersForRoom(Integer uid, WebSocketSession session) {
        onlineUsersForRoom.put(uid, session);
        return true;
    }
    //删除游戏房间中的在线用户
    public boolean removeOnlineUserForRoom(Integer uid) {
        onlineUsersForRoom.remove(uid);
        return  true;
    }
    //获取游戏房间中玩家的会话
    public WebSocketSession getWebSocketSessionForRoom(Integer uid) {
        return onlineUsersForRoom.getOrDefault(uid, null);
    }
}
