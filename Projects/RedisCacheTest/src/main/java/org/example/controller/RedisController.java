package org.example.controller;

import org.example.model.UserProfile;
import org.example.service.RedisCacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/redis")
public class RedisController {

    @Autowired
    private RedisCacheService redisService;

    @PostMapping("/save")
    public String save() {
        UserProfile user = new UserProfile("101", "Naseer", "naseer@test.com");
        redisService.save("user:101", user);
        return "Saved!";
    }

    @GetMapping("/get")
    public Object get() {
        return redisService.get("user:101");
    }
}


