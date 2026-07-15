package com.code.back_end;

import org.springframework.boot.SpringApplication;

public class TestBackEndApplication {

	public static void main(String[] args) {
		SpringApplication.from(BackEndApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
