package com.project2.urlshortner.rateLimiter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.*;
import org.springframework.web.client.*;

@Component
public class RateLimiterClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${RATE_LIMITER_URL}")
    private String baseUrl;

    public boolean isAllowed(String clientId, String endpoint) {
        try {
            System.out.println("📡 RL BASE URL: " + baseUrl);

            String url = baseUrl + "/check/tokenbucketlua";

            System.out.println("📡 Calling RL at: " + url);
            System.out.println("➡️ clientId: " + clientId + ", endpoint: " + endpoint);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("clientId", clientId);
            body.add("endpoint", endpoint);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request =
                    new HttpEntity<>(body, headers);

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            System.out.println("✅ RL Response Status: " + response.getStatusCode());

            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException.TooManyRequests e) {
            System.out.println("❌ RL BLOCKED (429)");
            return false;

        } catch (Exception e) {
            System.out.println("⚠️ RL ERROR: " + e.getMessage());
            return true; // fail-safe
        }
    }
}

