package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/1 11:52
 */
@Controller
@RequestMapping("api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;

    @Autowired
    private PaymentService paymentService;

    // 配置对应提交过来的映射
    @RequestMapping("submit/{orderId}")
    @ResponseBody // 有两个作用，第一个：直接返回Json 字符串 底层jackson.jar 第二个：直接将字符串渲染到页面！
    public String submitOrder(@PathVariable Long orderId){
        String from = "";
        try {
            // 该方法调用完成之后，返回的是生产二维码的字符串！
            from = alipayService.careteAliPay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 如何将from 渲染到页面生产二维码！
        return from; //对应找到from的变量值的html
    }

    // http://api.gmall.com/api/payment/alipay/callback/return
    @RequestMapping("callback/return")
    public String callBack(){
        // 支付成功页面！ 重定向到订单页面
        // return_order_url=http://payment.gmall.com/pay/success.html
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    // 异步回调的地址！
    // notify_payment_url=http://2kyixc.natappfree.cc/api/payment/alipay/callback/notify
    // 当用户支付完成之后，支付会发送一个异步通知{发送到公网【外网】} ，我们需要从公网上获取到这个通知
    // 我们的服务器linux ，那么我这个服务器是在公网上么？ 我们需要使用内网穿透技术获取支付宝发送的异步通知
    //  http://2kyixc.natappfree.cc -> 127.0.0.1:80  127.0.0.1:80其实就是我们的网关
    //  微信：异步回调的时候，发送很多次 1s,2s,...... 24H
    @RequestMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String ,String> paramMap){
        // Map<String, String> paramsMap = ... //将异步通知中收到的所有参数都存放到map中
        boolean signVerified = false;
        try {
            signVerified = AlipaySignature.rsaCheckV1(paramMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type); //调用SDK验证签名
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        // 获取交易状态
        String trade_status = paramMap.get("trade_status");
        String out_trade_no = paramMap.get("out_trade_no");
        // 获取out_trade_no
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(trade_status) || "TRADE_FINISHED".equals(trade_status)){
                // 更新交易记录的状态！
                // 在更改交易记录状态的时候，需要先做一个判断！如果说当前的交易记录中的状态已经是PAID或者是CLOSE 那么不能更新状态
                // 根据out_trade_no 还有payment_type支付方式
                // select * from payment_info where out_trade_no = out_trade_no and payment_type=ALIPAY
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(out_trade_no, PaymentType.ALIPAY.name());
                // 判断业务
                if (paymentInfo.getPaymentStatus()==PaymentStatus.PAID.name() ||
                    paymentInfo.getPaymentStatus()==PaymentStatus.ClOSED.name()){
                    return "failure";
                }
                // 正常支付成功了！更改交易记录状态！
                // update payment_info set payment_status=PAID ,callback_content=paramMap where out_trade_no = out_trade_no and payment_type=ALIPAY
                paymentService.paySuccess(out_trade_no,PaymentType.ALIPAY.name(),paramMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }

    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable Long orderId){
        // 调用退款接口
        boolean flag = alipayService.refund(orderId);

        return Result.ok(flag);
    }

    // 测试一下刚才关闭的是否好使！
    @RequestMapping("closePay/{orderId}")
    @ResponseBody
    public Boolean closePay(@PathVariable Long orderId){
        // 调用退款接口
        boolean flag = alipayService.closePay(orderId);
        return flag;
    }

    // 查看是否有过交易
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        // 调用退款接口
        boolean flag = alipayService.checkPayment(orderId);
        return flag;
    }

    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
        if (null!=paymentInfo){
            return paymentInfo;
        }
        return null;
    }


}
