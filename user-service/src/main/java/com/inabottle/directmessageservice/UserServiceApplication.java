package com.inabottle.directmessageservice;


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
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.util.UUID;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }

    @Bean
    TopicExchange exchange() {
        System.out.println("Criando TopicExchange: ");
        return new TopicExchange("inabottle-exchange");
    }

    @Bean
    Queue queue() {
        return new Queue("user-queue", false);
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
class User implements Serializable {
    @Id
    private UUID id;
    private String name;
    private String email;
    private String photoUrl;
    private Integer points;
    private String cellphone;
}

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
class UserPoints {
    private String createdBy;
    private Integer amount;
}


interface UserRepository extends ReactiveMongoRepository<User, UUID> {
    @Query("{ 'email' : ?0 }")
    Mono<User> findUsersByEmail(String email);

}

@Service
class UserListener {

    private UserRepository userRepository;
    private ObjectMapper jsonObjectMapper;

    public UserListener(UserRepository userRepository, ObjectMapper jsonObjectMapper) {
        System.out.println("Criando consumer: ");
        this.jsonObjectMapper = jsonObjectMapper;
        this.userRepository = userRepository;
    }

    @RabbitListener(queues = "user-queue")
    public void readMessage(UserPoints userPoints) {
        userRepository.findUsersByEmail(userPoints.getCreatedBy())
                .flatMap(user -> userRepository.save(user)).subscribe();
    }
}

@RestController
class UserController {

    private UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @PostMapping("/user/login")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> login(@RequestBody User car) {
        return userRepository.save(car);
    }

    @PostMapping("/user")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<User> addUser(@RequestBody User car) {
        return userRepository.save(car);
    }

    @GetMapping("/user")
    public Flux<User> getUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/user/{email}")
    public Mono<User> getUser(@PathVariable("email") String email) {
        return userRepository.findUsersByEmail(email);
    }

    @DeleteMapping("/direct/{id}")
    public Mono<ResponseEntity<Void>> deleteMessage(@PathVariable("id") UUID id) {
        return userRepository.findById(id)
                .flatMap(car -> userRepository.delete(car)
                        .then(Mono.just(new ResponseEntity<Void>(HttpStatus.OK)))
                )
                .defaultIfEmpty(new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }
}
