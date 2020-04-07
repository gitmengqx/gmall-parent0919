package com.atguigu.gmall.common.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * @author mqx
 * 获取消息 exchange.confirm routing.confirm hello rabbitmq!
 * @date 2020/3/30 14:34
 */
@Component
@Configuration
public class ConfirmReceiver {

    // rabbitmq 使用注解形式来获取消息队列中的数据
    @SneakyThrows // 忽略异常
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm",autoDelete = "false",durable = "true"),
            exchange = @Exchange(value = "exchange.confirm",autoDelete = "true"),
            key = {"routing.confirm"}
    ))
    public void process(Message message, Channel channel){
        // 声明一个字符串
        String msg = new String(message.getBody());
        System.out.println("获取到的消息："+msg);

        // 确认消息 第二个参数：false 每次只确认一条信息。 true 表示批量确认消息
        try {
//            int i = 1/0;
            // 没有确认呢，那么就抛出异常了！
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        } catch (Exception e) {
            // 一个消息能够被重复消息?不能！
            // 这有个方法 能够判断消息是否已经处理过了！
            // 此方法会判断 当前的消息是否有被确认过？{ack或者nack} 如果确认过 true ，没有确认过false
            if (message.getMessageProperties().getRedelivered()){
                System.out.println("消息已经重复处理，拒绝再次接收消费！");
                // 拒绝消息,不能重新进入队列
                channel.basicReject(message.getMessageProperties().getDeliveryTag(),false);
            }else {
                System.out.println("消息即将再次进入队列！");
                // 第三个参数：表示消息是否重回队列
                channel.basicNack(message.getMessageProperties().getDeliveryTag(),false,true);
            }
        }
    }
}
