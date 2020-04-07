package com.atguigu.gmall.payment.service;

import com.alipay.api.AlipayApiException;

/**
 * @author mqx
 * 支付宝支付接口
 * @date 2020/4/1 11:26
 */
public interface AlipayService {
    // 支付是根据订单Id 支付
    // 返回String 数据类型是因为 要将二维码显示到浏览器上！ @ResponseBody
    String careteAliPay(Long orderId) throws AlipayApiException;

    // 退款接口
    boolean refund(Long orderId);

    // 关闭交易接口
    Boolean closePay(Long orderId);

    /**
     * 根据订单查询是否支付成功！
     * @param orderId
     * @return
     */
    Boolean checkPayment(Long orderId);
}
