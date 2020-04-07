package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * @date 2020/3/14 14:52
 */
@RestController
@RequestMapping("admin/product/baseTrademark")
public class BaseTrademarkController {

    @Autowired
    private BaseTrademarkService baseTrademarkService;

    /**
     * 按照分页查询数据
     * @param page
     * @param limit
     * @return
     */
    // http://api.gmall.com/admin/product/baseTrademark/1/10
    @GetMapping("{page}/{limit}")
    public Result index(@ApiParam(name = "page",value = "当前页码",required = true) @PathVariable Long page,
                        @ApiParam(name = "limit",value = "每页的记录数",required = true) @PathVariable Long limit
                        ){

        Page<BaseTrademark> baseTrademarkPage = new Page<>(page,limit);
        IPage<BaseTrademark> baseTrademarkIPage = baseTrademarkService.selectPage(baseTrademarkPage);

        return Result.ok(baseTrademarkIPage);

    }

    // 根据品牌Id 查询数据并回显。。。
    @GetMapping("get/{id}")
    public Result get(@PathVariable Long id){
        // getById
        System.out.println("根据Id获取品牌数据方法");
        BaseTrademark baseTrademark = baseTrademarkService.getById(id);
        return Result.ok(baseTrademark);
    }

    // 点击保存按钮的时候save
    // 将json 数据转换为JavaObject
    // @RequestBody ：将页面提交过来的json 字符串转换为 java 对象
    // {"tradeMarkName":"三星","url":"http...."}
    @PostMapping("save")
    public Result save(@RequestBody BaseTrademark baseTrademark){
        System.out.println("保存方法");
        baseTrademarkService.save(baseTrademark);

        return Result.ok();
    }

    // 删除方法
    @DeleteMapping("remove/{id}")
    public Result remove(@PathVariable Long id){
        System.out.println("删除方法");
        baseTrademarkService.removeById(id);
        return Result.ok();
    }

    // 如果有修改的控制器，则走update。
    @PutMapping("update")
    public Result update(@RequestBody BaseTrademark baseTrademark){
        System.out.println("修改方法");
        baseTrademarkService.updateById(baseTrademark);
        return Result.ok();
    }

    @GetMapping("getTrademarkList")
    public Result getTrademarkList(){
        List<BaseTrademark> baseTrademarkList = baseTrademarkService.getBaseTrademarkList();

        return Result.ok(baseTrademarkList);
    }


}
