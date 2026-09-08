package com.campus.dorm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.campus.dorm.mapper")
public class DormApplication {
    public static void main(String[] args) {
        SpringApplication.run(DormApplication.class, args);
    }
}
