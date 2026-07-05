package org.example.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan({
        "org.example.orderservice",
        "org.example.messagingstarter"
})
@EntityScan({
        "org.example.orderservice",
        "org.example.messagingstarter"
})
@EnableJpaRepositories({
        "org.example.orderservice",
        "org.example.messagingstarter"
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
