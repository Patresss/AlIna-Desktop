package com.patres.alina.server;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

//@SpringBootApplication
public class ServerApplication {

	public static void main(String[] args) {
        new SpringApplicationBuilder(ServerApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
	}

}
