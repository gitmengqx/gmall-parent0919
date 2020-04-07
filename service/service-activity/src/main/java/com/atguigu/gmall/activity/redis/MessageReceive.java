package com.atguigu.gmall.activity.redis;

import com.atguigu.gmall.common.util.CacheHelper;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

@Component
public class MessageReceive {

    /**接收消息的方法*/
    public void receiveMessage(String message){
        System.out.println("----------收到消息了message："+message);
        if(!StringUtils.isEmpty(message)) {
            /*
             消息格式
                skuId:0 表示没有商品
                skuId:1 表示有商品
                17:0 表示没有商品
                17:1 表示有商品
             */
            message = message.replaceAll("\"","");
            String[] split = StringUtils.split(message, ":");
//            String[] split = message.split(":");

            if (split == null || split.length == 2) {
                // 商品的状态位！ 17
                // CacheHelper 本质就是一个 map 集合, 在tomcat 运行的时候生效，如果tomcat 停止了，再次启动CacheHelper 失效了。
                CacheHelper.put(split[0], split[1]);
            }
        }
    }

}
