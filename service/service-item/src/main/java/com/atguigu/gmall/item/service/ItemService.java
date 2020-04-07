package com.atguigu.gmall.item.service;

import java.util.Map;

/**
 * @author mqx
 * 汇总数据的接口
 * @date 2020/3/17 14:29
 */
public interface ItemService {

    // 如何定义数据接口？ sku基本信息，sku分类信息，sku图片信息

    /**
     *
     * @param skuId
     * map.put("skuInfo",skuInfo)
     * map.put("categoryView",categoryView)
     * map.put("skuImage",skuImage)
     * @return
     */
    Map<String,Object> getBySkuId(Long skuId);

}
