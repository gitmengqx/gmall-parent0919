package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/3/28 15:14
 */
@Controller
public class OrderController {

    @Autowired
    private OrderFeignClient orderFeignClient;

    /**
     * 去结算按钮处理
     * @param model
     * @param request
     * @return
     */
    @GetMapping("trade.html")
    public String trade(Model model, HttpServletRequest request){
        Result<Map<String, Object>> result = orderFeignClient.trade();
        model.addAllAttributes(result.getData());
        return "order/trade";
    }
}
