package com.atguigu.gmall.product.service;

/**
 * @author mqx
 * @date 2020/3/20 10:19
 */
public interface TestService {
    // 测试本地锁接口
    void testLock();

    // 读锁
    String readLock();
    // 写锁
    String writeLock();
}
