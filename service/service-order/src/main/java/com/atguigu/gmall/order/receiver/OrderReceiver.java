package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.payment.client.PaymentFeignClient;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/3/31 15:18
 */
@Component
public class OrderReceiver {

    @Autowired
    private OrderService orderService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private PaymentFeignClient paymentFeignClient;
    // 获取监听到的消息队列
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId , Message message, Channel channel){
        if (orderId!=null){
            // 根据订单Id 查询订单记录
            OrderInfo orderInfo = orderService.getById(orderId);

            if (null!= orderInfo){
                PaymentInfo paymentInfo = paymentFeignClient.getPaymentInfo(orderInfo.getOutTradeNo());
                // 先检查支付交易记录
                if (null!=paymentInfo && paymentInfo.getPaymentStatus().equals(PaymentStatus.UNPAID.name())){
                    // 关闭支付宝,先判断是否有交易记录。如果有交易记录，没有付款成功那么才能关闭成功！否则关闭失败。
                    Boolean flag = paymentFeignClient.checkPayment(orderId);
                    // 如果返回true ，则说明有交易记录
                    if (flag){
                        // 有交易记录，那么才调用关闭支付接口
                        Boolean result = paymentFeignClient.closePay(orderInfo.getId());
                        // 如果关闭成功，则说明用户没有付款，
                        if (result){
                            // 关闭交易记录状态，更改订单状态。
                            orderService.execExpiredOrder(orderId,"2");
                        }else {
                            // 说明没有关闭成功，那么一定是用户付款了。
                            // 发送支付成功的消息队列。
                            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
                        }
                    }else {
                        // 用户生成了二维码，但是没有扫描。
                        orderService.execExpiredOrder(orderId,"2");
                    }
                } else{
                  /*
                   支付交易记录中为空，那么说明用户根本没有生成付款码。没有生成付款码，那么可能下单了。
                   所以关闭订单的状态
                   */
                    if (orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                        orderService.execExpiredOrder(orderId,"1");
                    }
                }
            }
        }
        // 手动ack
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    // 监听支付完成之后的消息队列
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_PAYMENT_PAY,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_PAYMENT_PAY),
            key = {MqConst.ROUTING_PAYMENT_PAY}
    ))
    public void paySuccess(Long orderId,Message message,Channel channel){
        // 判断当前orderId 不能为空
        if (null != orderId){
            // 做准备更新订单的状态！ 再次确认订单的状态
            OrderInfo orderInfo = orderService.getById(orderId);
            // 判断在orderInfo 中有当前记录，并且进程状态，订单状态为未付款！
            if (null != orderInfo && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                // 更新订单
                orderService.updateOrderStatus(orderId,ProcessStatus.PAID);
                // 发送消息通知库存系统，减库存！
                // 库存要想减库存，根据订单的Id orderId
                orderService.sendOrderStatus(orderId);
            }
        }
        // 收到确认消息处理完毕！
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_WARE_ORDER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_WARE_ORDER),
            key = {MqConst.ROUTING_WARE_ORDER}
    ))
    public void updateOrderStatus(String msgJson,Message message,Channel channel){
        // 先判断当前消息不能为空
        if (!StringUtils.isEmpty(msgJson)){
            // 将json 转化为map 集合
            Map map = JSON.parseObject(msgJson, Map.class);
            String orderId = (String) map.get("orderId");
            String status = (String) map.get("status");

            // 判断减库存结果
            if("DEDUCTED".equals(status)){
                // 减库存成功，则需要更改订单的状态
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.WAITING_DELEVER);

            } else {
                // 如果库存异常，那么可能是超卖！
                // 1. 补货， 2. 人工客服与买家协商。
                orderService.updateOrderStatus(Long.parseLong(orderId),ProcessStatus.STOCK_EXCEPTION);
            }

        }
        // 手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
