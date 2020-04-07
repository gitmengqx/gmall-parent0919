package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring5.SpringTemplateEngine;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;

/**
 * @author mqx
 * @date 2020/3/23 10:36
 */
@Controller
public class IndexController {

    @Autowired
    private ProductFeignClient productFeignClient;

    // 引入对象
    @Autowired
    private SpringTemplateEngine templateEngine;
    // 利用模板引擎自动生成一个静态页面！可以直接将页面放入nginx 中
    @GetMapping("createHtml")
    @ResponseBody
    public Result createHtml() throws IOException {
        // 自动渲染数据
        Result result = productFeignClient.getBaseCategoryList();
        // 声明一个context 对象
        Context context = new Context();
        context.setVariable("list",result.getData());
        // 输入页面
        FileWriter fileWriter = new FileWriter("e:\\index.html");
        // 利用模板引擎创建
        templateEngine.process("index/index.html",context,fileWriter);
        return Result.ok();

    }

    // 直接编写一个控制器 走缓存的方式
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){
        Result result = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",result.getData());
        // 返回的视图名称
        return "index/index";
    }

}
