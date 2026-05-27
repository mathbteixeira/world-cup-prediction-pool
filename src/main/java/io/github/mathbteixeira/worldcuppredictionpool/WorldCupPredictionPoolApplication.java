package io.github.mathbteixeira.worldcuppredictionpool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WorldCupPredictionPoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(WorldCupPredictionPoolApplication.class, args);
    }
}
