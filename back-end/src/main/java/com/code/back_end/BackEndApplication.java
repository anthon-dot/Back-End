package com.code.back_end;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackEndApplication {

	private static final Logger log =
			LoggerFactory.getLogger(BackEndApplication.class);

	@Value("${server.port}")
	private String serverPort;

	@Value("${spring.datasource.url:}")
	private String datasourceUrl;

	public static void main(String[] args) {
		SpringApplication.run(BackEndApplication.class, args);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void logStartup() {
		log.info(
				"Backend application started on port {}",
				serverPort
		);
		log.info(
				"Database URL configured: {}",
				datasourceUrl == null || datasourceUrl.isBlank()
						? "no"
						: "yes"
		);
	}

}
