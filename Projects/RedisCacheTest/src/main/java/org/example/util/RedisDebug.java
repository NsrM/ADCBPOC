package org.example.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RedisDebug implements CommandLineRunner {

//    @Value("${spring.redis.host}")
//    private String host;
//
//    @Value("${spring.redis.port}")
//    private String port;
//
////    @Value("${spring.redis.username}")
////    private String username;
//
//    @Value("${spring.redis.password}")
//    private String password;
//
    @Override
    public void run(String... args) throws Exception {
//        System.out.println("REDIS HOST     = " + host);
//        System.out.println("REDIS PORT     = " + port);
//        //System.out.println("REDIS USERNAME = " + username);
//        System.out.println("REDIS PASSWORD = " + password);
    }
}

