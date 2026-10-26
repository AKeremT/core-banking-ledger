package com.akt.banking_v1;

import org.springframework.boot.SpringApplication;

public class TestBankingV1Application {

	public static void main(String[] args) {
		SpringApplication.from(BankingV1Application::main).with(TestcontainersConfiguration.class).run(args);
	}

}
