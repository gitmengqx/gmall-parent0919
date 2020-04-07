package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;

/**
 * @author mqx
 * @date 2020/3/23 15:32
 */
public interface SearchService {
    // 商品的上架 skuId
    void upperGoods(Long skuId);

    // 商品的下架 skuId
    void lowerGoods(Long skuId);

    // 热度更新
    void incrHotScore(Long skuId);

    // 检索列表
    SearchResponseVo search(SearchParam searchParam) throws IOException;

}
