package com.amar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class EcomOrderServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EcomOrderServiceApplication.class, args);
	}

}
