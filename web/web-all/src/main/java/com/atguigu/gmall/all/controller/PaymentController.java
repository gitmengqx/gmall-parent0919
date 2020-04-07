package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.client.OrderFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/4/1 9:30
 */
@Controller
public class PaymentController {

    @Autowired
    private OrderFeignClient orderFeignClient;
    // http://payment.gmall.com/pay.html?orderId=15
    // request model 都可以存储！
    @GetMapping("pay.html")
    public String pay(HttpServletRequest request){
        // 获取订单的Id
        String orderId = request.getParameter("orderId");
        OrderInfo orderInfo = orderFeignClient.getOrderInfo(Long.parseLong(orderId));
        // 后台应该存储一个orderInfo 的对象
        request.setAttribute("orderInfo",orderInfo);
        return "payment/pay";
    }

    // 设置同步回调的url
    // pay/success.html
    @GetMapping("pay/success.html")
    public String success(){
        return "payment/success";
    }

}
