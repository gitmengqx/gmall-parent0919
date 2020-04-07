package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration // CorsConfig.xml
public class CorsConfig {
    // <bean class="org.springframework.web.cors.reactive.CorsWebFilter"> </bean>
    @Bean
    public CorsWebFilter corsWebFilter(){

        // cors跨域配置对象 jsonp 只支持get 请求
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); //设置允许访问的网络
        configuration.setAllowCredentials(true); // 设置是否从服务器获取cookie 单点登录{利用cookie存储数据}
        configuration.addAllowedMethod("*"); // 设置请求方法 * 表示任意
        configuration.addAllowedHeader("*"); // 所有请求头信息 * 表示任意 单点登录时需要将某些数据放入到header 中！


        // 配置源对象
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        // 表示任意
        configurationSource.registerCorsConfiguration("/**", configuration);
        // cors过滤器对象
        return new CorsWebFilter(configurationSource);
    }
}
