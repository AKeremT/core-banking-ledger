package com.akt.banking_v1.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI coreBankingOpenAPI() {

        return new OpenAPI()
                .info(new Info()
                        .title("Core Banking Ledger API")
                        .description("High-Integrity Double-Entry Bookkeeping Ledger and " +
                                "Fund Transfer Engine with Concurrency & Idempotency Controls.")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Core Banking Engineering Team")
                                .email("engineering@corebank.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://springdoc.org")));
    }
}
