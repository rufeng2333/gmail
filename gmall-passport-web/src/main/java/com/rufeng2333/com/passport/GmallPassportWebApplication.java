package com.rufeng2333.com.passport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude= DataSourceAutoConfiguration.class)
public class GmallPassportWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GmallPassportWebApplication.class, args);
    }

}
