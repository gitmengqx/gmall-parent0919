package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mqx
 * @date 2020/3/24 16:02
 */
@Controller
public class ListController {

    @Autowired
    private ListFeignClient listFeignClient;
    // springMvc 对象传值！
    // http://list.gmall.com/list.html?category3Id=61
    // http://list.gmall.com/list.html?keyword=小米
    @GetMapping("list.html")
    public String search(SearchParam searchParam, Model model){
        Result<Map> result = listFeignClient.list(searchParam);
        // 后台只存储了检索之后的数据
        // 制作一个Url参数列表
        String urlParam = makeUrlParam(searchParam);

        // 处理品牌：
        String trademarkParam  = makeTrademark(searchParam.getTrademark());
        // 处理平台属性：
        List<Map<String, String>> propsParamList = makeProps(searchParam.getProps());

        // 处理排序：
        Map<String, Object> orderMap = dealOrder(searchParam.getOrder());
        // 保存数据给页面使用
        model.addAllAttributes(result.getData());
        model.addAttribute("urlParam",urlParam);
        model.addAttribute("searchParam",searchParam);

        // 进行保存数据：
        model.addAttribute("trademarkParam",trademarkParam);
        model.addAttribute("propsParamList",propsParamList);
        model.addAttribute("orderMap",orderMap);
        return "list/index";
    }

    /**
     *
     * @param searchParam 只要用户发起任何的查询请求，那么参数都会被封装到当前类中
     * @return
     */
    private String makeUrlParam(SearchParam searchParam) {
        // 做一个拼接字符串;
        StringBuffer urlParam = new StringBuffer();
        // 我们如何拼接字符串 {查询条件} http://list.gmall.com/list.html?keyword=小米手机
        if (searchParam.getKeyword()!=null){
            urlParam.append("keyword=").append(searchParam.getKeyword());
        }
        // 如果用户通过分类Id{1，2，3}查询
        // http://list.gmall.com/list.html?category1Id=2
        if (searchParam.getCategory1Id()!=null){
            urlParam.append("category1Id=").append(searchParam.getCategory1Id());
        }
        // http://list.gmall.com/list.html?category2Id=13
        if (searchParam.getCategory2Id()!=null){
            urlParam.append("category2Id=").append(searchParam.getCategory2Id());
        }
        // http://list.gmall.com/list.html?category3Id=61
        if (searchParam.getCategory3Id()!=null){
            urlParam.append("category3Id=").append(searchParam.getCategory3Id());
        }
        // 判断品牌 http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
        if (searchParam.getTrademark()!=null){
            if (urlParam.length()>0){
                urlParam.append("&trademark=").append(searchParam.getTrademark());
            }
        }
        // 判断平台属性值 http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=23:4G:运行内存
        if (searchParam.getProps()!=null){
            for (String prop : searchParam.getProps()) {
                if (urlParam.length()>0){
                    urlParam.append("&props=").append(prop);
                }
            }
        }
        // http://list.gmall.com/list.html?category3Id=61&trademark=2:华为&props=23:4G:运行内存
        return "list.html?"+urlParam.toString();
    }

    // 回显品牌数据格式：{品牌：值} trademark=2:华为
    // 如果手动传值 http://list.gmall.com/list.html?category3Id=61&trademark=2:华为
    private String makeTrademark(String trademark){
        if (!StringUtils.isEmpty(trademark)){
            String[] split = StringUtils.split(trademark, ":");
            // 为了防止传进来的split 是 空对象
            if (split !=null && split.length==2){
                return "品牌："+ split[1];
            }
        }
        return "";
    }

    /**
     * 处理平台属性 props=23:4G:运行内存 回显的数据{属性名称：属性值}
     * @param props 因为在页面可以点击多个平台属性值进行过滤
     * @return
     */
    private List<Map<String,String>> makeProps(String[] props){
        // 设置返回的集合
        List<Map<String,String>> list = new ArrayList<>();
        // 传进来的数据是否为空
        if (props!=null && props.length!=0){

            // 循环判断
            for (String prop : props) {
                // 对传进来的数据进行分割 23:4G:运行内存
                String[] split = StringUtils.split(prop, ":");
                if (split!=null && split.length==3){
                    // 创建一个map 来存储数据
                    Map<String,String> map = new HashMap<>();
                    // map 中的key 与实体类一致
                    map.put("attrId",split[0]);
                    map.put("attrValue",split[1]);
                    map.put("attrName",split[2]);
                    // map 添加到集合list
                    list.add(map);
                }

            }
        }
        return list;
    }

    // 处理排序 放入map 中 map.put(key,value) | class stu{ name , age}  stu.name
    // 接收前端页面传递过来的排序规则 order  1:hotScore 2:price
    private Map<String,Object> dealOrder(String order){
        // 声明map 集合对象
        Map<String,Object> map = new HashMap<>();
        // 排序规则 1:hotScore 2:price  | order=2:asc |
        if (!StringUtils.isEmpty(order)){
            // 对order 数据结构进行分割 :
//            String[] split = order.split(":");
            String[] split = StringUtils.split(order, ":");
            if (split!=null && split.length==2){
                // 存储排序规则
                map.put("type",split[0]);
                // 按照哪个字段排序的规则  asc | desc
                map.put("sort",split[1]);
            }
        }else {
            // 存储排序规则
            map.put("type","1");
            // 按照哪个字段排序
            map.put("sort","asc");
        }

        // 返回map
        return map;
    }
}
