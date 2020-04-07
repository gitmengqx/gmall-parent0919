package com.atguigu.gmall.activity.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisChannelConfig {

    /*
         docker exec -it  bc92 redis-cli
         subscribe seckillpush // 订阅 接收消息
         publish seckillpush admin // 发布消息
     */
    /**
     * 注入订阅主题
     * @param connectionFactory redis 链接工厂
     * @param listenerAdapter 消息监听适配器
     * @return 订阅主题对象
     */
    @Bean
    RedisMessageListenerContainer container(RedisConnectionFactory connectionFactory,
                                            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        //订阅主题
        container.addMessageListener(listenerAdapter, new PatternTopic("seckillpush"));
        //这个container 可以添加多个 messageListener
        return container;
    }

    /**
     * 返回消息监听器
     * @param receiver 创建接收消息对象
     * @return
     */
    @Bean
    MessageListenerAdapter listenerAdapter(MessageReceive receiver) {
        //这个地方 是给 messageListenerAdapter 传入一个消息接受的处理器，利用反射的方法调用“receiveMessage”
        //也有好几个重载方法，这边默认调用处理器的方法 叫handleMessage 可以自己到源码里面看
        return new MessageListenerAdapter(receiver, "receiveMessage");
    }

    @Bean //注入操作数据的 template
    StringRedisTemplate template(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

}
