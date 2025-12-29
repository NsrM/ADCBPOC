package org.example.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisCacheTester implements CommandLineRunner {

    @Autowired
    private RedisConnectionFactory connectionFactory;

    @Override
    public void run(String... args) {
        try (var conn = connectionFactory.getConnection()) {
            conn.ping();
            System.out.println("REDIS AUTH SUCCESS");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}

