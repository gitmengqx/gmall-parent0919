package com.atguigu.gmall.activity.service.impl;

import com.atguigu.gmall.activity.mapper.SeckillGoodsMapper;
import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * @date 2020/4/6 11:30
 */
@Service
public class SeckillGoodsServiceImpl implements SeckillGoodsService {

    @Autowired
    private SeckillGoodsMapper seckillGoodsMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
    public List<SeckillGoods> findAll() {
        // 直接获取缓存的数据 获取hash 数据结构中的商品数据
        List<SeckillGoods> list = redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).values();
        // 返回所有的秒杀商品集合
        return list;
    }

    @Override
    public SeckillGoods getSecKillGoodsById(Long id) {
        // 也是从hash 结果中获取Id对应的数据
        SeckillGoods seckillGoods = (SeckillGoods) redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).get(id.toString());
        return seckillGoods;
    }

    // 预下单
    @Override
    public void seckillOrder(Long skuId, String userId) {
        // 判断状态位
        String state = (String) CacheHelper.get(skuId.toString());
        // 已经售罄
        if ("0".equals(state)){
            return;
        }
        // 用户是否已经下过订单
        // setnx(key,value,timeout,timeUnit)
        // key = seckill:user:userId value =skuId
        Boolean isExist = redisTemplate.opsForValue().setIfAbsent(RedisConst.SECKILL_USER + userId, skuId, RedisConst.SECKILL__TIMEOUT, TimeUnit.SECONDS);
        if (!isExist){
            return;
        }
        // 获取商品
        String goodsId = (String) redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).rightPop();
        // 判断goodsId 是否合法
        if (StringUtils.isEmpty(goodsId)){
            redisTemplate.convertAndSend("seckillpush",skuId+":0");
            // 已经售罄
            return;
        }
        // 订单记录
        OrderRecode orderRecode = new OrderRecode();
        orderRecode.setUserId(userId);
        // 数量一个
        orderRecode.setNum(1);
        // 生产下单码
        orderRecode.setOrderStr(MD5.encrypt(userId));
        // 根据商品Id 查询秒杀商品的方法
        orderRecode.setSeckillGoods(getSecKillGoodsById(skuId));

        // 将订单存储到缓存
        // hset(key,field,value) key = seckill:orders field = userId value = orderRecode
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).put(orderRecode.getUserId(),orderRecode);

        // 更新库存
        updateStockCount(orderRecode.getSeckillGoods().getSkuId());

    }

    @Override
    public Result checkOrder(Long skuId, String userId) {
        // 检查订单
        // 检查用户在缓存中是否存在
        Boolean isExist = redisTemplate.hasKey(RedisConst.SECKILL_USER + userId);
        // 用户在缓存中
        if (isExist){
            // 用户已经下单了，并且存储在缓存中
            Boolean isHasKey = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).hasKey(userId);
            if (isHasKey){
                // 抢单成功！
                OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
                return Result.build(orderRecode, ResultCodeEnum.SECKILL_SUCCESS);
            }
        }
        // 判断订单
        Boolean isExistOrder = redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).hasKey(userId);
        // 如果缓存中有当前的订单了，则不能重复下单
        if (isExistOrder){

            // 获取订单
            String orderId = (String) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).get(userId);

            // 返回 下单成功！
            return Result.build(orderId,ResultCodeEnum.SECKILL_ORDER_SUCCESS);
        }

        // 判断商品的状态位
        String status = (String) CacheHelper.get(skuId.toString());
        if ("0".equals(status)){
            // 已经售罄抢单失败！
            return Result.build(null,ResultCodeEnum.SECKILL_FAIL);
        }
        return Result.build(null,ResultCodeEnum.SECKILL_RUN);
    }

    // 表示更新库存
    private void updateStockCount(Long skuId) {
        // 先获取到当前的库存剩余数量
        // key = seckill:stock:skuId
        Long stockSize = redisTemplate.boundListOps(RedisConst.SECKILL_STOCK_PREFIX + skuId).size();

        // 判断很有意思！ 不要频繁更新数据
        if (stockSize%2==0){
            SeckillGoods secKillGoods = getSecKillGoodsById(skuId);
            secKillGoods.setStockCount(stockSize.intValue());
            // 更新数据库
            seckillGoodsMapper.updateById(secKillGoods);

            // 更新缓存
            redisTemplate.boundHashOps(RedisConst.SECKILL_GOODS).put(skuId.toString(),secKillGoods);
        }
    }

}
