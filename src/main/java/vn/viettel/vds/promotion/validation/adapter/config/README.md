# Adapter Config Package

## Description
The adapter.config package contains configuration classes that set up the application's infrastructure, such as database connections, security settings, and other external dependencies.

## Purpose
The purpose of this package is to centralize configuration code and separate it from the actual adapter implementations, making the system more maintainable and easier to configure.

## Usage
This package typically contains:
- Spring configuration classes (annotated with @Configuration)
- Bean definitions for external dependencies
- Security configuration
- Database configuration
- Messaging configuration
- Other infrastructure-related configurations

## Examples

### Database Configuration Example
```java
// PersistenceConfig.java
@Configuration
@EnableJpaRepositories(basePackages = "vn.viettel.vds.promotion.campaign.adapter.out.persistence")
@EntityScan(basePackages = "vn.viettel.vds.promotion.campaign.adapter.out.persistence")
public class PersistenceConfig {

    @Bean
    public CampaignRepository campaignRepository(SpringDataCampaignRepository springDataRepository, CampaignMapper mapper) {
        return new JpaCampaignRepository(springDataRepository, mapper);
    }

    @Bean
    public CustomerRepository customerRepository(SpringDataCustomerRepository springDataRepository, CustomerMapper mapper) {
        return new JpaCustomerRepository(springDataRepository, mapper);
    }

    @Bean
    public PromotionRepository promotionRepository(SpringDataPromotionRepository springDataRepository, PromotionMapper mapper) {
        return new JpaPromotionRepository(springDataRepository, mapper);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dataSource);
        em.setPackagesToScan("vn.viettel.vds.promotion.campaign.adapter.out.persistence");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        Map<String, Object> properties = new HashMap<>();
        properties.put("hibernate.hbm2ddl.auto", "update");
        properties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.put("hibernate.show_sql", "true");
        em.setJpaPropertyMap(properties);

        return em;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}
```

### Web Configuration Example
```java
// WebConfig.java
@Configuration
@EnableWebMvc
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper()));
    }

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        return objectMapper;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
            .addResourceLocations("classpath:/META-INF/resources/");

        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
```

### Security Configuration Example
```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .csrf().disable()
            .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/api/auth/**").permitAll()
            .antMatchers("/swagger-ui.html", "/v2/api-docs", "/webjars/**", "/swagger-resources/**").permitAll()
            .antMatchers("/api/campaigns/**").hasRole("ADMIN")
            .anyRequest().authenticated()
            .and()
            .apply(new JwtConfigurer(jwtTokenProvider));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        return super.authenticationManagerBean();
    }
}
```

### Messaging Configuration Example
```java
// MessagingConfig.java
@Configuration
@EnableKafka
public class MessagingConfig {

    @Value("${kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public CampaignEventPublisher campaignEventPublisher(KafkaTemplate<String, CampaignEvent> kafkaTemplate) {
        return new KafkaCampaignEventPublisher(kafkaTemplate);
    }

    @Bean
    public ProducerFactory<String, CampaignEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, CampaignEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

## Rules
1. Configuration classes should be placed in this package, not scattered throughout the codebase
2. Configuration should be externalized as much as possible (using properties files, environment variables, etc.)
3. Configuration classes should focus on wiring dependencies, not implementing business logic
4. Sensitive information (passwords, API keys) should not be hardcoded but retrieved from secure sources
5. Configuration should be environment-aware, allowing for different settings in development, testing, and production
6. Bean definitions should be clearly named and documented
