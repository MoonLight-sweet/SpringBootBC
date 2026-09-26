package com.codeclinic;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@MapperScan("com.codeclinic.**.mapper")
public class CodeClinicApplication {
    public static void main(String[] args) {
        SpringApplication.run(CodeClinicApplication.class, args);
    }
}
