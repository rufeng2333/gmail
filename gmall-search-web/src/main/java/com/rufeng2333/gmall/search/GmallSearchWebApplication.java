package com.rufeng2333.gmall.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude= DataSourceAutoConfiguration.class)
public class GmallSearchWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallSearchWebApplication.class, args);
    }

}
