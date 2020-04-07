package com.atguigu.gmall.item.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.item.service.ItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * @author mqx
 * @date 2020/3/17 14:34
 */
@RestController
@RequestMapping("api/item")
public class ItemApiController {

    @Autowired
    private ItemService itemService;
    // 通过商品Id 访问
    @RequestMapping("{skuId}")
    public Result getItem(@PathVariable Long skuId){
        Map<String, Object> map = itemService.getBySkuId(skuId);
        return Result.ok(map);
    }
}
