package com.mytool.zktool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.mybatis.spring.annotation.MapperScan;

@SpringBootApplication
@EnableWebSecurity
@MapperScan("com.mytool.zktool.mapper")
public class ZkToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(ZkToolApplication.class, args);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .authorizeHttpRequests()
            .anyRequest().permitAll();
        return http.build();
    }
}
