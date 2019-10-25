# spring-cloud-resilience4j-over-openfeign
## 简介
  目前官方支持的resilience4j和spring-cloud-openfeign   相结合使用只在resilience4j官方
  的[resilience4j-feign项目](https://github.com/resilience4j/resilience4j/tree/master/resilience4j-feign)有所提及,
  但相对而言使用编程式的方式来使用resilience4j并不是我们想要的,我们更需要想hystrix和spring-cloud-openfeign那样结合使用,可惜的是
  spring-cloud-openfeign官方并没有直接提供支持,所以这个项目的目的就是让你像hystrix和spring-cloud-openfeign   那样使用resilience4j.
  
  此外,你可能会问为什么该项目没有直接对spring-cloud-openfeign提PR,原因有两点
  1. springcloud正在孵化[spring-cloud-circuitbreaker](https://github.com/spring-cloud/spring-cloud-circuitbreaker#configuring-resilience4j-circuit-breakers)项目,已经包含了
  resilience4j的支持,只是当前还未发布正式版本
  2. openfeign已经提供了良好的扩展性,允许了我们自由的结合第三方依赖,并且和resilience4j的整合相对简单
  
## 使用  
1. 添加依赖
    ```xml
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
            <exclusions>
                <exclusion>
                    <artifactId>hystrix-core</artifactId>
                    <groupId>com.netflix.hystrix</groupId>
                </exclusion>
                <exclusion>
                    <artifactId>feign-hystrix</artifactId>
                    <groupId>io.github.openfeign</groupId>
                </exclusion>
            </exclusions>
        </dependency>
        <dependency>
            <groupId>cn.ocoop.framework</groupId>
            <artifactId>spring-cloud-resilience4j-over-openfeign</artifactId>
            <version>1.0</version>
        </dependency>
    ```  

2. 开启resilience4j开关
    ```java
       @EnableResilience4j
       @EnableFeignClients
       @SpringBootApplication
       public class App {
       
           public static void main(String[] args) {
               SpringApplication application = new SpringApplication(App.class);
               application.run(args);
           }
       }
    ```
3. 配置resilience4j,[参考地址](https://resilience4j.readme.io/docs/getting-started-3) 
    ```yml
        resilience4j.circuitbreaker:
            instances:
                backendA:
                    registerHealthIndicator: true
                    slidingWindowSize: 100
                backendB:
                    registerHealthIndicator: true
                    slidingWindowSize: 10
                    permittedNumberOfCallsInHalfOpenState: 3
                    slidingWindowType: TIME_BASED
                    minimumNumberOfCalls: 20
                    waitDurationInOpenState: 50s
                    failureRateThreshold: 50
                    eventConsumerBufferSize: 10
                    recordFailurePredicate: io.github.robwin.exception.RecordFailurePredicate
                    
        resilience4j.retry:
            instances:
                backendA:
                    maxRetryAttempts: 3
                    waitDuration: 10s
                    enableExponentialBackoff: true
                    exponentialBackoffMultiplier: 2
                    retryExceptions:
                        - org.springframework.web.client.HttpServerErrorException
                        - java.io.IOException
                    ignoreExceptions:
                        - io.github.robwin.exception.BusinessException
                backendB:
                    maxRetryAttempts: 3
                    waitDuration: 10s
                    retryExceptions:
                        - org.springframework.web.client.HttpServerErrorException
                        - java.io.IOException
                    ignoreExceptions:
                        - io.github.robwin.exception.BusinessException
                        
        resilience4j.bulkhead:
            instances:
                backendA:
                    maxConcurrentCall: 10
                backendB:
                    maxWaitDuration: 10ms
                    maxConcurrentCall: 20
                    
        ...
    ```   

至此,你已经可以像hystrix和spring-cloud-openfeign一起使用那样来使用resilience4j了!
启动你的项目,试试吧!
    
## 特殊情况
目前,该项目仅对resilience4j一下模块提供了支持:
+ Retry 
+ CircuitBreaker 
+ RateLimiter 
+ Bulkhead (仅限 Bulkhead,不支持ThreadPoolBulkhead)
并且,这些模块的组合顺序按照Retry > CircuitBreaker > RateLimiter > Bulkhead,并且是不支持改变的!


  
