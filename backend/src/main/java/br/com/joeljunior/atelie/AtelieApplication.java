package br.com.joeljunior.atelie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class AtelieApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Fortaleza"));
        SpringApplication.run(AtelieApplication.class, args);
    }
}
