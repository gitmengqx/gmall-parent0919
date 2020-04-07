package com.atguigu.gmall.common.config;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.entity.GmallCorrelationData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author mqx
 * @date 2020/3/30 14:06
 *
 * @Description 消息发送确认
 * <p>
 * ConfirmCallback  只确认消息是否正确到达 Exchange 中
 * ReturnCallback   消息没有正确到达队列时触发回调，如果正确到达队列不执行
 * <p>
 * 1. 如果消息没有到exchange,则confirm回调,ack=false
 * 2. 如果消息到达exchange,则confirm回调,ack=true
 * 3. exchange到queue成功,则不回调return
 * 4. exchange到queue失败,则回调return
 *
 */
@Component
@Slf4j
public class MQProducerAckConfig  implements RabbitTemplate.ConfirmCallback,RabbitTemplate.ReturnCallback {

    // 引入操作消息队列的模板
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RedisTemplate redisTemplate;

    // 还需要做一个初始化方法
    @PostConstruct
    public void init(){
        rabbitTemplate.setReturnCallback(this);
        rabbitTemplate.setConfirmCallback(this);
    }

    /**
     * 表示判断消息是否能够发送到交换机上
     * @param correlationData 封装发送数据 消息的载体
     * @param ack 消息是否到交换机
     * @param cause 获取响应信息
     */
    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        // 返回true
        if (ack){
            log.info("发送成功"+ JSON.toJSONString(correlationData));
        } else {
            log.info("发送失败"+ JSON.toJSONString(correlationData));
            // 需要重新发送的！
//            this.addRetry(correlationData);
        }
    }

    /**
     * 如果消息没有走到队列，则会走该方法
     * @param message 消息内容
     * @param replyCode 应答码
     * @param replyText 描述
     * @param exchange 交换机
     * @param routingKey 路由键
     */
    @Override
    public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
        // 反序列化对象输出
        System.out.println("消息主体: " + new String(message.getBody()));
        System.out.println("应答码: " + replyCode);
        System.out.println("描述：" + replyText);
        System.out.println("消息使用的交换器 exchange : " + exchange);
        System.out.println("消息使用的路由键 routing : " + routingKey);

        // 如果没有发送到队列中才会走这个方法。
//        String correlationDataId = (String) message.getMessageProperties().getHeaders().get("spring_returned_message_correlation");
//        System.out.println("CorrelationDataId: " + correlationDataId);
//
//        // 发送消息的时候的 存储了GmallCorrelationData 对象
//        //  JSON.parseObject(); 第一个参数是String ，第二个参数是获取到的数据应该是哪种数据类型 GmallCorrelationData.class
//        GmallCorrelationData gmallCorrelationData = JSON.parseObject((String)redisTemplate.opsForValue().get(correlationDataId),GmallCorrelationData.class);
//
//        // 重新发送消息 消息的内容，都被封装到gmallCorrelationData
//        this.addRetry(gmallCorrelationData);
    }

    // 重新发送消息
    private void addRetry(CorrelationData correlationData) {
        // 强制类型转化！
        GmallCorrelationData gmallCorrelationData =(GmallCorrelationData) correlationData;
        // 重试次数
        int retryCount = gmallCorrelationData.getRetryCount();
        // 重试了三次以上
        if (retryCount >= MqConst.RETRY_COUNT){
            System.out.println("重试失败！");
        } else {
            // 更新次数
            retryCount+=1;
            // 将更新过的次数放入对象中
            gmallCorrelationData.setRetryCount(retryCount);
            // 将数据重新放入一个 List 集合中！
            redisTemplate.opsForList().leftPush(MqConst.MQ_KEY_PREFIX,JSON.toJSONString(gmallCorrelationData));
            // 更新次数
            redisTemplate.opsForValue().set(gmallCorrelationData.getId(),JSON.toJSONString(gmallCorrelationData));
        }
    }
}
