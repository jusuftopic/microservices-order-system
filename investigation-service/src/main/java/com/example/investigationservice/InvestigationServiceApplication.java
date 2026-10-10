package com.example.investigationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class InvestigationServiceApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(
                InvestigationServiceApplication.class,
                args
        );
        if (context.getEnvironment().getProperty(
                "app.rag.ingestion.enabled",
                Boolean.class,
                false
        )) {
            context.close();
        }
    }

}
