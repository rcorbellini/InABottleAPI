package com.inabottle.pointservice;

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
import org.springframework.stereotype.Service;

import java.util.UUID;

@EnableEurekaClient
@EnableDiscoveryClient
@SpringBootApplication
@Slf4j
class PointServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PointServiceApplication.class, args);
    }

    @Bean
    TopicExchange exchange() {
        System.out.println("Criando TopicExchange: ");
        return new TopicExchange("inabottle-exchange");
    }

    @Bean
    Queue queue() {
        return new Queue("points-queue", false);
    }

    @Bean
    Binding binding(Queue queue, TopicExchange exchange) {
        System.out.println("Criando Biding: ");
        return BindingBuilder.bind(queue).to(exchange).with("points.#");
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
class PointsHistory {
    @Id
    private UUID selector;
    private String createdBy;
    private UUID idSource;
    private String typeSource;
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

@Service
class DirectMessageListener {

    private final PointsHistoryRepository pointsHistoryRepository;

    public DirectMessageListener(PointsHistoryRepository pointsHistoryRepository) {
        System.out.println("Criando consumer: ");
        this.pointsHistoryRepository = pointsHistoryRepository;
    }

    @RabbitListener(queues = "points-queue")
    public void readMessage(PointsHistory points) {
        pointsHistoryRepository.save(points).subscribe();
    }
}

