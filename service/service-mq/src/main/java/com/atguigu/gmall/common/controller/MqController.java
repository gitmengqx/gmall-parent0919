package com.atguigu.gmall.common.controller;

import com.atguigu.gmall.common.config.DeadLetterMqConfig;
import com.atguigu.gmall.common.config.DelayedMqConfig;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * @date 2020/3/30 14:29
 */
@RestController
@RequestMapping("/mq")
@Slf4j
public class MqController {

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // 调用rabbitService.sendMessage();
    @GetMapping("sendConfirm")
    public Result sendConfirm(){
        // 声明一个发送的数据
        String msg = "hello rabbitmq!";
        rabbitService.sendMessage("exchange.confirm","routing.confirm66",msg);
        return Result.ok();
    }

    @GetMapping("sendDeadLettle")
    public Result sendDeadLettle(){
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        //发送消息
        // 方法二 是全局设置的。
        this.rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead, DeadLetterMqConfig.routing_dead_1, "11");
        System.out.println(sdf.format(new Date()) + " Delay sent.");
        // 方法一
//        rabbitTemplate.convertAndSend(DeadLetterMqConfig.exchange_dead,DeadLetterMqConfig.routing_dead_1,"ok",message ->{
//            // 设置消息的过期时间
//            message.getMessageProperties().setExpiration(1000*10+"");
//            System.out.println(sdf.format(new Date()) + " Delay sent.");
//            return message;
//        });
        return Result.ok();
    }

    @GetMapping("sendDelay")
    public Result sendDelay(){
        // 设置发送的消息
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        // 准备发送消息
        rabbitTemplate.convertAndSend(DelayedMqConfig.exchange_delay, DelayedMqConfig.routing_delay,
                sdf.format(new Date()), new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        // 在这个地方设置当前这个消息的延迟时间
                        message.getMessageProperties().setDelay(10*1000);
                        System.out.println(sdf.format(new Date()) + " Delay sent.");
                        return message;
                    }
                }
        );
        return Result.ok();
    }
}
