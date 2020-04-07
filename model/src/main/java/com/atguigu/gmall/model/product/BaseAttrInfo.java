package com.atguigu.gmall.model.product;

import com.atguigu.gmall.model.base.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * <p>
 * BaseAttrInfo
 * </p>
 *
 */
@Data
@ApiModel(description = "平台属性")
@TableName("base_attr_info")
public class BaseAttrInfo extends BaseEntity {
	
	private static final long serialVersionUID = 1L;
	
	@ApiModelProperty(value = "属性名称")
	@TableField("attr_name")
	private String attrName;

	@ApiModelProperty(value = "分类id")
	@TableField("category_id")
	private Long categoryId;

	@ApiModelProperty(value = "分类层级")
	@TableField("category_level")
	private Integer categoryLevel;

	//  平台属性值集合 表示非数据库字段
	//  当通过分类Id 查询数据的时候，将平台属性值一起查询出来 {1:n}
	@TableField(exist = false)
	private List<BaseAttrValue> attrValueList;

}

