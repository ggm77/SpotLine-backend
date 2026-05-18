package com.pohanghang.spotline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SpotlineApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpotlineApplication.class, args);
	}

}
