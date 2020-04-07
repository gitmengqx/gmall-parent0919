package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.product.service.ManageService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * @date 2020/3/16 9:22
 */
@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("baseSaleAttrList")
    public Result<List<BaseSaleAttr>> getBaseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrList = manageService.getBaseSaleAttrList();

        // 返回数据
        return Result.ok(baseSaleAttrList);
    }
    // 使用@RequestBody 将页面传递过来的json 数据转化为java 对象？
    // java 对象是谁？ vo 通常是用来展示数据 | dto 数据传输的？
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        manageService.saveSpuInfo(spuInfo);
        return Result.ok();
    }
}
