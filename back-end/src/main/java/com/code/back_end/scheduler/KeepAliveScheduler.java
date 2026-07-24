package com.code.back_end.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Keeps the Render free-tier service alive by self-pinging /api/health
 * every 14 minutes (Render spins down after 15 minutes of inactivity).
 */
@Component
public class KeepAliveScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${app.base-url:}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    // Runs every 14 minutes
    @Scheduled(fixedRateString = "840000")
    public void keepAlive() {
        if (baseUrl == null || baseUrl.isBlank()) {
            return; // skip in local dev (no base URL configured)
        }

        String url = baseUrl + "/api/health";
        try {
            restTemplate.getForObject(url, String.class);
            log.info("[KeepAlive] Pinged {} — service is awake.", url);
        } catch (Exception e) {
            log.warn("[KeepAlive] Ping failed: {}", e.getMessage());
        }
    }
}
