package com.atguigu.gmall.product.service;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.model.product.*;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/3/13 14:06
 */
public interface ManageService {

    /**
     * 表示获取所有的一级分类数据
     * @return
     */
    List<BaseCategory1> getCategory1();

    /**
     * 根据一级分类Id 查询二级分类数据
     * @param category1Id
     * @return
     */
    List<BaseCategory2> getCategory2(Long category1Id);

    /**
     * 根据二级分类Id 查询三级分类数据
     * @param category2Id
     * @return
     */
    List<BaseCategory3> getCategory3(Long category2Id);

    /**
     * 根据分类Id 获取平台属性
     * @param category1Id
     * @param category2Id
     * @param category3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(Long category1Id, Long category2Id, Long category3Id);

    /**
     * 保存平台属性
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     * 通过attrId 获取平台属性对象
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrInfo(Long attrId);

    /**
     * 构建分页查询数据
     * @param pageParam 分页条件{pageSize ，pageNum，total等}
     * @param spuInfo 我们需要根据条件来查询数据，将查询出来的数据进行分页 {pageParam}
     * @return
     */
    IPage<SpuInfo> selectPage(Page<SpuInfo> pageParam, SpuInfo spuInfo);

    /**
     * 获取所有的销售属性列表
     * @return
     */
    List<BaseSaleAttr> getBaseSaleAttrList();

    /**
     * 大保存
     * @param spuInfo
     */
    void saveSpuInfo(SpuInfo spuInfo);

    /**
     * 根据spuId 查询商品图片列表
     * @param spuId
     * @return
     */
    List<SpuImage> getSpuImageList(Long spuId);

    /**
     * 通过spuId 查询销售属性数据
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrList(Long spuId);

    /**
     * 保存skuInfo 方法
     * @param skuInfo
     */
    void saveSkuInfo(SkuInfo skuInfo);
    // 当前页，没有显示的总条数等信息，Page类

    /**
     * 查询所有的skuInfo 数据
     * @param pageParam
     * @return
     */
    IPage<SkuInfo> selectPage(Page<SkuInfo> pageParam);

    /**
     * 商品上架
     * @param skuId
     */
    void onSale(Long skuId);

    /**
     * 商品下架
     * @param skuId
     */
    void cancelSale(Long skuId);

    /**
     * 根据商品Id 查询skuInfo 数据
     * @param skuId
     * @return
     */
    SkuInfo getSkuInfo(Long skuId);

    /**
     * 根据三级分类Id 查询实体数据
     * @param category3Id
     * @return
     */
    BaseCategoryView getCategoryViewByCategory3Id(Long category3Id);

    /**
     * 通过skuId 获取商品价格
     * @param skuId
     * @return
     */
    BigDecimal getSkuPrice(Long skuId);

    /**
     * 根据skuId ,spuId 查询销售属性，并锁定销售属性值
     * @param skuId
     * @param spuId
     * @return
     */
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(Long skuId,Long spuId);

    /**
     * 通过spuId 查询数据
     * @param spuId
     * @return
     */
    Map getSkuValueIdsMap(Long spuId);


    /**
     * 获取全部分类信息 制作首页
     * @return
     */
    List<JSONObject> getBaseCategoryList();



    /**
     * 通过品牌Id 来查询数据
     * @param tmId
     * @return
     */
    BaseTrademark getTrademarkByTmId(Long tmId);

    /**
     * 通过skuId 集合来查询数据
     * @param skuId
     * @return
     */
    List<BaseAttrInfo> getAttrList(Long skuId);
}
