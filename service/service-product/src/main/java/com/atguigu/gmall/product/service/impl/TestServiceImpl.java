package com.atguigu.gmall.product.service.impl;

import com.atguigu.gmall.product.service.TestService;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @author mqx
 * 测试：在缓存中存储一个num的数据，模拟并发访问这个接口方法。
 * @date 2020/3/20 10:20
 */
@Service
public class TestServiceImpl implements TestService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    /**
     * 方法功能：数据累加
     */
    @Override
    public synchronized void testLock() {
        // 自定义锁
        RLock lock = redissonClient.getLock("lock");

        // 加锁： 设置过期时间
        lock.lock(10,TimeUnit.SECONDS);
        // 业务处理代码
        // 查询缓存中num的key数据
        // Jedis String set(key,value) get(key)
        String value = redisTemplate.opsForValue().get("num");
        // 判断value 是否有数据
        if (StringUtils.isBlank(value)){
            // 如果value 是空
            return;
        }
        // 将value 转化为Integer 数据类型
        int num = Integer.parseInt(value);
        // 将num 放入缓存，并自增
        redisTemplate.opsForValue().set("num",String.valueOf(++num));

        // 解锁：
        // lock.unlock();

    }

    @Override
    public String readLock() {
        // 什么锁对象
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("readwriteLock");
        // 获取锁
        RLock rLock = rwlock.readLock();
        // 加锁：10秒自动解锁
        rLock.lock(10,TimeUnit.SECONDS);

        // 读取数据
        String msg = redisTemplate.opsForValue().get("msg");

        // 返回数据
        return msg;

    }

    @Override
    public String writeLock() {
        // 保证读锁，写锁他们的key 一致！
        RReadWriteLock rwlock = redissonClient.getReadWriteLock("readwriteLock");
        // 写锁
        RLock rLock = rwlock.writeLock();
        // 获取锁
        rLock.lock(10,TimeUnit.SECONDS);
        // 写入数据
        redisTemplate.opsForValue().set("msg",UUID.randomUUID().toString());


        return "写入完成。。。。。。";
    }

    // trylock
    private void rTrylock(RLock lock) {
        try {
            boolean res = lock.tryLock(100, 10, TimeUnit.SECONDS);
            if (res){
                try {
                    // 业务处理代码
                    // 查询缓存中num的key数据
                    // Jedis String set(key,value) get(key)
                    String value = redisTemplate.opsForValue().get("num");
                    // 判断value 是否有数据
                    if (StringUtils.isBlank(value)){
                        // 如果value 是空
                        return;
                    }
                    // 将value 转化为Integer 数据类型
                    int num = Integer.parseInt(value);
                    // 将num 放入缓存，并自增
                    redisTemplate.opsForValue().set("num",String.valueOf(++num));
                }catch (Exception e){
                    e.printStackTrace();
                }finally {
                    lock.unlock();
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void testRedis() {
        // 声明一个UUID
        String uuid = UUID.randomUUID().toString();
        // 加锁：setnx ，del
        // 在执行set的时候，直接给过期时间，这样就保证了命令的原子性
        Boolean lock = redisTemplate.opsForValue().setIfAbsent("lock", uuid,2, TimeUnit.SECONDS);
        // 没有锁加锁：处理数据
        if (lock){
            // 查询缓存中num的key数据
            // Jedis String set(key,value) get(key)
            String value = redisTemplate.opsForValue().get("num");
            // 判断value 是否有数据
            if (StringUtils.isBlank(value)){
                // 如果value 是空
                return;
            }
            // 将value 转化为Integer 数据类型
            int num = Integer.parseInt(value);
            // 将num 放入缓存，并自增
            redisTemplate.opsForValue().set("num",String.valueOf(++num));

            // 声明script--lua 脚本
            /*
            if redis.call("get",KEYS[1]) == ARGV[1]
            then
                return redis.call("del",KEYS[1])
            else
                return 0
            end
             */
            String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
            // 设置lua脚本返回的数据类型
            DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
            // 设置lua脚本返回类型为Long
            redisScript.setResultType(Long.class);
            redisScript.setScriptText(script);
            redisTemplate.execute(redisScript, Arrays.asList("lock"),uuid);

            // 打开锁
//            if (uuid.equals(redisTemplate.opsForValue().get("lock"))){
//                // 是谁的锁，谁删！
//                redisTemplate.delete("lock");
//            }

        }else{
            // 其他线程需要登录 最好给1秒测试
            try {
                Thread.sleep(1000);
                // 睡醒了之后
                testLock();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
