package org.example.lobbycenter.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;

public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient(){
        Config config = new Config();
        config.useSingleServer().setAddress("redis://21.91.242.77:6379").setPassword("123456");
        // 创建RedissonClient对象
        return Redisson.create(config);
    }
}
