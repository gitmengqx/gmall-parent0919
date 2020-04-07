package com.atguigu.gmall.model.order;

import com.atguigu.gmall.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@ApiModel(description = "订单明细")
@TableName("order_detail")
public class OrderDetail extends BaseEntity {
    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "订单编号")
    @TableField("order_id")
    private Long orderId;

    @ApiModelProperty(value = "sku_id")
    @TableField("sku_id")
    private Long skuId;

    @ApiModelProperty(value = "sku名称（冗余)")
    @TableField("sku_name")
    private String skuName;

    @ApiModelProperty(value = "图片名称（冗余)")
    @TableField("img_url")
    private String imgUrl;

    @ApiModelProperty(value = "购买价格(下单时sku价格）")
    @TableField("order_price")
    private BigDecimal orderPrice;

    @ApiModelProperty(value = "购买个数")
    @TableField("sku_num")
    private Integer skuNum;

    // 验证库存！是否有足够的库存！hasStock=1 表示有库存，hasStock=0 当前购买的这个商品没有足够的库存！请求重新下单。
    @TableField(exist = false)
    private String hasStock;


}
