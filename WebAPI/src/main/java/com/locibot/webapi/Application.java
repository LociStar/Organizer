package com.locibot.webapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);
        //LoginClient loginClient = context.getBean(LoginClient.class);
        // We need to block for the content here or the JVM might exit before the message is logged
        //System.out.println(">> message = " + loginClient.getMessage().block());
    }
}
