package com.bank.yankiservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication(exclude = {RedisReactiveAutoConfiguration.class})
@EnableEurekaClient
public class YankiServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(YankiServiceApplication.class, args);
	}

}
