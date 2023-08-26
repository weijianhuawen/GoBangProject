package com.example.gobang001.controller;

import com.example.gobang001.mode.*;
import com.example.gobang001.service.RecordService;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.jdbc.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RestController
@RequestMapping("/record")
@Slf4j
public class RecordController {
    //专属记录数据的服务层对象
    @Autowired
    private RecordService recordService;
    //查询全站的对战记录
    @GetMapping("play")
    public Object getAllPlayRecord() {
        //统一拦截未登录处理

        //获取查询数据
        List<GobangPlayRecord> data = recordService.getAllPlayRecord();
        //封装数据
        ResultPackage<List<GobangPlayRecord>> resultPackage = new ResultPackage<>();
//        resultPackage.setState(Code.RECORD_GET_SUCCESS);
        resultPackage.setState(301);
        resultPackage.setMessage("成功获取全站对战记录！");
        resultPackage.setIsOk(true);
        resultPackage.setData(data);
        log.info("[记录模块] 获取全部对战记录！");
        return resultPackage;
    }

    //查询用户最新的对战记录
    @GetMapping
    public Object getPlayerScore(HttpServletRequest request) {
        //获取用户的session
        HttpSession session = request.getSession(false);
        User user;
        ResultPackage<List<PlayerRecord>> resultPackage = new ResultPackage<>();
        //判断session是否存在
        try {
            user = (User) session.getAttribute("user");
            //获取用户的uid
            Integer uid = user.getUid();

            if (uid == null || uid <= 0) {
                log.info("[记录模块] 用户uid不合法，无法查询该玩家的最新对战记录！");
                resultPackage.setData(null);
                resultPackage.setState(930);
                resultPackage.setMessage("非法用户！无法获取到有效的游戏对战记录！");
                resultPackage.setIsOk(false);
                return resultPackage;
            }
        } catch (NullPointerException e) {
            //发生空指针异常，表示用户未登录（该情况理论上会被拦截器拦截，该处为了保险，做二次校验）
            log.info("[记录模块] 用户未登录！");
            resultPackage.setMessage("用户未登录！");
            resultPackage.setState(911);
            return resultPackage;
        }

        //确认数据有效后，查询数据
        List<PlayerRecord> data = recordService.getPlayerRecord(user.getUid());
        resultPackage.setState(301);
        resultPackage.setData(data);
        resultPackage.setIsOk(true);
        resultPackage.setMessage("成功获取数据！");
        log.info("[记录模块] 成功获取玩家对战记录数据！");
        return resultPackage;
    }
}
