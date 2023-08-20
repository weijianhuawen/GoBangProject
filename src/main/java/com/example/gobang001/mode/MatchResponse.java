package com.example.gobang001.mode;

import lombok.Data;

/**
 * websocket 响应体
 */
@Data
public class MatchResponse {
    private String message;
    private Boolean isOk;
    private String reason;
}
