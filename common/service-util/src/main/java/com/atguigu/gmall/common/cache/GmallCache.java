package com.atguigu.gmall.common.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mqx
 * 说明该注解在什么位置上使用  在方法上面使用
 * 说明该注解的声明周期是多长  在java 源文件，class文件，jvm 都存在！
 * @date 2020/3/21 11:18
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GmallCache {

    // 自定义属性 set(key,value) 给一个前缀 默认值
    String prefix() default "cache";
}
