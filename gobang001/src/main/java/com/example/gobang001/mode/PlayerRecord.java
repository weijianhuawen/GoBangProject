package com.example.gobang001.mode;

import lombok.Data;

@Data
public class PlayerRecord {
    private Integer uid;
    private Integer enemyUid;
    private String username;
    private String enemyName;
    private Integer changeNum;
    private Integer score;
    private Integer preScore;
    private Integer totalCount;
    private Integer winCount;
    private Double winRate;
    private String updateTime;
    private Boolean isWinner;
}
