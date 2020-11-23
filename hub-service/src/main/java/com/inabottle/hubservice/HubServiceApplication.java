package com.inabottle.hubservice;

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

import java.util.List;
import java.util.Set;
import java.util.UUID;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class HubServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubServiceApplication.class, args);
    }

    @Bean
    ApplicationRunner init(DirectMessageRepository repository) {
        // Electric VWs from https://www.vw.com/electric-concepts/
        // Release dates from https://www.motor1.com/features/346407/volkswagen-id-price-on-sale/
        Hub ID = new Hub(UUID.randomUUID(), "rafaelcorbel@g.com", 1605532776910L, "abc", 15.0, 16.9, 1.0, "1", "bla", List.of("blu", "blu"), null);
        Hub ID2 = new Hub(UUID.randomUUID(), "rafaelcorbel@g.com", 1605532776910L, "abc", 15.0, 16.9, 1.0, "1", "bla", List.of("blu", "ble"), null);
        Hub ID3 = new Hub(UUID.randomUUID(), "rafaelcorbel@g.com", 1605532776910L, "abc", 15.0, 16.9, 1.0, "1", "bla", List.of("blu", "blo"), null);
        Hub ID4 = new Hub(UUID.randomUUID(), "rafaelcorbel@g.com", 1605532776910L, "abc", 15.0, 16.9, 1.0, "1", "bla", List.of("blu", "bli"),
                List.of(new HubMessage(UUID.randomUUID(), "rc@g.com", 1605793876475L, null, 0, 0, 0, "received", null, "ad", List.of(new UserReaction("r@g", new TypeReaction("1", "2", "3"))))));

        Set<Hub> vwConcepts = Set.of(ID, ID2, ID3, ID4);

        return args -> {
            repository
                    .deleteAll()
                    .thenMany(
                            Flux
                                    .just(vwConcepts)
                                    .flatMap(repository::saveAll)
                    )
                    .thenMany(repository.findAll())
                    .subscribe(car -> log.info("saving " + car.toString()));
        };
    }
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class Hub {
    @Id
    private UUID selector;
    private String createdBy;
    private Long createdAt;
    private String password;
    private double reach;
    private double latitude;
    private double longitude;
    private String status = "received";
    private String title;
    private List<String> admin;
    private List<HubMessage> messageChat = List.of();
}


@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class HubMessage {
    @Id
    private UUID selector;
    private String createdBy;
    private Long createdAt;
    private String password;
    private double reach;
    private double latitude;
    private double longitude;
    private String status = "received";
    private String title;
    private String text;
    private List<UserReaction> reactions = List.of();
}


@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class UserReaction {
    private String createdBy;
    private TypeReaction reaction;
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class TypeReaction {
    private String selector;
    private String url;
    private String urlPreview;
}

interface DirectMessageRepository extends ReactiveMongoRepository<Hub, UUID> {
}

@RestController
class HubController {

    private DirectMessageRepository directMessageRepository;

    public HubController(DirectMessageRepository directMessageRepository) {
        this.directMessageRepository = directMessageRepository;
    }

    @PostMapping("/hub")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Hub> addHub(@RequestBody Hub car) {
        return directMessageRepository.save(car);
    }


    @PutMapping("/hub/{id}")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Hub> updateHub(@PathVariable("id") UUID id, @RequestBody Hub hub) {
        hub.setSelector(id);
        return directMessageRepository.save(hub);
    }


    @GetMapping("/hub")
    public Flux<Hub> getHubs() {
        return directMessageRepository.findAll();
    }


    @GetMapping("/hub/{id}")
    public Mono<Hub> getHubs(@PathVariable("id") UUID id) {
        return directMessageRepository.findById(id);
    }

    @PostMapping("/hub/{id}/addMessage")
    public Mono<ResponseEntity<Void>> addMessage(@PathVariable("id") UUID id, @RequestBody HubMessage message) {
        return directMessageRepository.findById(id)
                .flatMap(hub -> saveMessageInHub(hub, message)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/hub/{id}/message/{idMessage}/addReaction")
    public Mono<ResponseEntity<Void>> addReaction(@PathVariable("id") UUID id, @PathVariable("idMessage") UUID idMessage, @RequestBody UserReaction reaction) {
        return directMessageRepository.findById(id)
                .flatMap(hub -> saveReactionInMessage(hub, idMessage, reaction)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @DeleteMapping("/hub/{id}/message/{idMessage}/removeReaction")
    public Mono<ResponseEntity<Void>> removeReaction(@PathVariable("id") UUID id, @PathVariable("idMessage") UUID idMessage, @RequestBody UserReaction reaction) {
        return directMessageRepository.findById(id)
                .flatMap(hub -> removeReactionInMessage(hub, idMessage, reaction)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }


    @DeleteMapping("/hub/{id}")
    public Mono<ResponseEntity<Void>> deleteHub(@PathVariable("id") UUID id) {
        return directMessageRepository.findById(id)
                .flatMap(hub -> directMessageRepository.delete(hub)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    private Mono<ResponseEntity<Void>> saveMessageInHub(Hub hub, HubMessage hubMessage) {
        hub.getMessageChat().add(hubMessage);
        return directMessageRepository.save(hub)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    private Mono<ResponseEntity<Void>> saveReactionInMessage(Hub hub, UUID idMessage, UserReaction userReaction) {
        hub.getMessageChat().stream()
                .filter(hubMessage -> idMessage.equals(hubMessage.getSelector())).findFirst().
                ifPresent(hubMessage -> hubMessage.getReactions().add(userReaction));
        return directMessageRepository.save(hub)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

    private Mono<ResponseEntity<Void>> removeReactionInMessage(Hub hub, UUID idMessage, UserReaction userReaction) {
        hub.getMessageChat().stream()
                .filter(hubMessage -> idMessage.equals(hubMessage.getSelector())).findFirst().
                ifPresent(hubMessage -> hubMessage.getReactions().removeIf(
                        reaction ->
                                reaction.getCreatedBy().equals(userReaction.getCreatedBy()) && reaction.getReaction().getSelector().equals(userReaction.getReaction().getSelector())
                        )
                );
        return directMessageRepository.save(hub)
                .then(Mono.just(new ResponseEntity<>(HttpStatus.OK)));
    }

}
