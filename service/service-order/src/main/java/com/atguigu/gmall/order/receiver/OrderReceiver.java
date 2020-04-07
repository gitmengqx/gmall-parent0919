package com.atguigu.gmall.order.receiver;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.order.service.OrderService;
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
    // 获取监听到的消息队列
    @SneakyThrows
    @RabbitListener(queues = MqConst.QUEUE_ORDER_CANCEL)
    public void orderCancel(Long orderId , Message message, Channel channel){
        if (orderId!=null){
            // 那么如果当前的订单已经付完款了。
            // 判断当前订单的状态！当前订单状态是为付款，order_status UNPAID 这个时候才关闭订单！
            // 先查询一下当前的订单！
            // 扩展：判断当前订单是否真正的支付{在支付宝中是否有交易记录。双保险！}
            OrderInfo orderInfo = orderService.getById(orderId);
            /**
             *  面试可能会问你，关单的业务逻辑！
             *  判断 paymentInfo 中有没有交易记录，是否是未付款！
             *  判断 orderInfo 订单的状态
             *  判断在支付宝中是否有交易记录。
             *  如果有交易记录{扫描了二维码} alipayService.checkPayment(orderId)
             *  如果在支付宝中有交易记录，调用关闭支付的订单接口。如果正常关闭了，那么说明，用户根本没有付款。如果关闭失败。
             *      说明用户已经付款成功了。 发送消息队列更新订单的状态！ 通知仓库，减库存。
             *      rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_PAYMENT_PAY,MqConst.ROUTING_PAYMENT_PAY,paymentInfo.getOrderId());
             *  关闭订单：
             *      1.  用户没有点到付款，二维码都没有出现。
             *      2.  系统出现了二维码，但是用户并没有扫描。
             *      3.  系统出现了二维码，用户扫了，但是没有输入密码。
             */

            if (orderInfo!=null && orderInfo.getOrderStatus().equals(ProcessStatus.UNPAID.getOrderStatus().name())){
                // 修改订单的状态！订单的状态变成CLOSED
                orderService.execExpiredOrder(orderId);
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
