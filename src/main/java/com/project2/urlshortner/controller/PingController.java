package com.project2.urlshortner.controller;

import com.project2.urlshortner.dto.ShortenRequest;
import com.project2.urlshortner.dto.ShortenResponse;
import com.project2.urlshortner.service.UrlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
public class PingController {

    @Autowired
    private UrlService urlService;

    @GetMapping("/ping")
    public String ping(){
        return "Hello shiv";
    }

    @PostMapping("/urls")
    public ShortenResponse getShortUrl(@RequestBody ShortenRequest requestdto){
        return urlService.shortenUrl(requestdto);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String longUrl = urlService.getLongUrl(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)   // 302
                .location(URI.create(longUrl))
                .build();
    }
}
