package com.juntospelobem.pets.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@EnableScheduling 
public class CacheCleaner {

    private static final Logger log = LoggerFactory.getLogger(CacheCleaner.class);
    private final CacheManager cacheManager;

    public CacheCleaner(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

 @Scheduled(fixedRate = 3600000)
    public void limparCacheOtp() {
        Objects.requireNonNull(cacheManager.getCache("otpCache")).clear();
        log.info("Faxina no Cache: Códigos OTP antigos e abandonados foram purgados da memória.");
    }

    @Scheduled(fixedRate = 3600000)
    public void limparCacheCards() {
        Objects.requireNonNull(cacheManager.getCache("cardsCache")).clear();
        log.info("🧹 Faxina no Cache: Cards apagados para forçar a API a buscar dados frescos do Bitrix!");
    }
}