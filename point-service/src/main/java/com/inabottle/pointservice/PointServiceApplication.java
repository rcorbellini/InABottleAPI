package com.inabottle.pointservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;
import java.util.UUID;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class PointServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointServiceApplication.class, args);
    }
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class PointsHistory {
    @Id
    private UUID selector;
    private String createdBy;
    private String source;
    private Integer amount;
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class UserPoints {
    private String createdBy;
    private Integer amount;
}

interface PointsHistoryRepository extends ReactiveMongoRepository<PointsHistory, UUID> {
}

interface UserPointsRepository extends ReactiveMongoRepository<UserPoints, UUID> {
}

@RestController
class UserPointsController {

    private UserPointsRepository userPointsRepository;

    public UserPointsController(UserPointsRepository userPointsRepository) {
        this.userPointsRepository = userPointsRepository;
    }

    @PostMapping("/point")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<UserPoints> add(@RequestBody UserPoints userPoints) {
        return userPointsRepository.save(userPoints);
    }

    @GetMapping("/point")
    public Flux<UserPoints> getMessages() {
        return userPointsRepository.findAll();
    }


    @GetMapping("/point/{id}")
    public Mono<UserPoints> getMessage(@PathVariable("id") UUID id) {
        return userPointsRepository.findById(id);
    }

    @DeleteMapping("/point/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable("id") UUID id) {
        return userPointsRepository.findById(id)
                .flatMap(userPoints -> userPointsRepository.delete(userPoints)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
