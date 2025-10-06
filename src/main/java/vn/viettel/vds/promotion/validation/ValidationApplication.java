package vn.viettel.vds.promotion.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories(basePackages = "vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository")
public class ValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }

}
