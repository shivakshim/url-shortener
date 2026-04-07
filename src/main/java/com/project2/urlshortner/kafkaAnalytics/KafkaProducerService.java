package com.project2.urlshortner.kafkaAnalytics;

import com.project2.urlshortner.dto.ClickEventDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class KafkaProducerService {

    @Autowired
    private KafkaTemplate<String, ClickEventDto> kafkaTemplate;

    //whenever there is a click event -> push analytics to redis
    public void sendClickEvent(String shortCode){
        ClickEventDto dto = new ClickEventDto();
        dto.setShortCode(shortCode);
        dto.setTimestamp(LocalDateTime.now());

        kafkaTemplate.send("click-events", dto);
    }
}
