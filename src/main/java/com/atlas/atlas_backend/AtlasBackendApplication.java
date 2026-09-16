package com.atlas.atlas_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AtlasBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AtlasBackendApplication.class, args);
	}

}
