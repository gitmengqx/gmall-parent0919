package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;

import java.util.List;

/**
 * @author mqx
 * @date 2020/4/6 11:28
 */
public interface SeckillGoodsService {

    /**
     * 查询所有的秒杀商品
     * @return
     */
    List<SeckillGoods> findAll();

    /**
     * 根据Id查询秒杀商品
     * @param id
     * @return
     */
    SeckillGoods getSecKillGoodsById(Long id);

    /**
     * 预下单接口
     * @param skuId
     * @param userId
     */
    void seckillOrder(Long skuId, String userId);

    /**
     * 检查订单的接口
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId, String userId);
}
