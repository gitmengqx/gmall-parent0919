package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * @author mqx
 * @date 2020/3/23 14:37
 */
@RestController
@RequestMapping("api/list")
public class ListApiController {

    // 引入操作es的api对象
    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Autowired
    private SearchService searchService;

    // 生成对应的mapping
    // localhost:8203/api/list/inner/createIndex 访问，访问完成，那么mapping 则会自动建立
    @GetMapping("inner/createIndex")
    public Result createIndex(){
        // 创建goods 的index
        restTemplate.createIndex(Goods.class);
        // 创建mapping
        restTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    // 商品的上架
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        searchService.upperGoods(skuId);
        return Result.ok();
    }
    // 商品的下架
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        searchService.lowerGoods(skuId);
        return Result.ok();
    }

    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    // 测试的时候，需要传递json 数据
    @PostMapping
    public Result getList(@RequestBody SearchParam searchParam) throws IOException {
        SearchResponseVo responseVo = searchService.search(searchParam);
        return  Result.ok(responseVo);
    }

}
