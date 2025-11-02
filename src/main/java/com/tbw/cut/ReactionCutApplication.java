package com.tbw.cut;

import com.tbw.cut.bilibili.config.BilibiliConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.tbw.cut.mapper")
@Import(BilibiliConfig.class)
public class ReactionCutApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReactionCutApplication.class, args);
    }

}