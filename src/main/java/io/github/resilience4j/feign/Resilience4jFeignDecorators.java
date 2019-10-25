package io.github.resilience4j.feign;
import feign.InvocationHandlerFactory.MethodHandler;
import feign.Target;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.retry.Retry;
import io.vavr.CheckedFunction1;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class Resilience4jFeignDecorators implements FeignDecorator {

    private final List<FeignDecorator> decorators;

    private Resilience4jFeignDecorators(List<FeignDecorator> decorators) {
        this.decorators = decorators;
    }

    @Override
    public CheckedFunction1<Object[], Object> decorate(CheckedFunction1<Object[], Object> fn,
                                                       Method method, MethodHandler methodHandler, Target<?> target) {
        CheckedFunction1<Object[], Object> decoratedFn = fn;
        for (final FeignDecorator decorator : decorators) {
            decoratedFn = decorator.decorate(decoratedFn, method, methodHandler, target);
        }
        return decoratedFn;
    }

    public static Resilience4jFeignDecorators.Builder builder() {
        return new Resilience4jFeignDecorators.Builder();
    }

    public static final class Builder {

        private final List<FeignDecorator> decorators = new ArrayList<>();

        /**
         * Adds a {@link Retry} to the decorator chain.
         *
         * @param retry a fully configured {@link Retry}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withRetry(Retry retry) {
            decorators.add((fn, m, mh, t) -> Retry.decorateCheckedFunction(retry, fn));
            return this;
        }

        /**
         * Adds a {@link Bulkhead} to the decorator chain.
         *
         * @param bulkhead a fully configured {@link Bulkhead}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withBulkhead(Bulkhead bulkhead) {
            decorators.add((fn, m, mh, t) -> Bulkhead.decorateCheckedFunction(bulkhead, fn));
            return this;
        }

        /**
         * Adds a {@link CircuitBreaker} to the decorator chain.
         *
         * @param circuitBreaker a fully configured {@link CircuitBreaker}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withCircuitBreaker(CircuitBreaker circuitBreaker) {
            decorators.add((fn, m, mh, t) -> CircuitBreaker.decorateCheckedFunction(circuitBreaker, fn));
            return this;
        }

        /**
         * Adds a {@link RateLimiter} to the decorator chain.
         *
         * @param rateLimiter a fully configured {@link RateLimiter}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withRateLimiter(RateLimiter rateLimiter) {
            decorators.add((fn, m, mh, t) -> RateLimiter.decorateCheckedFunction(rateLimiter, fn));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallback(Object fallback) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback)));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception thrown on error.
         * Multiple fallbacks can be applied with the next fallback being called when the previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallbackFactory(Function<Exception, ?> fallbackFactory) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory)));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @param filter only {@link Exception}s matching the specified {@link Exception} will
         *        trigger the fallback.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallback(Object fallback, Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback factory to the decorator chain. A factory can consume the exception thrown on error.
         * Multiple fallbacks can be applied with the next fallback being called when the previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @param filter only {@link Exception}s matching the specified {@link Exception} will
         *        trigger the fallback.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallbackFactory(Function<Exception, ?> fallbackFactory, Class<? extends Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. Multiple fallbacks can be applied with the next
         * fallback being called when the previous one fails.
         *
         * @param fallback must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @param filter the filter must return <code>true</code> for the fallback to be called.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallback(Object fallback, Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new DefaultFallbackHandler<>(fallback), filter));
            return this;
        }

        /**
         * Adds a fallback to the decorator chain. A factory can consume the exception thrown on error.
         * Multiple fallbacks can be applied with the next fallback being called when the previous one fails.
         *
         * @param fallbackFactory must match the feign interface, i.e. the interface specified when calling
         *        {@link Resilience4jBuilder#target(Class, String)}.
         * @param filter the filter must return <code>true</code> for the fallback to be called.
         * @return the builder
         */
        public Resilience4jFeignDecorators.Builder withFallbackFactory(Function<Exception, ?> fallbackFactory, Predicate<Exception> filter) {
            decorators.add(new FallbackDecorator<>(new FallbackFactory<>(fallbackFactory), filter));
            return this;
        }

        /**
         * Builds the decorator chain. This can then be used to setup an instance of
         * {@link Resilience4jFeign}.
         *
         * @return the decorators.
         */
        public Resilience4jFeignDecorators build() {
            return new Resilience4jFeignDecorators(decorators);
        }

    }

}
