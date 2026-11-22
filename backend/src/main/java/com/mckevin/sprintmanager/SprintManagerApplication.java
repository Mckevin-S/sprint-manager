package com.mckevin.sprintmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SprintManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(SprintManagerApplication.class, args);
    }
}
