package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author mqx
 * @date 2020/3/16 15:20
 */
@Api(tags = "商品SKU接口")
@RestController
@RequestMapping("admin/product")
public class SkuManageController {

    @Autowired
    private ManageService manageService;

    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable  Long spuId){

        return Result.ok(manageService.getSpuImageList(spuId));
    }

    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable  Long spuId){
        return Result.ok(manageService.getSpuSaleAttrList(spuId));
    }

    @PostMapping("saveSkuInfo")
    public Result saveSkuInfo(@RequestBody SkuInfo skuInfo){
        manageService.saveSkuInfo(skuInfo);
        return Result.ok();
    }

    @GetMapping("list/{page}/{limit}")
    public Result getSkuInfoPage(@ApiParam(name = "page",value = "当前页",required = true) @PathVariable Long page,
                                 @ApiParam(name = "limit",value = "每页显示的条数",required = true) @PathVariable Long limit
                                 ){
        // 构造Page
        Page<SkuInfo> skuInfoPage = new Page<>(page, limit);
        // 调用服务层方法
        IPage<SkuInfo> skuInfoIPage = manageService.selectPage(skuInfoPage);
        // 将分页的数据返回
        return Result.ok(skuInfoIPage);

    }

    // 商品的上架操作
    @GetMapping("onSale/{skuId}")
    public Result onSale(@PathVariable Long skuId){
        manageService.onSale(skuId);
        return Result.ok();
    }
    // 商品的下架操作
    @GetMapping("cancelSale/{skuId}")
    public Result cancelSale(@PathVariable Long skuId){
        manageService.cancelSale(skuId);
        return Result.ok();
    }
}
