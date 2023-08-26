package com.example.gobang001.mode;

import lombok.Data;


public enum Code {
    //登录相关
    LOGIN_SUCCESS(101), LOGIN_FALSE(910), NO_LOGIN(911), MANY_LOGIN(912),
    //注册相关
    REGISTER_SUCCESS(201), REGISTER_FALSE(920),
    //记录相关
    RECORD_GET_SUCCESS(301), RECORD_GET_FALSE(930);
    private Integer state;
    private Code(Integer state) {
        this.state = state;
    }
}
