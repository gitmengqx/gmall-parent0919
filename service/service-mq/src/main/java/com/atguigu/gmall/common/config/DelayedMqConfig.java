package com.atguigu.gmall.common.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;


/**
 * @author mqx
 * @date 2020/3/31 14:12
 */
@Configuration
public class DelayedMqConfig {
    public static final String exchange_delay = "exchange.delay";
    public static final String routing_delay = "routing.delay";
    public static final String queue_delay_1 = "queue.delay.1";

    // 声明一个队列
    @Bean
    public Queue delayQueue(){
        return new Queue(queue_delay_1,true);
    }
    // 声明交换机
    @Bean
    public Exchange delayExchange(){
        HashMap<String, Object> map = new HashMap<>();
        // 设置基于插件做延迟队列的交换机
        map.put("x-delayed-type","direct");
        // 自定义交换机
        return new CustomExchange(exchange_delay,"x-delayed-message",true,false,map);
    }

    // 设置绑定
    @Bean
    public Binding delayBinding(){
        // 完成了，延迟队列的绑定！
        return BindingBuilder.bind(delayQueue()).to(delayExchange()).with(routing_delay).noargs();
    }

}
