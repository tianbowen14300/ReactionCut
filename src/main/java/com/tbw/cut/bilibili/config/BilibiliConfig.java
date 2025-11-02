package com.tbw.cut.bilibili.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = {"com.tbw.cut.bilibili", "com.tbw.cut.bilibili.service"})
public class BilibiliConfig {
    // Bilibili模块配置类
    // 通过@ComponentScan确保所有Bilibili相关的组件都被扫描到
}