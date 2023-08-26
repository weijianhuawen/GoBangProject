package com.example.gobang001.mode;

import lombok.Data;

@Data
public class GobangPlayRecord {
    private Integer rid;
    private Integer uid1;
    private Integer uid2;
    private String enemy;
    private Integer changeNum;
    private Integer lastScore;
    private Integer oldScore;
    private Integer winnerCount;
    private Integer totalCount;
    private Double winnerRate;
    private Boolean isWinner;
    private String updateTime;
}
