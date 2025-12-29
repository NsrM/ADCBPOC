package org.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheConnectionTest implements CommandLineRunner {

    @Autowired
    RedisConnectionFactory factory;

    @Override
    public void run(String... args) throws Exception {
        try {
            System.out.println("Testing Redis connection...");
            var connection = factory.getConnection();
            System.out.println("PING => " + connection.ping());
        } catch (Exception ex) {
            System.out.println("Redis ERROR:");
            ex.printStackTrace();
        }
    }
}

