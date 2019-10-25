/*
 * Copyright 2013-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.openfeign;

import feign.Feign;
import feign.Target;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.feign.Resilience4jBuilder;
import io.github.resilience4j.feign.Resilience4jFeignDecorators;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

/**
 * @author liolay
 */
@SuppressWarnings("unchecked")
public class Resilience4jTargeter implements Targeter {
    private static final Logger logger = LoggerFactory.getLogger(Resilience4jTargeter.class);

    @Override
    public <T> T target(FeignClientFactoryBean factory, Feign.Builder feign,
                        FeignContext context, Target.HardCodedTarget<T> target) {
        if (!(feign instanceof Resilience4jBuilder)) {
            return feign.target(target);
        }

        Resilience4jFeignDecorators.Builder decoratorsBuilder = Resilience4jFeignDecorators.builder();

        withRetry(factory, context, decoratorsBuilder);

        withCircuitBreaker(factory, context, decoratorsBuilder);

        withRateLimiter(factory, context, decoratorsBuilder);

        withBulkhead(factory, context, decoratorsBuilder);

        withFallback(factory, context, target, decoratorsBuilder);

        return ((Resilience4jBuilder) feign).target(target, decoratorsBuilder.build());
    }

    private <T> void withFallback(FeignClientFactoryBean factory, FeignContext context, Target.HardCodedTarget<T> target, Resilience4jFeignDecorators.Builder decoratorsBuilder) {
        Class<?> fallbackClass = factory.getFallback();
        if (fallbackClass != void.class) {
            T fallbackInstance = getFromContext("fallback", factory.getName(), context,
                    fallbackClass, target.type());
            decoratorsBuilder.withFallback(fallbackInstance);
        } else {
            Class<?> fallbackFactoryClass = factory.getFallbackFactory();
            if (fallbackFactoryClass != void.class) {
                Function<Exception, ?> fallbackFactory = (Function<Exception, ?>) getFromContext(
                        "fallbackFactory", factory.getName(), context, fallbackFactoryClass,
                        Function.class);
                decoratorsBuilder.withFallbackFactory(fallbackFactory);
            }
        }
    }

    private void withBulkhead(FeignClientFactoryBean factory, FeignContext context, Resilience4jFeignDecorators.Builder decoratorsBuilder) {
        BulkheadRegistry bulkheadRegistry = getOptional(factory.getName(), context, BulkheadRegistry.class);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(factory.getName());
        logger.debug("Created or retrieved bulkhead '{}' with maxConcurrentCalls: '{}', maxWaitDuration: '{}'s",
                factory.getName(), bulkhead.getBulkheadConfig().getMaxConcurrentCalls(),
                bulkhead.getBulkheadConfig().getMaxWaitDuration().getSeconds()
        );
        decoratorsBuilder.withBulkhead(bulkhead);
    }

    private void withRateLimiter(FeignClientFactoryBean factory, FeignContext context, Resilience4jFeignDecorators.Builder decoratorsBuilder) {
        RateLimiterRegistry rateLimiterRegistry = getOptional(factory.getName(), context, RateLimiterRegistry.class);
        RateLimiter rateLimiter = rateLimiterRegistry.rateLimiter(factory.getName());
        RateLimiterConfig rateLimiterConfig = rateLimiter.getRateLimiterConfig();
        logger.debug(
                "Created or retrieved rate limiter '{}' with limitRefreshPeriod: '{}'s, limitForPeriod: '{}', timeoutDuration: '{}'s",
                factory.getName(), rateLimiterConfig.getLimitRefreshPeriod().getSeconds(), rateLimiterConfig.getLimitForPeriod(),
                rateLimiterConfig.getTimeoutDuration().getSeconds()
        );
        decoratorsBuilder.withRateLimiter(rateLimiter);
    }

    private void withCircuitBreaker(FeignClientFactoryBean factory, FeignContext context, Resilience4jFeignDecorators.Builder decoratorsBuilder) {
        CircuitBreakerRegistry circuitBreakerRegistry = getOptional(factory.getName(), context, CircuitBreakerRegistry.class);
        CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(factory.getName());
        logger.debug("Created or retrieved circuit breaker '{}' with failureRateThreshold: '{}', waitDurationInOpenState: '{}'s",
                factory.getName(), circuitBreaker.getCircuitBreakerConfig().getFailureRateThreshold(),
                circuitBreaker.getCircuitBreakerConfig().getWaitDurationInOpenState().getSeconds()
        );
        decoratorsBuilder.withCircuitBreaker(circuitBreaker);
    }

    private void withRetry(FeignClientFactoryBean factory, FeignContext context, Resilience4jFeignDecorators.Builder decoratorsBuilder) {
        RetryRegistry retryRegistry = getOptional(factory.getName(), context, RetryRegistry.class);
        Retry retry = retryRegistry.retry(factory.getName());
        logger.debug("Created or retrieved withRetry '{}' with maxAttempts '{}'",
                factory.getName(), retry.getRetryConfig().getMaxAttempts());
        decoratorsBuilder.withRetry(retry);
    }

    private <T> T getOptional(String feignClientName, FeignContext context,
                              Class<T> beanType) {
        return context.getInstance(feignClientName, beanType);
    }

    private <T> T getFromContext(String fallbackMechanism, String feignClientName,
                                 FeignContext context, Class<?> beanType, Class<T> targetType) {
        Object fallbackInstance = context.getInstance(feignClientName, beanType);
        if (fallbackInstance == null) {
            throw new IllegalStateException(String.format(
                    "No " + fallbackMechanism
                            + " instance of type %s found for feign client %s",
                    beanType, feignClientName));
        }

        if (!targetType.isAssignableFrom(beanType)) {
            throw new IllegalStateException(String.format("Incompatible "
                            + fallbackMechanism
                            + " instance. Fallback/fallbackFactory of type %s is not assignable to %s for feign client %s",
                    beanType, targetType, feignClientName));
        }
        return (T) fallbackInstance;
    }
}
