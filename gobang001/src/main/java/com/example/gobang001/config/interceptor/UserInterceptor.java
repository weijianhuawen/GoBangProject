package com.example.gobang001.config.interceptor;

import com.example.gobang001.mode.User;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.UUID;

@Component
public class UserInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //拦截未登录的用户，如果没有登陆直接重定向到登录页面
        //获取Session
        boolean ret = true;
        HttpSession session = request.getSession(false);
        if (session == null) {
            //获取的session表示用户未登录
            ret = false;
            //重定向到登录页面
            response.sendRedirect("/login.html");
            return ret;
        }

        //获取用户对应的cookie
        User user = (User) session.getAttribute("user");
        if (user == null) {
            //无法获取到用户信息，用户未登录，重定向到登录页面
            ret = false;
            //重定向到登录页面
            response.sendRedirect("/login.html");
            return ret;
        }
        return ret;
    }
}
