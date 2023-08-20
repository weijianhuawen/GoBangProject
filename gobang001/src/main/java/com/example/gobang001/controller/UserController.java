package com.example.gobang001.controller;

import com.example.gobang001.mode.ResultPackage;
import com.example.gobang001.mode.User;
import com.example.gobang001.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@RestController
@Slf4j
public class UserController {
    @Autowired
    UserService userService;
    //login
    @RequestMapping("/login")
    public Object login(String username, String password, HttpServletRequest request) {
        //判断传入参数是否有效
        ResultPackage<User> resultPackage = new ResultPackage<>();
        resultPackage.setState(-1);
        resultPackage.setIsOk(false);

        if (username == null || username.equals("")
            || password == null || password.equals("")) {
            //用户名或密码不能够为null
            log.info("[login]用户用户名或密码为空");
            resultPackage.setMessage("用户输入用户名或密码为空！请完整输入用户名和密码！");
            return resultPackage;
        }

        //验证密码是否与数据库中用户的密码是否一致
        User user = userService.findUserByUsername(username);
        if (user == null || !user.getPassword().equals(password)) {
            //用户不存在或输入密码错误
            log.info("[login]用户不存在或密码错误");
            resultPackage.setMessage("用户名或密码错误！请重新输入！");
            return resultPackage;
        }

        //验证成功，存储session
        //获取Session
        HttpSession session = request.getSession(true);
        //将信息存入session中
        session.setAttribute("user", user);

        //抹去敏感信息
        user.setEmail("");
        user.setPassword("000000");
        user.setNumbers("");
        //设置返回结果体
        resultPackage.setData(user);
        resultPackage.setMessage("登录成功！");
        resultPackage.setIsOk(true);
        resultPackage.setState(100);
        return resultPackage;
    }

    //注册
    @RequestMapping("register")
    public Object register(User user, String repeatPassword) {
        ResultPackage<User> resultPackage = new ResultPackage<>();
        resultPackage.setState(-1);
        resultPackage.setIsOk(false);
        //判断用户必填信息是否为空
        if (user == null
            || user.getUsername() == null || user.getUsername().equals("")
            || user.getPassword() == null || user.getPassword().equals("")
            || user.getNumbers() == null || user.getNumbers().equals("")
            || user.getEmail() == null || user.getEmail().equals("")
        ) {
            //用户的用户名，密码，手机号，邮箱不能为空
            log.info("用户必填信息出现空缺");
            resultPackage.setMessage("用户名或密码或手机号或邮箱不能为空");
            return resultPackage;
        }

        //验证确认密码栏与用户所输入的密码是否一致
        if (repeatPassword == null || repeatPassword.equals("")) {
            log.info("用户未输入确认密码字段");
            resultPackage.setMessage("请输入确认密码");
            return resultPackage;
        }

        if (!repeatPassword.equals(user.getPassword())) {
            //确认密码与第一次输入的密码不一致
            log.info("用户两次输入的密码不一致");
            resultPackage.setMessage("两次输入的密码不一致，请重新输入!");
            return resultPackage;
        }

        //基本数据验证通过，将数据存入数据库
        try {
            userService.addUser(user);
        } catch (DuplicateKeyException e) {
            //注册的用户名已经存在
            resultPackage.setMessage("用户已存在，请登录！");
            return resultPackage;
        }

        //注册成功
        resultPackage.setMessage("注册成功!");
        resultPackage.setIsOk(true);
        resultPackage.setState(100);
        return resultPackage;
    }

    //获取最新的登录用户信息
    @RequestMapping("/userinfo")
    public Object getUserInfo(HttpServletRequest request) {
        ResultPackage<User> resultPackage = new ResultPackage<>();
        resultPackage.setState(-1);

        //从session中获取用户信息
        HttpSession session = request.getSession(false);
        User user = null;
        //验证session是否存在
        try {
            user = (User) session.getAttribute("user");
        } catch (NullPointerException e) {
            //触发空指针异常，表示session为空
            log.info("session为空");
            resultPackage.setMessage("登录状态已失效，请重新登录！");
            resultPackage.setState(-2);
            return resultPackage;
        }

        //验证user是否存在
        if (user == null || user.getUsername() == null || user.getUsername().equals("")) {
            //有关user的会话已经被删除或不存在，表示用户登录状态失效
            log.info("会话失效，用户已经退出登录");
            resultPackage.setMessage("登录状态已失效，请重新登录！");
            resultPackage.setState(-2);
            return resultPackage;
        }

        //验证通过，获取最新的用户信息，并返回
        User lastUser = userService.findUserByUsername(user.getUsername());
        //抹除敏感信息
        if (lastUser == null) {
            //表示该用户已经注销
            log.info("会话存在，但用户不存在，需要删除session");
            //删除session
            session.removeAttribute("user");
            resultPackage.setMessage("用户信息获取失败，请重新登录！");
            return resultPackage;
        }

        lastUser.setNumbers("");
        lastUser.setPassword("000000");
        lastUser.setEmail("");
        resultPackage.setState(100);
        resultPackage.setData(lastUser);
        resultPackage.setIsOk(true);
        resultPackage.setMessage("获取成功!");
        return resultPackage;
    }
 }
