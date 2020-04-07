package com.atguigu.gmall.payment.service;

import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/1 10:48
 */
public interface PaymentService {


    /**
     * 保存交易记录
     * @param orderInfo 保存交易记录到paymentInfo 中，paymentInfo 中的数据大部分都是来自于orderInfo
     * @param paymentType 交易的类型
     */
    void savePaymentInfo (OrderInfo orderInfo,String paymentType);

    /**
     * 根据第三方交易编号，支付方式查询交易记录对象
     * @param out_trade_no
     * @param name
     * @return
     */
    PaymentInfo getPaymentInfo(String out_trade_no, String name);

    /**
     * 支付成功之后，修改交易记录的状态
     * @param out_trade_no
     * @param name
     * @param paramMap
     */
    void paySuccess(String out_trade_no, String name, Map<String, String> paramMap);

    /**
     * 根据out_trade_no 更新数据
     * @param out_trade_no
     * @param paymentInfoUPd
     */
    void updatePymentInfo(String out_trade_no, PaymentInfo paymentInfoUPd);

    /**
     * 根据订单Id 关闭支付交易
     * @param orderId
     */
    void closePayment(Long orderId);
}
