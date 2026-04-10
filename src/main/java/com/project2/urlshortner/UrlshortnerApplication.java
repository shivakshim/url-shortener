package com.project2.urlshortner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableWebMvc
@EnableKafka
@SpringBootApplication
public class UrlshortnerApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlshortnerApplication.class, args);
	}

}
