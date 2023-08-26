package com.example.gobang001.dao;

import com.example.gobang001.mode.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserDao {
    //根据username查询用户
    public User selectUserByName(String username);
    //根据用户对象插入用户
    public Integer insertUser(User user);

    //更新胜利者的战绩
    public Integer updateWinnerScore(@Param("user") User user, Integer winnerScore);

    //更新失败方的战绩
    public Integer updateLoserScore(@Param("user") User user, Integer loserScore);

    //更新平手时两位玩家的战绩
    public Integer updateDrawScore(@Param("user") User user);
}
