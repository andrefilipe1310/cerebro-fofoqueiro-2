package com.fofoqueiro.notification.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class ThrottleService {

    private final StringRedisTemplate redisTemplate;

    public boolean isThrottled(String orgId, String emailType, String resourceId) {
        String key = "throttle:" + emailType + ":" + orgId + ":" + resourceId;
        Boolean wasAbsent = redisTemplate.opsForValue().setIfAbsent(key, "1", Duration.ofHours(1));
        return wasAbsent != null && !wasAbsent;
    }
}
