package com.tearsdeepmind;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class TearsDeepMindApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TearsDeepMindApplication.class, args);
        Crawler crawler = context.getBean(Crawler.class);
        crawler.initializeAndStart();
    }

}
