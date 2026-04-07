package com.project2.urlshortner.kafkaAnalytics;

import com.project2.urlshortner.dto.ClickEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
public class KafkaListenerService {
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @KafkaListener(topics = "click-events", groupId = "click-group")
    public void consume(ClickEventDto dto) {
        String shortCode = dto.getShortCode();
        LocalDateTime time = dto.getTimestamp();
        //ANALYTICS
            //A1. Total clicks for this shortcode (incr)
            redisTemplate.opsForValue().increment("click:" + shortCode);

            //A2. Clicks per day for this shortcode (incr)
            String dateKey = "click:" + shortCode + ":" + LocalDate.now();
            redisTemplate.opsForValue().increment(dateKey);

            redisTemplate.expire(dateKey, 7, TimeUnit.DAYS); //remove old dates data

            //A3. Set lastAccess - now
            redisTemplate.opsForValue()
                    .set("lastAccess:" + shortCode, time);

    }
}
