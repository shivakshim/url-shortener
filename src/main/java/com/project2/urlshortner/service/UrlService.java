package com.project2.urlshortner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project2.urlshortner.dto.ShortenRequest;
import com.project2.urlshortner.dto.ShortenResponse;
import com.project2.urlshortner.dto.UrlCacheDto;
import com.project2.urlshortner.kafkaAnalytics.KafkaProducerService;
import com.project2.urlshortner.model.Url;
import com.project2.urlshortner.repository.UrlRepository;
import com.project2.urlshortner.util.Base62Encoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService {

    @Value("${app.base-url}")
    private String baseUrl;

    private final UrlRepository urlRepository;
    private static final Logger log = LoggerFactory.getLogger(UrlService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private ObjectMapper objectMapper;

    public UrlService(UrlRepository urlRepository){
        this.urlRepository = urlRepository;
    }

    public boolean isValidUrl(String url) {
        return url != null && url.startsWith("http");
    }

    public ShortenResponse shortenUrl(ShortenRequest requestdto) {

        String longUrl = requestdto.getLongUrl();
        String shortCode;

        //validate the longUrl for good/bad request
        if(!isValidUrl(longUrl)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid URL");
        }

        String alias = requestdto.getCustomAlias();

        // Step 1: Save URL to get ID
        Url url = new Url();
        url.setLongUrl(longUrl);
        url.setCreatedAt(LocalDateTime.now());
        // set expiry -> expires in 10 day
        url.setExpiryTime(LocalDateTime.now().plusDays(10));

        urlRepository.save(url);

        //Step 2 : Generate shortcode
        //if alias is given - set that as shortcode
        if(alias != null && !alias.isEmpty()) {
            //  check if already exists
            if (urlRepository.findByShortCode(alias).isPresent()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Alias already taken");
            }
            shortCode = alias;
        }
        else {
            //Generate short code  -- if alias is not given
            Long id = url.getId();
            shortCode = Base62Encoder.encode(id);
            url.setShortCode(shortCode);
        }

        // Step 3: Update entity
        urlRepository.save(url);
        // Store in Cache - on
        UrlCacheDto dto = new UrlCacheDto();
        dto.setLongUrl(url.getLongUrl());
        dto.setExpiryTime(url.getExpiryTime());

        String key = "url:" + shortCode;
        try {
            redisTemplate.opsForValue().set(key, dto, 10, TimeUnit.MINUTES);
        }catch(Exception e){
            log.error("Redis failed", e);
        }

        // Step 4: Return response
        // USE BASE URL (THIS IS THE FIX)
        String shortUrl = baseUrl + "/" + shortCode;

        return new ShortenResponse(shortCode, shortUrl);
    }

    public String getLongUrl(String shortCode){
        // 1. Check cache
        Object obj = redisTemplate.opsForValue().get("url:" + shortCode);

        UrlCacheDto cached = objectMapper.convertValue(obj, UrlCacheDto.class);

        if (cached != null) {
            log.info("Cache HIT {}", shortCode);
            UrlCacheDto dto = (UrlCacheDto) cached;

            //check if expired
            if(dto.getExpiryTime() != null
            && dto.getExpiryTime().isBefore(LocalDateTime.now())){
                // optional: delete stale cache
                redisTemplate.delete("url:" + shortCode);

                throw new ResponseStatusException(
                        HttpStatus.GONE, "Link expired"
                );
            }

            //put in topic for async analytics
            kafkaProducerService.sendClickEvent(shortCode);
            log.info("Kafka event sent for {}", shortCode);

            log.info("Redirect request for {}", shortCode);
            return dto.getLongUrl();
        }

        log.info("Cache Miss {}", shortCode);
        //2. DB Lookup
        Url url = urlRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "URL not found"
                ));

        //3. Check if link is expired
        if (url.getExpiryTime() != null &&
                url.getExpiryTime().isBefore(LocalDateTime.now())) {

            throw new ResponseStatusException(
                    HttpStatus.GONE, "Link expired");
        }

        String longUrl = url.getLongUrl();
        LocalDateTime expiryTime = url.getExpiryTime();

        UrlCacheDto dto = new UrlCacheDto();
        dto.setLongUrl(longUrl);
        dto.setExpiryTime(expiryTime);

        //4. Store in Cache -  on read
        String key = "url:" + shortCode;
        try {
            redisTemplate.opsForValue().set(key, dto, 10, TimeUnit.MINUTES);
        } catch(Exception e){
            log.error("Redis failed", e);
        }

        //push to kafka for async analytics and return longUrl
        kafkaProducerService.sendClickEvent(shortCode);
        log.info("Kafka event sent for {}", shortCode);
        log.info("Redirect request for {}", shortCode);
        return longUrl;
    }
}
