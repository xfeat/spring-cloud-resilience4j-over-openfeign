package cn.ocoop.framwork;

import org.springframework.cloud.openfeign.Resilience4jConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Convenience annotation for clients to enable Resilience4j
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(Resilience4jConfiguration.class)
public @interface EnableResilience4j {
}
