package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author mqx
 * IService 需要调用mybatis-plus 内部的增删改查方法
 * @date 2020/3/14 14:42
 */
public interface BaseTrademarkService extends IService<BaseTrademark> {

    // 做分页查询 上来就查询所有，所以不需要实体类接收任何查询条件！
    IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam);

    // 查询所有数据
    List<BaseTrademark> getBaseTrademarkList();
}
