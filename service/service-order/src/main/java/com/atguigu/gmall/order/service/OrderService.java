package com.atguigu.gmall.order.service;

import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * 继承IService
 * @date 2020/3/28 15:43
 */
public interface OrderService extends IService<OrderInfo> {

    // 保存订单 OrderInfo
    Long saveOrderInfo(OrderInfo orderInfo);

    /**
     * 生成流水号
     * @param userId
     * @return
     */
    String getTradeNo(String userId);

    /**
     * 比较流水号
     * @param tradeCodeNo 页面提交过来的流水号
     * @param userId 获取缓存的流水号的key
     * @return
     */
    boolean checkTradeCode(String tradeCodeNo,String userId);

    /**
     * 删除流水号
     * @param userId 获取缓存的流水号的key
     */
    void deleteTradeNo(String userId);

    /**
     * 调用验证库存方法
     * @param skuId
     * @param skuNum
     * @return
     */
    boolean checkStock(Long skuId, Integer skuNum);

    /**
     * 更新过期订单
     * @param orderId
     */
    void execExpiredOrder(Long orderId);
    /**
     * 更新过期订单
     * @param orderId
     * @param flag
     */
    void execExpiredOrder(Long orderId,String flag);

    /**
     * 处理过期订单
     */
    void updateOrderStatus(Long orderId, ProcessStatus processStatus);

    /**
     * 根据订单编号查询订单对象{查询订单明细为了后续需要订单明细}
     * @param orderId
     * @return
     */
    OrderInfo getOrderInfo(Long orderId);

    /**
     * 根据订单发送消息给库存，实现减库存的效果
     * @param orderId
     */
    void sendOrderStatus(Long orderId);

    /**
     * 将orderInfo 转换为map集合
     * @param orderInfo
     * @return
     */
    Map initWareOrder(OrderInfo orderInfo);

    /**
     * 拆单接口
     * @param orderId
     * @param wareSkuMap
     * @return
     */
    List<OrderInfo> orderSplit(long orderId, String wareSkuMap);
}
