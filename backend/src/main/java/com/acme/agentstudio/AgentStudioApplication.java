package com.acme.agentstudio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AgentStudioApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentStudioApplication.class, args);
    }
}
