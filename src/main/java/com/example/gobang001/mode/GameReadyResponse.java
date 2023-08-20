package com.example.gobang001.mode;

import com.sun.org.apache.xpath.internal.operations.Bool;
import lombok.Data;

@Data
public class GameReadyResponse {
    //房间rid
    private String rid;
    //响应消息
    private String message;
    //请求是否处理成功
    private Boolean isOk;
    //请求响应成功的说明或者是响应失败的原因
    private String reason;
    //当前玩家的uid
    private Integer thisPlayerUid;
    //对手玩家的uid
    private Integer thatPlayerUid;
    //白字先手玩家uid
    private Integer priorityPlayerUid;
    //是否是我方落子
    private Boolean white;
    //当前玩家的用户名
    private String thisPlayerName;
    //对手玩家的用户名
    private String thatPlayerName;
}
