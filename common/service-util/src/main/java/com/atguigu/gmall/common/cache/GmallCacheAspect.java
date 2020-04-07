package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * 将这个类放入spring 容器
 * 利用spring aop 特性 做一个环绕通知
 * @date 2020/3/21 11:27
 */
@Component
@Aspect
public class GmallCacheAspect {

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;
    // 通过 aop 实现自动添加缓存的效果！{工具 任何一个方法都可以实现 【不能保证每个方法的返回值都一样】}
    // 返回数据类型 ：不一定是SkuInfo spring.io;
    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point){
        // 声明一个对象Object
        Object result = null;
        // demo: item.gmall.com/18.html  自定义注解的前缀：sku:  skuId:18  sku:[18] sku:[17]
        // 获取到传递的参数
        Object[] args = point.getArgs();
        // 想得到哪些方法上有注解
        // 获取方法上的签名
        MethodSignature signature = (MethodSignature) point.getSignature();
        // 得到注解
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        // 查询缓存数据 看缓存是否有数据，有则返回，没有则去数据库中获取数据
        // 获取前缀 sku:
        String prefix = gmallCache.prefix();
        // 定义缓存key sku:[skuId] sku:[18]
        String key = prefix + Arrays.asList(args).toString();
        // 正常先查询缓存 {从缓存获取数据：第一必须传递key，第二必须知道缓存中存储的数据类型}
        result = cacheHit(signature,key);
        // 表示缓存不为空，则直接返回数据
        if (result!=null){
            return result;
        }
        // 缓存要是空，则从数据库中获取数据{避免缓存击穿，穿透}
        RLock lock = redissonClient.getLock(key + ":lock");
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            // 判断是否能够获取锁
            if (res){
                // 获取业务数据：得到带注解的方体执行结果
                // 表示调用了getSkuInfoDB(skuId)
                result = point.proceed(point.getArgs());
                // 判断执行结果:说明数据库中根本没有这个数据 防止缓存穿透
                if (result==null){
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key,JSONObject.toJSONString(o), RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                // 数据库中有数据直接放入缓存
                redisTemplate.opsForValue().set(key,JSONObject.toJSONString(result), RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            }else {
                // 没获取到的锁的线程
                Thread.sleep(1000);
                // 获取缓存数据
                return cacheHit(signature,key);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }finally {
            lock.unlock();
        }
        return result;
    }

    /**
     *
     * @param signature 可以获取到方法的返回值
     * @param key 需要获取数据使用
     * @return
     */
    private Object cacheHit(MethodSignature signature, String key) {
        // 获取数据 redis 的 String数据类型 ： key，value 都是字符串
        String cache = (String) redisTemplate.opsForValue().get(key);
        // 从缓存中获取的字符串是否为空
        if (StringUtils.isNotBlank(cache)){
            // 有数据 ，则将数据进行转化
            // 方法返回的数据类型
            Class returnType = signature.getReturnType();
            // 直接转化数据：
            return JSONObject.parseObject(cache,returnType);

        }
        return null;
    }
}
