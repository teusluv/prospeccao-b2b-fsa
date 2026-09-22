package br.com.fsa.prospeccao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ProspeccaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProspeccaoApplication.class, args);
    }
}
