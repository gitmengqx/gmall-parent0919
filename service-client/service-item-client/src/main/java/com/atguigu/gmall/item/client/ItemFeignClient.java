package com.atguigu.gmall.item.client;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.client.impl.ItemDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * @author mqx
 * @date 2020/3/18 11:51
 */
@FeignClient(name = "service-item",fallback = ItemDegradeFeignClient.class)
public interface ItemFeignClient {
    // 把service-item 中的服务暴露出来
    // 根据skuId 查询数据
    @GetMapping("api/item/{skuId}")
    Result getItem(@PathVariable Long skuId);


}
