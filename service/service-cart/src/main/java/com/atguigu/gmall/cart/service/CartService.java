package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

/**
 * @author mqx
 * @date 2020/3/27 9:46
 */
public interface CartService {

    // 添加购物车接口
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 查询购物车列表
     * @param userId 用户真正登录时的用户Id
     * @param userTempId 用户未登录的临时用户Id
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更新选中状态
     *
     * @param userId
     * @param isChecked
     * @param skuId
     */
    void checkCart(String userId, Integer isChecked, Long skuId);


    /**
     * 删除购物车
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId,String userId);
    /**
     * 根据用户Id 查询购物车列表
     *
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

    /**
     * 根据用户Id 查询最新商品数据
     * @param userId
     */
    List<CartInfo> loadCartCache(String userId);
}
