package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author mqx
 * 限流规则只能采用一种方式！
 * @date 2020/4/7 11:33
 */
@Configuration
public class KeyResolverConfig {

    // 通过userKey 作用用户限流
//    @Bean
//    KeyResolver userKeyResolver() {
//        return exchange -> Mono.just(exchange.getRequest().getQueryParams().getFirst("token"));
//    }

    // 通过Ip 限流
//    @Bean
//    KeyResolver ipKeyResolver() {
//        System.out.println("Ip限流");
//        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
//    }

    // 通过api 接口限流
    @Bean
    KeyResolver apiKeyResolver() {
        System.out.println("API限流");
        return exchange -> Mono.just(exchange.getRequest().getPath().value());
    }
}
