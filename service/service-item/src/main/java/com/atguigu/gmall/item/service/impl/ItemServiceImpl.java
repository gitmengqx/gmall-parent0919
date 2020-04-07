package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.BaseCategoryView;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.bouncycastle.cert.ocsp.Req;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author mqx
 * @date 2020/3/17 14:32
 */
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;

    @Override
    public Map<String, Object> getBySkuId(Long skuId) {
        // 构建返回map
        Map<String, Object> result = new HashMap<>();
        // 重构代码：异步编排：
        // 需要有返回值 获取skuInfo 数据
        CompletableFuture<SkuInfo> skuCompletableFuture  = CompletableFuture.supplyAsync(() -> {
            // 获取skuInfo 数据
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            result.put("skuInfo", skuInfo);
            return skuInfo;
        }, threadPoolExecutor);

        // 查询所有的销售属性销售属性值回显数据
        CompletableFuture<Void> spuSaleAttrCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuInfo.getId(), skuInfo.getSpuId());
            // 返回数据集
            result.put("spuSaleAttrList", spuSaleAttrList);
        }, threadPoolExecutor);
        
        // 获取对应map 集合数据 点击销售属性值切换的数据
        CompletableFuture<Void> skuValueIdsMapCompletableFuture = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
            // 获取销售属性值Id与商品Id 生产的Map 集合
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            // 将map 转化为json 数据
            String skuValueIdsMapJson = JSON.toJSONString(skuValueIdsMap);
            // 保存json 数据
            result.put("valuesSkuJson", skuValueIdsMapJson);

        }, threadPoolExecutor);

        // 获取商品最新价格价格 BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
        // 没有返回数据？{从数据库 skuInfo.price 后面可能需要对商品价格做一个更新}
        // 不依赖于skuCompletableFuture 此对象也能查询数据  select price from skuInfo where id = skuId
        CompletableFuture<Void> skuPriceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            result.put("price", skuPrice);
        }, threadPoolExecutor);
        // 利用异步编排调用商品热度接口
        CompletableFuture<Void> hotScoreCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        }, threadPoolExecutor);


        // 通过三级分类Id 查询分类数据
            CompletableFuture<Void> categoryViewCompletableFuture  = skuCompletableFuture.thenAcceptAsync(skuInfo -> {
                // 获取分类数据
                BaseCategoryView categoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
                result.put("categoryView", categoryView);
        }, threadPoolExecutor);



        // 将所有线程都进行汇总
        // allOf 后面追加 join();
        CompletableFuture.allOf(skuCompletableFuture,
                spuSaleAttrCompletableFuture,
                skuValueIdsMapCompletableFuture,
                skuPriceCompletableFuture,
                categoryViewCompletableFuture,
                hotScoreCompletableFuture).join();

        // map 返回
        return result;
    }

}
