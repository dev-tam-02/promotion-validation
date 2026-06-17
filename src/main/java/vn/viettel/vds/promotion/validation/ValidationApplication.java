package vn.viettel.vds.promotion.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoReactiveDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoReactiveAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import vn.viettel.vds.promotion.validation.config.ValidationModuleProperties;

// Promix outbox (JPA) is now the single outbox: RuleService/PublishService write
// via OutboxService and the promix scheduler publishes to Kafka. The previous
// exclude of PromixOutboxJpaAutoConfiguration (custom HTTP outbox) was removed.
@SpringBootApplication(exclude = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        MongoReactiveAutoConfiguration.class,
        MongoReactiveDataAutoConfiguration.class
})
@EnableFeignClients
@EnableJpaRepositories(basePackages = "vn.viettel.vds.promotion.validation.adapter.out.persistence.jpa.repository")
@EnableConfigurationProperties(ValidationModuleProperties.class)
public class ValidationApplication {

    public static void main(String[] args) {
        SpringApplication.run(ValidationApplication.class, args);
    }

}
