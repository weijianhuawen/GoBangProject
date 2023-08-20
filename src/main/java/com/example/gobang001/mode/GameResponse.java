package com.example.gobang001.mode;

import lombok.Data;

/**
 * 落子的响应体，基本约定如下：
 * message 响应体的类型
 * reason 未预期响应的原因
 * isOk 响应是否达到预期
 * row 下棋者下棋的位置，行
 * col 下棋者下棋的位置，列
 * uid 下棋者的uid
 * winner 是否分出胜负，0表示未分出胜负，1表示分出了胜负
 */
@Data
public class GameResponse {
    //响应类型
    private String message;
    //下棋者uid
    private Integer uid;
    //下棋的位置
    private Integer row;
    private Integer col;
    //是否分出胜负
    private Integer winner;
    //响应是否达到预期
    private Boolean isOk;
    //响应未达到预期的说明
    private String reason;
    //胜利者的用户名
    private String winnerName;
}
