package com.atguigu.gmall.order.controller;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.client.CartFeignClient;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.model.cart.CartInfo;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.product.client.ProductFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * 订单的数据接口
 * @date 2020/3/28 14:24
 */
@RestController
@RequestMapping("api/order")
public class OrderApiController {

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private CartFeignClient cartFeignClient;

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductFeignClient productFeignClient;


    // 点击去结算 按钮 http://order.gmall.com/trade.html 它应该是web-all中的控制器
    // 点击去结算，那么用户必须登录！ 这个登录规则，在网关限制好的！
    @GetMapping("auth/trade")
    public Result<Map<String,Object>> trade(HttpServletRequest request){
        // 必须要获取到用户Id ，根据用户Id 获取到收货地址列表
        String userId = AuthContextHolder.getUserId(request);
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);
        // 送货清单 本质：订单明细
        // 根据用户Id 查询购物车中被选中的商品
        List<CartInfo> cartCheckedList = cartFeignClient.getCartCheckedList(userId);
        // 声明一个集合来存储订单明细数据
        List<OrderDetail> detailArrayList = new ArrayList<>();
        for (CartInfo cartInfo : cartCheckedList) {
            // 创建一个订单明细对象
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setImgUrl(cartInfo.getImgUrl());
            // 能否明白? cartInfo.cartPrice 加入购物车时的价格
            // cartInfo.skuPrice 购物车中的实时价格
            orderDetail.setOrderPrice(cartInfo.getSkuPrice());
            orderDetail.setSkuId(cartInfo.getSkuId());
            orderDetail.setSkuName(cartInfo.getSkuName());
            orderDetail.setSkuNum(cartInfo.getSkuNum());
            // 将每一个订单明细添加到集合中
            detailArrayList.add(orderDetail);
        }
        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        // 需要给orderInfo 中的订单明细赋值
        orderInfo.setOrderDetailList(detailArrayList);
        // 计算总金额 并给当前orderInfo的totalAmount
        orderInfo.sumTotalAmount();

        // 用map 将数据集合存储起来
        Map<String,Object> map = new HashMap<>();
        // 订单收货地址列表
        map.put("userAddressList",userAddressList);
        // 订单明细集合
        map.put("detailArrayList",detailArrayList);
        // 总件数
        map.put("totalNum",detailArrayList.size());
        // 总金额
        map.put("totalAmount",orderInfo.getTotalAmount());

        // 获取流水号
        String tradeNo = orderService.getTradeNo(userId);
        // 保存流水号，前台会自动获取
        map.put("tradeNo",tradeNo);
        // 将map集合返回
        return Result.ok(map);
    }

    // 提交订单
    // 前台数据提交的是Json 数据，后台使用OrderInfo 对象接收
    // 将json 对象转化成OrderInfo 所以这个地方需要@RequestBody
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        orderInfo.setUserId(Long.parseLong(userId));
        // 判断用户是否回退无刷新提交订单
        // 获取页面提交过来的流水号
        String tradeNo = request.getParameter("tradeNo");
        // 比较流水号 调用比较流水号的方法
        boolean flag = orderService.checkTradeCode(tradeNo, userId);
        // 用户重复提交了。
        if (!flag){
            return  Result.fail().message("不能重复提交订单");
        }
        // 删除缓存的流水号
        orderService.deleteTradeNo(userId);
        // 验证库存：获取订单明细集合
        List<OrderDetail> orderDetailList = orderInfo.getOrderDetailList();
        for (OrderDetail orderDetail : orderDetailList) {
            // 返回true ，表示有库存，否则没有库存
            boolean result = orderService.checkStock(orderDetail.getSkuId(), orderDetail.getSkuNum());
            if (!result){
                return Result.fail().message(orderDetail.getSkuName()+"库存不足");
            }
            // 获取到商品的实时价格 查询一下skuInfo.price 与 orderDetail.getOrderPrice()
            BigDecimal skuPrice = productFeignClient.getSkuPrice(orderDetail.getSkuId());
            if (skuPrice.compareTo(orderDetail.getOrderPrice())!=0){
                // 说明价格有变动！ 更新一下商品的最新价格 {根据用户Id重新查询价格，添加到缓存}
                // 调用 loadCartCache 方法就可以了！
                cartFeignClient.loadCartCache(userId);
                return Result.fail().message(orderDetail.getSkuName()+"价格有变动");
            }
        }

        // 保存订单，并返回订单Id
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return Result.ok(orderId);
    }

    // 将根据订单编号查询订单对象
    @GetMapping("inner/getOrderInfo/{orderId}")
    public OrderInfo getOrderInfo (@PathVariable Long orderId){
        return orderService.getOrderInfo(orderId);
    }
    // http://localhost:8204/api/order/orderSplit?orderId=xxx&wareSkuMap=xxx
    @RequestMapping("orderSplit")
    public String orderSplit(HttpServletRequest request){
        // 获取对应的数据
        String orderId = request.getParameter("orderId");
        // 获取仓库Id与商品Id 的对照关系
        String wareSkuMap = request.getParameter("wareSkuMap");

        // 根据上面的需求进行拆单 ,得到的数据是子订单集合
        List<OrderInfo> subOrderInfoList =  orderService.orderSplit(Long.parseLong(orderId),wareSkuMap);
        // 声明一个集合来存储Map
        List<Map> mapList = new ArrayList<>();
        // 声明一个集合来存储数据 将买个OrderInfo ,先变成map 。然后将map 转换Json字符串。
        for (OrderInfo orderInfo : subOrderInfoList) {
            // 将每个子订单对象转换成一个map
            Map map = orderService.initWareOrder(orderInfo);
            // 添加到集合中
            mapList.add(map);
        }
        // 转换集合为字符串
        return JSON.toJSONString(mapList);

    }

    @PostMapping("inner/seckill/submitOrder")
    public Long submitOrder(@RequestBody OrderInfo orderInfo){
        // 保存订单方法
        Long orderId = orderService.saveOrderInfo(orderInfo);
        return orderId;
    }

}
