package com.example.investigationservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.kafka.listener.auto-startup=false")
class InvestigationServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
