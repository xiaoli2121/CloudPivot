package io.cloudpivot.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.cloudpivot")
public class CloudPivotBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudPivotBootApplication.class, args);
    }
}
