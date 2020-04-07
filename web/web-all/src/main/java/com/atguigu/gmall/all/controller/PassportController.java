package com.atguigu.gmall.all.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/3/25 14:13
 */
@Controller
public class PassportController {

    // http://passport.gmall.com/login.html?originUrl=http://www.gmall.com/
    // @RequestMapping("login.html")
    @GetMapping("login.html")
    public String login(HttpServletRequest request){
        // originUrl=http://www.gmall.com
        // 如果登录成功，那么返回http://www.gmall.com 路径
        String originUrl = request.getParameter("originUrl");
        // 保存起来
        request.setAttribute("originUrl",originUrl);
        // 返回登录页面
        return "login";
    }
}
