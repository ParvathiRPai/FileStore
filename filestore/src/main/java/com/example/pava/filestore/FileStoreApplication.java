package com.example.pava.filestore;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import com.example.pava.filestore.storage.IStorageService;

@SpringBootApplication
public class FileStoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(FileStoreApplication.class, args);
	}

	@Bean
	CommandLineRunner init(IStorageService storageService) {
		return (args) -> {
		};
	}
}
