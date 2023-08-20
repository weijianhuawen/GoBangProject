package com.example.gobang001.service.impl;

import com.example.gobang001.dao.UserDao;
import com.example.gobang001.mode.User;
import com.example.gobang001.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    //注入Dao层对象
    @Resource
    UserDao userDao;
    //根据name查询用户，用于登录验证

    @Override
    public User findUserByUsername(String username) {
        if (username == null || username.equals("")) return null;

        return userDao.selectUserByName(username);
    }

    @Override
    public Integer addUser(User user) {
        if (user == null) return 0;

        return userDao.insertUser(user);
    }

    @Override
    public Integer updateWinnerScore(User user, Integer winnerScore) {
        //验证参数
        if (user == null || winnerScore == null || winnerScore == 0) {
            log.info("[service层] 更新获胜者战绩方法参数错误！");
            return null;
        }
        return userDao.updateWinnerScore(user, winnerScore);
    }

    @Override
    public Integer updateLoserScore(User user, Integer loserScore) {
        //验证参数
        if (user == null || loserScore == null || loserScore == 0) {
            log.info("[service层] 更新失败者战绩方法参数错误！");
            return null;
        }
        return userDao.updateLoserScore(user, loserScore);
    }

    @Override
    public Integer updateDrawScore(User user) {
        //验证参数
        if (user == null ) {
            log.info("[service层] 更新平手时战绩方法参数错误！");
            return null;
        }
        return userDao.updateDrawScore(user);
    }
}
