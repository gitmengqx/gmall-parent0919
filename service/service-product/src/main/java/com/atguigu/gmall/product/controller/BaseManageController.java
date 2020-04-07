package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author mqx
 * @date 2020/3/13 15:39
 */
@Api(tags = "商品基础属性接口")
@RestController
@RequestMapping("admin/product")
public class BaseManageController {

    @Autowired
    private ManageService manageService;

    // @RequestMapping("getCategory1")
    // @RequestMapping(value = "getCategory1",method = RequestMethod.GET) //能够接收get ，post 都可以！  @GetMapping("getCategory1")
    // @RequestMapping(value = "getCategory1",method = RequestMethod.POST //能够接收get ，post 都可以！ @PostMapping("getCategory1")
    //    @GetMapping("getCategory1")
    //    public List<BaseCategory1> getCategory1(){
    //        List<BaseCategory1> category1List = manageService.getCategory1();
    //        return category1List;
    //    }
    @GetMapping("getCategory1")
    public Result<List<BaseCategory1>> getCategory1(){
        List<BaseCategory1> category1List = manageService.getCategory1();
        // 返回的是数据，并且还有code 码，以及消息！
        return Result.ok(category1List);
    }

    @GetMapping("getCategory2/{category1Id}")
    public Result<List<BaseCategory2>> getCategory2(@PathVariable Long category1Id){
        List<BaseCategory2> category2List = manageService.getCategory2(category1Id);
        return Result.ok(category2List);
    }

    @GetMapping("getCategory3/{category2Id}")
    public Result<List<BaseCategory3>> getCategory3(@PathVariable Long category2Id){
        List<BaseCategory3> baseCategory3List = manageService.getCategory3(category2Id);
        return Result.ok(baseCategory3List);
    }

    // 通过分类Id 查询数据
    @GetMapping("attrInfoList/{category1Id}/{category2Id}/{category3Id}")
    public Result<List<BaseAttrInfo>> attrInfoList(@PathVariable Long category1Id,
                                                   @PathVariable Long category2Id,
                                                   @PathVariable Long category3Id){
        List<BaseAttrInfo> attrInfoList = manageService.getAttrInfoList(category1Id, category2Id, category3Id);

        return Result.ok(attrInfoList);
    }

    // 先获取到页面的数据  vue 项目在做保存的时候，通常前端数据会传递一个json 对象给后台
    // 需要将json 对象 转化为java对象 springMvc 的知识点 @RequestBody
    // @RequestBody BaseAttrInfo baseAttrInfo 表示前台数据自动封装到baseAttrInfo 对象中！
    @PostMapping("saveAttrInfo")
    public Result saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
        return Result.ok();
    }

    @GetMapping("getAttrValueList/{attrId}")
    public Result<List<BaseAttrValue>> getAttrValueList(@PathVariable Long attrId){
        // select * from baseAttr_Value where attr_id = ? 更加符合业务不应该直接查询
        // 先查询平台属性 ，从平台属性下面 对应获取到平台属性值？
        // attrId 是baseAttr_Value.attr_id 同时 baseAttrInfo.id
        // 平台属性：平台属性值  1：n
        // 综合业务分析：应该先查询平台属性，从平台属性中获取平台属性值
        BaseAttrInfo baseAttrInfo = manageService.getAttrInfo(attrId);
        // 获取到平台属性值集合
        List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();

        return Result.ok(attrValueList);

    }

    // 还需要获取到spuInfo 中的三级分类Id

    /**
     *
     * @param page 当前页码
     * @param size 每页显示的记录数
     * @param spuInfo 查询对象  不需要：@PathVariable
     * @return
     */
    /*
    springmvc 对象传值？
    页面传递数据的时候
    <form>
        <input name = "category3Id" value="1">
        <input type="submit" value="提交"/>
    </form>

    public void index(SpuInfo spuInfo){
        spuInfo
    }
     */
    @GetMapping("{page}/{size}")
    public Result<IPage<SpuInfo>> index(@ApiParam(name = "page",value = "当前页码",required = true) @PathVariable Long page,
                                        @ApiParam(name = "size",value = "每页显示的记录数",required = true) @PathVariable Long size,
                                        @ApiParam(name = "spuInfo",value = "查询对象",required = false) SpuInfo spuInfo
                                        ){
        // 封装分页查询参数
        Page<SpuInfo> pageParam = new Page<>(page,size);
        IPage<SpuInfo> spuInfoIPage = manageService.selectPage(pageParam,spuInfo);
        return Result.ok(spuInfoIPage);
    }

}
