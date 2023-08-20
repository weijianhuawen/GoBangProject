package com.example.gobang001.mode;

import lombok.Data;

/**
 * 落子请求体 约定如下：
 * message 表示约定的信息标志
 * uid 下棋者的uid
 * row 下棋的位置，行
 * col 下棋的位置，列
 */
@Data
public class GameRequest {
    //请求类型
    private String message;
    //下棋者的uid
    private Integer uid;
    //下棋的位置（row, col）
    private Integer row;
    private Integer col;

    //前端棋盘大小（备用）
    private Integer maxRow;
    private Integer maxCol;
}
