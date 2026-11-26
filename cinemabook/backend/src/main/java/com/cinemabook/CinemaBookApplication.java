package com.cinemabook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // needed for auto-delete posts job (Phase 4)
public class CinemaBookApplication {
    public static void main(String[] args) {
        SpringApplication.run(CinemaBookApplication.class, args);
    }
}
