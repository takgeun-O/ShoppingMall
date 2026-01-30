package io.github.takgeun.shop;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class Jackson2TestConfig {

    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
