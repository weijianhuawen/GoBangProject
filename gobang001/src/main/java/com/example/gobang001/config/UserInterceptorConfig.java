package com.example.gobang001.config;

import com.example.gobang001.config.interceptor.UserInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class UserInterceptorConfig implements WebMvcConfigurer {
    @Autowired
    private UserInterceptor userInterceptor;
    //设立拦截规则
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/login.html")
                .excludePathPatterns("/login")
                .excludePathPatterns("/register.html")
                .excludePathPatterns("/register")
                .excludePathPatterns("/css/**")
                .excludePathPatterns("/fonts/**")
                .excludePathPatterns("/images/**")
                .excludePathPatterns("/js/**");
    }
}
