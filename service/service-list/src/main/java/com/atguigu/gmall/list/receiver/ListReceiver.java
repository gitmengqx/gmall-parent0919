package com.atguigu.gmall.list.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.list.service.SearchService;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author mqx
 * @date 2020/3/31 10:29
 */
@Component
public class ListReceiver {

    @Autowired
    private SearchService searchService;
    // 获取队列中的消息！ 商品的上架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_UPPER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS,type = ExchangeTypes.DIRECT,durable = "true"),
            key = {MqConst.ROUTING_GOODS_UPPER}
    ))
    public void goodsUpper(Long skuId, Message message, Channel channel){
        if (skuId!=null){
            // 调用service-list 项目中商品上架的方法
            searchService.upperGoods(skuId);
        }
        // 确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

    // 商品的下架
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_GOODS_LOWER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_GOODS,durable = "true"),
            key = {MqConst.ROUTING_GOODS_LOWER}
    ))
    public void goodsLower(Long skuId ,Message message,Channel channel){
        if (skuId!=null){
            // 调用service-list 商品下架的方法
            searchService.lowerGoods(skuId);
        }
        // 手动确认
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }

}
