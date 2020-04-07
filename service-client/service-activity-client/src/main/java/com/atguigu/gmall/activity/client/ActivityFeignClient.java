package com.atguigu.gmall.activity.client;

import com.atguigu.gmall.activity.client.impl.ActivityDegradeFeignClient;
import com.atguigu.gmall.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/4/6 11:40
 */
@FeignClient(value = "service-activity",fallback = ActivityDegradeFeignClient.class)
public interface ActivityFeignClient {
    // 对应的数据接口卡封装好

    /**
     * 获取所有的秒杀商品数据
     * @return
     */
    @GetMapping("/api/activity/seckill/findAll")
    Result findAll();

    @GetMapping("/api/activity/seckill/getSeckillGoods/{skuId}")
    Result getSeckillGoods(@PathVariable Long skuId);


    /**
     * 秒杀确认订单
     * @return
     */
    @GetMapping("/api/activity/seckill/auth/trade")
    Result<Map<String, Object>> trade();
}
