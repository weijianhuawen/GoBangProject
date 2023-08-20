package com.example.gobang001.mode;

import lombok.Data;

@Data
public class User {
    private Integer uid;
    private String username;
    private String password;
    private String email;
    private String numbers;
    private Integer score;
    private Integer totalCount;
    private Integer winCount;
    private Double winRate;
}
