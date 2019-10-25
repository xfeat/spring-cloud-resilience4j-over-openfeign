package org.springframework.cloud.openfeign;

import feign.Feign;
import io.github.resilience4j.feign.Resilience4jBuilder;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
@AutoConfigureBefore(FeignAutoConfiguration.class)
public class Resilience4jConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public Targeter feignTargeter() {
        return new Resilience4jTargeter();
    }

    @Bean
    @Scope("prototype")
    @ConditionalOnMissingBean
    public Feign.Builder feignBuilder() {
        return new Resilience4jBuilder();
    }
}
