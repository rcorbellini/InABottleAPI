package com.example.directmessageservice;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
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
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class DirectMessageServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DirectMessageServiceApplication.class, args);
    }

    @Bean
    TopicExchange exchange() {
        System.out.println("Criando TopicExchange: ");
        return new TopicExchange("inabottle-exchange");
    }

    @Bean
    Queue queue() {
        return new Queue("direct-message-queue", false);
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        System.out.println("Criando Biding: ");
        return BindingBuilder.bind(queue).to(exchange).with("direct.message.#");
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

interface DirectMessageRepository extends ReactiveMongoRepository<DirectMessage, UUID> {
}

@Service
class DirectMessageListener {

    private DirectMessageRepository directMessageRepository;
    private ObjectMapper jsonObjectMapper;

    public DirectMessageListener(DirectMessageRepository directMessageRepository, ObjectMapper jsonObjectMapper) {
        System.out.println("Criando consumer: ");
        this.jsonObjectMapper = jsonObjectMapper;
        this.directMessageRepository = directMessageRepository;
    }

    @RabbitListener(queues = "direct-message-queue")
    public void readMessage(List<Map<String, Object>> messages) {
        final List<DirectMessage> teste =  messages.stream().map(map -> jsonObjectMapper.convertValue(map, DirectMessage.class)).collect(Collectors.toList());
        final Disposable returno = directMessageRepository.saveAll(teste).subscribe();
    }
}

@RestController
class DirectMessageController {

    private DirectMessageRepository directMessageRepository;

    public DirectMessageController(DirectMessageRepository directMessageRepository) {
        this.directMessageRepository = directMessageRepository;
    }

    @PostMapping("/direct")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<DirectMessage> addMessage(@RequestBody DirectMessage car) {
        return directMessageRepository.save(car);
    }

    @GetMapping("/direct")
    public Flux<DirectMessage> getMessages() {
        System.out.println("ok");
        return directMessageRepository.findAll();
    }

    @GetMapping("/direct/{id}")
    public Mono<DirectMessage> getMessage(@PathVariable("id") UUID id) {
        return directMessageRepository.findById(id);
    }

    @DeleteMapping("/direct/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable("id") UUID id) {
        return directMessageRepository.findById(id)
                .flatMap(car -> directMessageRepository.delete(car)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
