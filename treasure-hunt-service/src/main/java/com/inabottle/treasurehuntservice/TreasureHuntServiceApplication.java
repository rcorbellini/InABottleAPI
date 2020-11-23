package com.inabottle.treasurehuntservice;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
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
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class TreasureHuntServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(TreasureHuntServiceApplication.class, args);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(final ConnectionFactory connectionFactory) {
        final var rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(producerJackson2MessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Jackson2JsonMessageConverter producerJackson2MessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class TreasureHunt {
    @Id
    private UUID selector;
    private String createdBy;
    private Long createdAt;
    private String password;
    private double reach;
    private double latitude;
    private double longitude;
    private String status;
    private String description;
    private String title;
    private List<DirectMessage> messages;
    //
    private Integer points;
    private Integer extraPoints;
    private Long startDate;
    private Long endDate;
    private String rewards;
}


@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class DirectMessage implements Serializable {
    @Id
    private UUID selector;
    private String createdBy;
    private Long createdAt;
    private String password;
    private double reach;
    private double latitude;
    private double longitude;
    private String status;
    private String text;
    private String title;
    private UUID huntId;
}

interface TreasureHuntRepository extends ReactiveMongoRepository<TreasureHunt, UUID> {
}

@Component()
class DirectMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public DirectMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public TreasureHunt sendDirectMessage(TreasureHunt treasureHunt) {
        if (treasureHunt.getMessages() != null && !treasureHunt.getMessages().isEmpty()) {
            rabbitTemplate.convertAndSend("inabottle-exchange", "direct.message.save", treasureHunt.getMessages());
        }
        return treasureHunt;
    }
}

@RestController
class TreasureHuntController {

    private TreasureHuntRepository treasureHuntRepository;
    private DirectMessageProducer directMessageProducer;

    public TreasureHuntController(TreasureHuntRepository treasureHuntRepository, DirectMessageProducer directMessageProducer) {
        this.treasureHuntRepository = treasureHuntRepository;
        this.directMessageProducer = directMessageProducer;
    }

    @PostMapping("/treasure")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<TreasureHunt> addMessage(@RequestBody TreasureHunt treasureHunt) {
        return treasureHuntRepository.save(treasureHunt).map(treasureHuntSaved -> directMessageProducer.sendDirectMessage(treasureHuntSaved));
    }

    @GetMapping("/treasure")
    public Flux<TreasureHunt> getMessages() {
        return treasureHuntRepository.findAll();
    }


    @GetMapping("/treasure/{id}")
    public Mono<TreasureHunt> getMessage(@PathVariable("id") UUID id) {
        return treasureHuntRepository.findById(id);
    }

    @DeleteMapping("/treasure/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable("id") UUID id) {
        return treasureHuntRepository.findById(id)
                .flatMap(treasureHunt -> treasureHuntRepository.delete(treasureHunt)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

}


