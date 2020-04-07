package com.atguigu.gmall.common.receiver;

import com.atguigu.gmall.common.config.DeadLetterMqConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author mqx
 * @date 2020/3/31 11:37
 */
@Component
@Configuration
public class DeadLetterReceiver {

    // 监听消息
    @RabbitListener(queues = DeadLetterMqConfig.queue_dead_2)
    public void getMsg(String msg){
        System.out.println("Receive:" + msg);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        System.out.println("Receive queue_dead_2: " + sdf.format(new Date()) + " Delay rece." + msg);

    }
}
