package com.example.gobang001.mode;

import lombok.Data;

@Data
public class ResultPackage<T> {
    //数据
    private T data;
    //状态码 100表示ok
    private Integer state;
    //响应结果是否顺利
    private Boolean isOk;
    //捎带消息
    private String message;
}
