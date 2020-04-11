package com.atguigu.gmall.activity.controller;

import com.atguigu.gmall.activity.service.SeckillGoodsService;
import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.service.RabbitService;
import com.atguigu.gmall.common.util.AuthContextHolder;
import com.atguigu.gmall.common.util.CacheHelper;
import com.atguigu.gmall.common.util.DateUtil;
import com.atguigu.gmall.common.util.MD5;
import com.atguigu.gmall.model.activity.OrderRecode;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.order.client.OrderFeignClient;
import com.atguigu.gmall.user.client.UserFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author mqx
 * @date 2020/4/6 11:35
 */
@RestController
@RequestMapping("/api/activity/seckill")
public class SeckillGoodsController {

    @Autowired
    private SeckillGoodsService seckillGoodsService;

    @Autowired
    private RabbitService rabbitService;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private UserFeignClient userFeignClient;

    @Autowired
    private OrderFeignClient orderFeignClient;

    // 查询所有的秒杀商品
    @GetMapping("/findAll")
    public Result findAll(){
        List<SeckillGoods> list = seckillGoodsService.findAll();

        return Result.ok(list);
    }

    @GetMapping("/getSeckillGoods/{skuId}")
    public Result getSeckillGoods(@PathVariable Long skuId){
        return Result.ok(seckillGoodsService.getSecKillGoodsById(skuId));
    }
    // 获取下单码
    @GetMapping("auth/getSeckillSkuIdStr/{skuId}")
    public Result getSeckillSkuIdStr(@PathVariable Long skuId, HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 通过skuId 获取到当前要秒杀的商品
        SeckillGoods secKillGoods = seckillGoodsService.getSecKillGoodsById(skuId);
        // 当前的秒杀商品
        if (secKillGoods!=null){
            // 判断当前是否能够秒杀商品
            // 当前开始秒的时间，一定在 商品秒杀的时间范围内！秒杀开始时间， 秒杀结束时间 做一个比较
            Date curTime = new Date();
            if (DateUtil.dateCompare(secKillGoods.getStartTime(),curTime) && DateUtil.dateCompare(curTime,secKillGoods.getEndTime())){
                // 可以秒，先给一个秒杀资格！
                String skuIdStr = MD5.encrypt(userId);
                // 返回数据
                return Result.ok(skuIdStr);
            }
        }
        return Result.fail().message("获取下单码失败！");
    }

    // url: this.api_name + '/auth/seckillOrder/' + skuId + '?skuIdStr=' + skuIdStr,
    @PostMapping("/auth/seckillOrder/{skuId}")
    public Result seckillOrder(@PathVariable Long skuId,HttpServletRequest request){
        // 校验下单码 下单码是由用户Id 经过md5加密得到的！
        // 先获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 传递过来的下单码
        String skuIdStr = request.getParameter("skuIdStr");
        // 防止用户通过浏览器的地址直接修改下单码。
        if (!skuIdStr.equals(MD5.encrypt(userId))){
            // 请求不合法，你传递过来的下单码是错误的！
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 判断缓存标识位 我们在一开始做商品秒杀，将商品加入缓存的时候，我们做了一个状态赋值
        // 先获取状态位
        String state = (String) CacheHelper.get(skuId.toString());
        //        17:0 表示没有商品
        //        17:1 表示有商品
        if (StringUtils.isEmpty(state)){
            // 请求不合法，状态位是空
            return Result.build(null, ResultCodeEnum.SECKILL_ILLEGAL);
        }
        // 表示当前商品可以秒杀！
        if ("1".equals(state)){
            // 存储用户信息 只存储商品Id，用户Id
            UserRecode userRecode = new UserRecode();
            userRecode.setSkuId(skuId);
            userRecode.setUserId(userId);
            // 发送一个消息，将获取到秒杀资格的用户，存储起来 {发送消息的主要目的，保存用户的一个顺序}
            rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_SECKILL_USER,MqConst.ROUTING_SECKILL_USER,userRecode);
        }else {
            // 已经售罄
            return Result.build(null, ResultCodeEnum.SECKILL_FINISH);
        }
        return Result.ok();
    }

    @GetMapping(value = "auth/checkOrder/{skuId}")
    public Result checkOrder(@PathVariable Long skuId,HttpServletRequest request){
        // 获取到用户Id，当然还需要商品Id
        String userId = AuthContextHolder.getUserId(request);
        // 一定有商品Id，用户Id
        return seckillGoodsService.checkOrder(skuId,userId);
    }

    @GetMapping("auth/trade")
    public Result trade(HttpServletRequest request){
        // 获取用户Id
        String userId = AuthContextHolder.getUserId(request);
        // 获取用户购买的商品
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null==orderRecode){
            return Result.fail().message("操作失败!");
        }
        // 获取里面的数据
        SeckillGoods seckillGoods = orderRecode.getSeckillGoods();

        // 获取用户地址列表
        List<UserAddress> userAddressList = userFeignClient.findUserAddressListByUserId(userId);

        // 声明一个订单明细集合
        List<OrderDetail> orderDetailList = new ArrayList<>();
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setSkuId(seckillGoods.getSkuId());
        orderDetail.setSkuName(seckillGoods.getSkuName());
//        orderDetail.setSkuNum(seckillGoods.getNum());
        orderDetail.setSkuNum(orderRecode.getNum());
        orderDetail.setImgUrl(seckillGoods.getSkuDefaultImg());
        orderDetail.setOrderPrice(seckillGoods.getCostPrice());

        orderDetailList.add(orderDetail);

        // 计算总金额
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderDetailList(orderDetailList);
        orderInfo.sumTotalAmount();

        // 声明一个map集合
        HashMap<String, Object> map = new HashMap<>();
        map.put("detailArrayList",orderDetailList);
        map.put("userAddressList",userAddressList);
        map.put("totalAmount",orderInfo.getTotalAmount());

        return Result.ok(map);
    }

    // 前台传递的是json 字符串，所有后台用java 对象接收。
    // json -- > javaObject @RequestBody
    @PostMapping("auth/submitOrder")
    public Result submitOrder(@RequestBody OrderInfo orderInfo,HttpServletRequest request){
        // 获取用户id
        String userId = AuthContextHolder.getUserId(request);
        // 下单数据存储在缓存中 获取下订单数据。
        OrderRecode orderRecode = (OrderRecode) redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).get(userId);
        if (null == orderRecode){
            return Result.fail().message("下单失败!");
        }
        // 如果没有用户Id 将用户赋值！如果有了，不需要赋值了。
        orderRecode.setUserId(userId);
        // 提交订单方法
        Long orderId = orderFeignClient.submitOrder(orderInfo);

        if (null == orderId){
            return Result.fail().message("下单失败,联系客服.");
        }
        // 删除下订单记录{当前用户的}
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS).delete(userId);
        // 记录下当前的订单
        redisTemplate.boundHashOps(RedisConst.SECKILL_ORDERS_USERS).put(userId,orderId.toString());
        return Result.ok(orderId);
    }
}
