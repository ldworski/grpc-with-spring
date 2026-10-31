package pl.dworski.grpcwithspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GrpcWithSpringApplication {

    public static void main(String[] args) {
        if (true) {
            throw new IllegalArgumentException("should fail");
        }
        SpringApplication.run(GrpcWithSpringApplication.class, args);
    }

}
