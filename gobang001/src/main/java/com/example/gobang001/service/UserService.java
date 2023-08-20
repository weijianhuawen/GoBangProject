package com.example.gobang001.service;

import com.example.gobang001.mode.User;
import org.springframework.stereotype.Service;

@Service
public interface UserService {
    //login，获取用户信息
    public User findUserByUsername(String username);
    //register, 注册，将用户信息提交到Dao层
    public Integer addUser(User user);
    //更新获胜方的战绩
    public Integer updateWinnerScore(User user, Integer winnerScore);
    //更新失败方的战绩
    public Integer updateLoserScore(User user, Integer loserScore);
    //更新平手时的战绩
    public Integer updateDrawScore(User user);
}
