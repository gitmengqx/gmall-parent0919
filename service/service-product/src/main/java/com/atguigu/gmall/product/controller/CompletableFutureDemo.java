package com.atguigu.gmall.product.controller;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author mqx
 * @date 2020/3/21 14:39
 */
public class CompletableFutureDemo {

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // 可以接收返回值
        CompletableFuture.supplyAsync(() -> {
            return "hello";
        }).thenApplyAsync(t -> { // 得到上一个线程结果，返回自己的处理结果
            return t + " world!";
        }).thenCombineAsync(CompletableFuture.completedFuture(" Both CompletableFuture"), (t, u) -> {
            return t + u; // t hello world！ u=CompletableFuture
        }).whenComplete((t, u) -> {
            System.out.println(t);
        });

//        // 可以接收返回值
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName() + "\t completableFuture");
//                //int i = 10 / 0;
//                return 1024;
//            }
//        }).thenApply(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer o) {
//                System.out.println("thenApply方法，上次返回结果：" + o); // 1024
//                return  o * 2;
//            }
//        }).whenComplete(new BiConsumer<Integer, Throwable>() {
//            @Override
//            public void accept(Integer o, Throwable throwable) {
//                System.out.println("-------o=" + o); // 2048
//                System.out.println("-------throwable=" + throwable);
//            }
//        }).exceptionally(new Function<Throwable, Integer>() { // 不走！
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable=" + throwable);
//                return 6666;
//            }
//        }).handle(new BiFunction<Integer, Throwable, Integer>() {
//            @Override
//            public Integer apply(Integer integer, Throwable throwable) { // 正常处理
//                System.out.println("handle o=" + integer);
//                System.out.println("handle throwable=" + throwable);
//                return 8888;
//            }
//        });
//        System.out.println(future.get());
    }
//        // 可以接收返回值  runAsync方法不支持返回值
//        CompletableFuture future = CompletableFuture.supplyAsync(new Supplier<Object>() {
//            @Override
//            public Object get() {
//                System.out.println(Thread.currentThread().getName() + "\t completableFuture");
////                int i = 10 / 0; // 发生异常！
//                return 1024;
//            }
//        }).whenComplete(new BiConsumer<Object, Throwable>() { // 处理正常或异常
//            // o 表示获取上一个的返回结果值
//            @Override
//            public void accept(Object o, Throwable throwable) {
//                System.out.println("-------o=" + o.toString());
//                System.out.println("-------throwable=" + throwable);
//            }
//        }).exceptionally(new Function<Throwable, Object>() { // 处理异常
//            @Override
//            public Object apply(Throwable throwable) {
//                System.out.println("throwable=" + throwable);
//                return 6666;
//            }
//        });
//        // 获取返回结果集
//         System.out.println(future.get());
//    }

}
