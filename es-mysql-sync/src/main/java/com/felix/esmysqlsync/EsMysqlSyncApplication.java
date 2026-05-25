package com.felix.esmysqlsync;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.felix.esmysqlsync.mapper")
public class EsMysqlSyncApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsMysqlSyncApplication.class, args);
    }

}
