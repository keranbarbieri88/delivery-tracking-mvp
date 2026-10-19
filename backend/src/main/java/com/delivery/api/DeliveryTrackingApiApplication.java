package com.delivery.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.nio.file.Paths;

@SpringBootApplication
public class DeliveryTrackingApiApplication {

    public static void main(String[] args) {
        validarDiretorioDeExecucao();
        SpringApplication.run(DeliveryTrackingApiApplication.class, args);
    }

    private static void validarDiretorioDeExecucao() {
        String currentDir = Paths.get("").toAbsolutePath().toString();
        if (!currentDir.endsWith("backend")) {
            System.err.println("\n=======================================================");
            System.err.println("ERRO: Execução bloqueada!");
            System.err.println("A API deve ser executada a partir da pasta 'backend'.");
            System.err.println("Directório actual: " + currentDir);
            System.err.println("Execute: cd backend");
            System.err.println("=======================================================\n");
            System.exit(1);
        }
    }
}