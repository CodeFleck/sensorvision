package io.indcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IndCloudApplication {

    public static void main(String[] args) {
        SpringApplication.run(IndCloudApplication.class, args);
    }
}
