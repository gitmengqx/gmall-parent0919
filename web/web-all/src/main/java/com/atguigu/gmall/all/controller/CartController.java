package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.model.product.SkuInfo;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;

/**
 * @author mqx
 * @date 2020/3/28 9:42
 */
@Controller
public class CartController {

    // 调用service-cart-client 模块 它才能调用添加购物车数据接口
    @Autowired
    private CartFeignClient cartFeignClient;

    // 需要引入service-product-client
    @Autowired
    private ProductFeignClient productFeignClient;

    // 制作一个addCart.html的控制器
    // http://cart.gmall.com/addCart.html?skuId=17&skuNum=1
    @RequestMapping("addCart.html")
    public String addCart(@RequestParam(name = "skuId") Long skuId,
                          @RequestParam(name = "skuNum") Integer skuNum,
                          HttpServletRequest request
                          ){
        // CartApiController控制器中的方法传入的参数 HttpServletRequest request 获取用户Id的。
        // 在这个控制器中我们如何才能得到用户Id？
        // 这个对象 cartFeignClient 能否获取到用户Id 呢？ 不能！微服务模块通过feign 远程调用 并不能传递头文件信息。
        // 需要自定义一个拦截器获取文件信息。放入header 中
        cartFeignClient.addToCart(skuId,skuNum);

        // 需要保存信息：skuNum skuInfo
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        request.setAttribute("skuNum",skuNum);
        return "cart/addCart";
    }

    @RequestMapping("cart.html")
    public String cartIndex(){
        return "cart/index";
    }

}
