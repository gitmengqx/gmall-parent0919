package com.atguigu.gmall.activity.receiver;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * @author mqx
 * 只需要根据条件查询秒杀的商品
 * @date 2020/4/6 9:43
 */
@Component // 将这个类注入到spring容器中
public class SeckillReceiver {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    // 监听消息队列
    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_1),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_1}
    ))
    public void importGoods(Message message, Channel channel){
        // 什么样的商品才算是秒杀的商品！
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        // 秒杀商品表中审核状态位 1 的时候，表示审核通过，其他数据则不是秒杀商品！
        // 商品的库存剩余数量>0
        seckillGoodsQueryWrapper.eq("status",1).gt("stock_count",0);
        // 当前的时间必须为今天的日期：只查询今天要被秒杀的商品！
        seckillGoodsQueryWrapper.eq("DATE_FORMAT(start_time,'%Y-%m-%d')", DateUtil.formatDate(new Date()));
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        // 当前集合就是被秒杀的商品集合数据
        // 将商品集合数据放入缓存。。等一系列操作。。。。
        if (seckillGoodsList!=null && seckillGoodsList.size()>0){
            // 循环遍历当前的集合
            for (SeckillGoods seckillGoods : seckillGoodsList) {
                // 遍历出来放入缓存中 以什么数据类型来存储数据
                // 使用hash 来存储 hset(key,field,value)
                // key = seckill:goods field = skuId value = SeckillGoods 要秒杀的商品
                // 先判断缓存中是否已经存在当前的秒杀商品，如果有则不放入，没有则放入数据
                Boolean flag = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).hasKey(seckillGoods.getSkuId().toString());
                // 说明缓存中已经有当前的秒杀的商品了。
                if (flag){
                    continue;
                }
                // 将秒杀的商品放入缓存 数据结构是 hash
                redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(seckillGoods.getSkuId().toString(),seckillGoods);

                // 如何保证商品不超卖！  seckillGoods.getStockCount() 数量存储到redis-list队列中！
                for (Integer i = 0; i < seckillGoods.getStockCount(); i++) {
                    // key = seckill:stock:skuId
                    // value = skuId
                    redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId()).leftPush(seckillGoods.getSkuId().toString());
                }

                // 发布一个消息：当前所有的商品初始化状态位都是1
                // 初始化所有商品都是可以秒杀的！ skuId:1
                redisTemplate.convertAndSend("seckillpush",seckillGoods.getSkuId()+":1");
            }
            // 手动确认当前消息已被处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_SECKILL_USER,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_SECKILL_USER),
            key = {MqConst.ROUTING_SECKILL_USER}
    ))
    public void seckillUser(UserRecode userRecode,Message message,Channel channel){
        if (null!=userRecode){
            // 预下单
            seckillGoodsService.seckillOrder(userRecode.getSkuId(),userRecode.getUserId());

            // 手动确认消息已经处理
            channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
        }
    }

    @SneakyThrows
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = MqConst.QUEUE_TASK_18,durable = "true"),
            exchange = @Exchange(value = MqConst.EXCHANGE_DIRECT_TASK),
            key = {MqConst.ROUTING_TASK_18}
    ))
    public void clearRedis(Message message,Channel channel){
        // 清空缓存数据
        // 数据库中有一个状态status 1的话是正在秒杀 如果是2的话，那么表示活动结束
        // 查询结束秒杀的商品
        QueryWrapper<SeckillGoods> seckillGoodsQueryWrapper = new QueryWrapper<>();
        seckillGoodsQueryWrapper.eq("status",1);
        seckillGoodsQueryWrapper.le("end_time",new Date());
        List<SeckillGoods> seckillGoodsList = seckillGoodsMapper.selectList(seckillGoodsQueryWrapper);

        // 清空秒杀商品的库存数
        for (SeckillGoods seckillGoods : seckillGoodsList) {
            // del seckill:stock:skuId
            redisTemplate.delete(RedisConst.SECKILL_STOCK_PREFIX+seckillGoods.getSkuId());
        }
        // 清空秒杀的订单
        redisTemplate.delete(RedisConst.SECKILL_ORDERS);
        // 清空秒杀商品
        redisTemplate.delete(RedisConst.SECKILL_GOODS);
        // 删除用户秒杀的订单
        redisTemplate.delete(RedisConst.SECKILL_ORDERS_USERS);

        // 处理数据库 status如果是2的话，那么表示活动结束
        SeckillGoods seckillGoods = new SeckillGoods();
        seckillGoods.setStatus("2");
        seckillGoodsMapper.update(seckillGoods,seckillGoodsQueryWrapper);

        // 手动确认消息
        channel.basicAck(message.getMessageProperties().getDeliveryTag(),false);
    }
}
