package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserAddress;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author mqx
 * 提供用户收货地址列表
 * 初步设计到IService 是在我们做品牌管理的时候。
 * @date 2020/3/28 11:24
 */
public interface UserAddressService {
    /**
     * 根据用户Id 查询用户的收货地址列表！
     * select * from user_address where user_id = ?
     * @param userId
     * @return
     */
    List<UserAddress> findUserAddressListByUserId(String userId);
}
