package vn.viettel.vds.promotion.validation;

import com.promix.batch.autoconfigure.EnablePromixBatch;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vn.viettel.vds.promotion.validation.config.ValidationModuleProperties;

@SpringBootApplication
@EnableFeignClients
@EnableJpaRepositories(basePackages = "vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository")
@EnablePromixBatch
@EnableConfigurationProperties(ValidationModuleProperties.class)
public class ValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }

}
