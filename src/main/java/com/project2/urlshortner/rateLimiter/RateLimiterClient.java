package com.project2.urlshortner.rateLimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.client.*;

    @Component
    public class RateLimiterClient {

        private final RestTemplate restTemplate = new RestTemplate();

        @Value("${rate.limiter.url}")
        private String baseUrl;

        public boolean isAllowed(String clientId, String endpoint) {
            try {
                String url = baseUrl + "/check/tokenbucketlua";

                MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
                body.add("clientId", clientId);
                body.add("endpoint", endpoint);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                HttpEntity<MultiValueMap<String, String>> request =
                        new HttpEntity<>(body, headers);

                ResponseEntity<String> response =
                        restTemplate.postForEntity(url, request, String.class);

                return response.getStatusCode().is2xxSuccessful();

            } catch (HttpClientErrorException.TooManyRequests e) {
                return false;
            } catch (Exception e) {
                return true; // fail-safe
            }
        }
    }

