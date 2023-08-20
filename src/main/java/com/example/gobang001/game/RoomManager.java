package com.example.gobang001.game;

import com.example.gobang001.mode.Room;
import com.example.gobang001.mode.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

//房间管理器，对进行游戏对局的房间进行管理
@Component
@Slf4j
public class RoomManager {
    //维护房间rid与房间实体的映射
    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();
    //维护玩家uid与房间rid的映射
    private final ConcurrentHashMap<Integer, String> uidToRoom = new ConcurrentHashMap<>();

    //添加玩家到指定房间
    public void add(Room room, User[] players) {
        //添加房间rid与room实体
        rooms.put(room.getRid(), room);

        //将房间中所有的玩家uid与房间建立联系
        for (int i = 0; i < players.length; i++) {
            uidToRoom.put(players[i].getUid(), room.getRid());
        }

        log.info("[房间管理器] 添加rid为:" + room.getRid() + "的管理！");
    }

    //移除房间
    public void remove(Room room) {
        if (room == null) return;
        User[] players = room.getPlayers();
        //将房间中的玩家与房间解除关系
        for (int i = 0; i < players.length; i++) {
            uidToRoom.remove(players[i].getUid());
        }
        //将房间rid与房间实体关系解除
        rooms.remove(room.getRid());
        log.info("[房间管理器] 删除rid为:" + room.getRid() + "的管理！");
    }

    //根据rid查询某个房间
    public Room getRoomByRid(String rid) {
        return rooms.getOrDefault(rid, null);
    }

    //根据玩家uid查询某个房间
    public Room getRoomByUid(Integer uid) {
        //首先根据uid查询rid
        String rid = uidToRoom.getOrDefault(uid, "");
        //再根据rid查询房间实体
        return rooms.getOrDefault(rid, null);
    }
}
