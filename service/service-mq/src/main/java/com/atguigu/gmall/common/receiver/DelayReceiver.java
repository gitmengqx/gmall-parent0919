package com.atguigu.gmall.common.receiver;

import com.atguigu.gmall.common.config.DelayedMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * @date 2020/3/31 14:25
 */
@Configuration
@Component
public class DelayReceiver {

    @RabbitListener(queues = DelayedMqConfig.queue_delay_1)
    public void getMsg(String msg){
        // 设置发送的消息
        SimpleDateFormat sdf =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Receive queue_delay_1: " + sdf.format(new Date()) + " Delay rece." + msg);

    }
}
