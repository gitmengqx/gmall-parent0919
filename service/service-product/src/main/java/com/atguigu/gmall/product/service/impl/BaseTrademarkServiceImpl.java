package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.product.mapper.BaseTrademarkMapper;
import com.atguigu.gmall.product.service.BaseTrademarkService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author mqx
 * 因为：BaseTrademarkService 集成了 IService 接口
 * IService 接口的实现类由ServiceImpl 已经帮我们实现好了！
 * @date 2020/3/14 14:46
 */
@Service
public class BaseTrademarkServiceImpl extends ServiceImpl<BaseTrademarkMapper, BaseTrademark> implements BaseTrademarkService {

    @Autowired
    private BaseTrademarkMapper baseTrademarkMapper;

    @Override
    public IPage<BaseTrademark> selectPage(Page<BaseTrademark> pageParam) {
        // select * from BaseTrademark 添加一个排序规则
        QueryWrapper<BaseTrademark> baseTrademarkQueryWrapper = new QueryWrapper<>();
        baseTrademarkQueryWrapper.orderByAsc("id");
        // 调用分页查询方法
        return baseTrademarkMapper.selectPage(pageParam,baseTrademarkQueryWrapper);
    }

    @Override
    public List<BaseTrademark> getBaseTrademarkList() {
        //select * from base_trademark
        return baseTrademarkMapper.selectList(null);
    }
}
