package com.inabottle.apigateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("direct-message-service", r -> r.path("/direct")
                        .filters(f -> f.hystrix(c -> c.setName("directFallback")
                                .setFallbackUri("forward:/empty-fallback")))
                        .uri("lb://direct-message-service"))
                .route("direct-message-service-id", r -> r.path("/direct/**")
                        .filters(f -> f.hystrix(c -> c.setName("directFallback").setFallbackUri("forward:/empty-fallback"))
                                .rewritePath("direct-message-service/(?<segment>.*)", "direct-message-service/${segment}"))
                        .uri("lb://direct-message-service/"))

                .route("hub-service", r -> r.path("/hub")
                        .filters(f -> f.hystrix(c -> c.setName("hubFallback")
                                .setFallbackUri("forward:/empty-fallback")))
                        .uri("lb://hub-service"))
                .route("hub-service-id", r -> r.path("/hub/**")
                        .filters(f -> f.hystrix(c -> c.setName("hubFallback").setFallbackUri("forward:/empty-fallback"))
                                .rewritePath("hub-service/(?<segment>.*)", "hub-service/${segment}"))
                        .uri("lb://hub-service/"))

                .route("treasure-hunt-service", r -> r.path("/treasure")
                        .filters(f -> f.hystrix(c -> c.setName("treasureFallback")
                                .setFallbackUri("forward:/empty-fallback")))
                        .uri("lb://treasure-hunt-service"))
                .route("treasure-hunt-service-id", r -> r.path("/treasure/**")
                        .filters(f -> f.hystrix(c -> c.setName("treasureFallback").setFallbackUri("forward:/empty-fallback"))
                                .rewritePath("treasure-hunt-service/(?<segment>.*)", "treasure-hunt-service/${segment}"))
                        .uri("lb://treasure-hunt-service/"))


                .build();
    }


}

@RestController
class EmptyFallback {

    @GetMapping("/empty-fallback")
    public Flux<Empty> empty() {
        return Flux.empty();
    }
}

class Empty {

}
