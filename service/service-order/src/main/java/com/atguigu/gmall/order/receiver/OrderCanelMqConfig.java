package com.atguigu.gmall.order.receiver;

import com.atguigu.gmall.common.constant.MqConst;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

/**
 * @author mqx
 * @date 2020/3/31 15:05
 */
@Configuration
public class OrderCanelMqConfig {

    // 定义队列
    @Bean
    public Queue dealyQueue(){
        // 订单的队列
        return new Queue(MqConst.QUEUE_ORDER_CANCEL,true);
    }

    // 定义我们这个交换机
    @Bean
    public CustomExchange dealyExchange(){
        // 定义一个map 
        HashMap<String, Object> map = new HashMap<>();
        // 封装对应的参数
        map.put("x-delayed-type","direct");
        return new CustomExchange(MqConst.EXCHANGE_DIRECT_ORDER_CANCEL,"x-delayed-message",
                true,false,map);
    }
    // 定义一个绑定
    @Bean
    public Binding delayBinding(){
        return BindingBuilder.bind(dealyQueue()).to(dealyExchange()).with(MqConst.ROUTING_ORDER_CANCEL).noargs();
    }
}
