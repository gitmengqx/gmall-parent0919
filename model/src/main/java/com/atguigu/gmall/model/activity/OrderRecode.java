package com.atguigu.gmall.model.activity;

import lombok.Data;

import java.io.Serializable;

@Data
public class OrderRecode implements Serializable {

	private static final long serialVersionUID = 1L;
	// 用户Id
	private String userId;
	// 秒杀商品
	private SeckillGoods seckillGoods;
	// 数量
	private Integer num;
	// 下单码
	private String orderStr;
}
