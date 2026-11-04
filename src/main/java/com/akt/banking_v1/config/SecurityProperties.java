package com.akt.banking_v1.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class SecurityProperties {

    private String secretKey;
    private Duration secretKeyExpiration;
    private String issuer;
    private String audience;
}
