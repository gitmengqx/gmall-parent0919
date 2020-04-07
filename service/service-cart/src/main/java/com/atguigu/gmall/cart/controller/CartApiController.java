package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author mqx
 * CartApiController 为 web-all 中 addCart.html?skuId=20&skuNum=1 提供数据
 * @date 2020/3/27 11:43
 */
@RestController
@RequestMapping("api/cart")
public class CartApiController {

    @Autowired
    private CartService cartService;

    @PostMapping("addToCart/{skuId}/{skuNum}")
    public Result addToCart(@PathVariable Long skuId,
                            @PathVariable Integer skuNum,
                            HttpServletRequest request
                            ){
        // addToCart 需要userId,用户Id从网关中header 中获取
        // 如果它能获取到用户Id ，那么说明我们已经登录了。
        String userId = AuthContextHolder.getUserId(request);
        // 未登录的时候，也可添加购物车
        if (StringUtils.isEmpty(userId)){
            // 获取临时的用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        cartService.addToCart(skuId,userId,skuNum);
        // 返回
        return Result.ok();
    }

    // 查询购物车列表控制器
    @GetMapping("cartList")
    public Result cartList(HttpServletRequest request){
        // 获取用户登录时 的id ，
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户临时的Id
        String userTempId = AuthContextHolder.getUserTempId(request);
        // 查询购物车集合列表
        List<CartInfo> cartList = cartService.getCartList(userId,userTempId);

        // 将集合列表放入Result中
        return Result.ok(cartList);

    }

    @GetMapping("checkCart/{skuId}/{isChecked}")
    public Result checkCart(@PathVariable Long skuId,
                            @PathVariable Integer isChecked,
                            HttpServletRequest request
                            ){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            // 获取用户临时的Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // 调用修改状态方法
        cartService.checkCart(userId,isChecked,skuId);

        return Result.ok();

    }
    // 传递一个商品Id
    @DeleteMapping("deleteCart/{skuId}")
    public Result deleteCart(@PathVariable Long skuId,
                             HttpServletRequest request){
        // 获取到用户Id 包括登录时的用户Id，那么也包括未登录时的临时用户Id
        String userId = AuthContextHolder.getUserId(request);
        if (StringUtils.isEmpty(userId)){
            // 获取临时用户Id
            userId = AuthContextHolder.getUserTempId(request);
        }
        // userId 如果有登录的用户Id，那么它就是登录的用户Id ，如果没有userId 就是临时用户Id
        cartService.deleteCart(skuId,userId);

        return Result.ok();
    }

    // 暴露接口数据
    // 根据用户Id 获取到被选中的购物车商品
    @GetMapping("getCartCheckedList/{userId}")
    public List<CartInfo> getCartCheckedList(@PathVariable String userId){
        return cartService.getCartCheckedList(userId);
    }

    /**
     *
     * @param userId
     * @return
     */
    @GetMapping("loadCartCache/{userId}")
    public Result loadCartCache(@PathVariable("userId") String userId) {
        cartService.loadCartCache(userId);
        return Result.ok();
    }


}
