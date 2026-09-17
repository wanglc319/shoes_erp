package com.shoerp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import com.shoerp.config.AppProperties;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
@EnableJpaAuditing
@EnableConfigurationProperties(AppProperties.class)
public class ShoeErpApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShoeErpApplication.class, args);
    }
}
