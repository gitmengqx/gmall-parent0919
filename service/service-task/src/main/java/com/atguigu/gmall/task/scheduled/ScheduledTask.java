package com.atguigu.gmall.task.scheduled;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import com.atguigu.gmall.common.service.RabbitService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author mqx
 * @date 2020/3/31 9:21
 */
@Component
@EnableScheduling // 表示开启定时任务
@Slf4j
public class ScheduledTask {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RabbitService rabbitService;

    // 定义定时任务规则 分，时，日，月，周，年
//    @Scheduled(cron = "0/20 * * * * ?")
//    public void testTask(){
//        log.info("每20秒执行一次");
//        // 在这个任务中扫描缓存队列 List 中的数据 ，这里的数据就是我们需要重新发送的消息。
//        String msg = (String) redisTemplate.opsForList().rightPop(MqConst.MQ_KEY_PREFIX);
//        // 如果从缓存中获取的数据是空那么就返回
//        if (StringUtils.isEmpty(msg)) return;
//        // 如果缓存获取的数据不是空，将msg消息转化为对象
//        GmallCorrelationData gmallCorrelationData = JSON.parseObject(msg, GmallCorrelationData.class);
//        // 每隔20秒扫描一次
//        if (gmallCorrelationData.isDelay()){
//            // 处理延迟消息 写案例：项目中需要使用的话！
//            rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),
//                    gmallCorrelationData.getMessage(),message -> {
//                    // 设置延迟时间
//                    message.getMessageProperties().setDelay(gmallCorrelationData.getDelayTime()*1000);
//                    return message;
//                    },gmallCorrelationData);
//        } else {
//          // 处理非延迟消息
//          // 将该对象重新发送！
//          rabbitTemplate.convertAndSend(gmallCorrelationData.getExchange(),gmallCorrelationData.getRoutingKey(),
//                gmallCorrelationData.getMessage(),gmallCorrelationData);
//
//        }
//    }

//    @Scheduled(cron = "0 0 1 * * ?") 2020-4-6凌晨1点钟的定时任务表达式
    @Scheduled(cron = "0/30 * * * * ?")
    public void taskActivity(){
        // 发送一个消息队列 只是提示消费者查询秒杀的商品而已！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");
    }
    // 每天下午18点钟删除缓存数据
    @Scheduled(cron = "0 0 18 * * ?")
    public void taskActivitydelete(){
        // 发送一个消息队列 只是提示消费者查询秒杀的商品而已！
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"");
    }
}
