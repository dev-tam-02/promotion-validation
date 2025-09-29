# Adapter Package

## Description
The adapter package contains all the components that connect the application to the outside world. It implements the interfaces defined in the application's port package and adapts external technologies to the application's needs.

## Purpose
The purpose of this package is to isolate the application core from external dependencies and technologies, making the system more maintainable and testable.

## Usage
This package is organized into three main subpackages:
- `config`: Contains configuration classes for the application
- `in`: Contains inbound adapters that handle incoming requests (e.g., REST controllers, message consumers)
- `out`: Contains outbound adapters that interact with external systems (e.g., database repositories, external API clients)

## Examples

### Inbound Adapter Example (REST Controller)
```java
// CampaignController.java in adapter.in.web package
@RestController
@RequestMapping("/api/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CreateCampaignUseCase createCampaignUseCase;
    private final GetCampaignQuery getCampaignQuery;
    private final ActivateCampaignUseCase activateCampaignUseCase;

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(@Valid @RequestBody CreateCampaignRequest request) {
        // Map the request to a command
        CreateCampaignCommand command = mapToCommand(request);

        // Execute the use case
        CampaignId campaignId = createCampaignUseCase.createCampaign(command);

        // Get the created campaign
        CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

        // Map to response and return
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(mapToResponse(campaignDto));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignResponse> getCampaign(@PathVariable("id") String id) {
        // Map the ID string to a domain ID
        CampaignId campaignId = new CampaignId(id);

        // Execute the query
        CampaignDto campaignDto = getCampaignQuery.getCampaignById(campaignId);

        // Map to response and return
        return ResponseEntity.ok(mapToResponse(campaignDto));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateCampaign(@PathVariable("id") String id) {
        // Map the ID string to a domain ID
        CampaignId campaignId = new CampaignId(id);

        // Execute the use case
        activateCampaignUseCase.activateCampaign(campaignId);

        return ResponseEntity.ok().build();
    }

    private CreateCampaignCommand mapToCommand(CreateCampaignRequest request) {
        return new CreateCampaignCommand(
            request.getName(),
            request.getDescription(),
            request.getStartDate(),
            request.getEndDate(),
            TargetAudience.valueOf(request.getTargetAudience()),
            request.getGeographicRestrictions().stream()
                .map(GeographicArea::new)
                .collect(Collectors.toSet()),
            request.getMaxUsagePerCustomer()
        );
    }

    private CampaignResponse mapToResponse(CampaignDto dto) {
        return new CampaignResponse(
            dto.getId(),
            dto.getName(),
            dto.getDescription(),
            dto.getStatus(),
            dto.getStartDate(),
            dto.getEndDate(),
            dto.getTargetAudience(),
            dto.getGeographicRestrictions(),
            dto.getMaxUsagePerCustomer(),
            dto.getPromotions().stream()
                .map(this::mapToPromotionResponse)
                .collect(Collectors.toList())
        );
    }

    private PromotionResponse mapToPromotionResponse(PromotionDto dto) {
        // Mapping logic omitted for brevity
        return new PromotionResponse(
            // ... map DTO properties to response
        );
    }
}
```

### Outbound Adapter Example (JPA Repository)
```java
// JpaCampaignRepository.java in adapter.out.persistence package
@Repository
@RequiredArgsConstructor
public class JpaCampaignRepository implements CampaignRepository {

    private final SpringDataCampaignRepository campaignRepository;
    private final CampaignMapper campaignMapper;

    @Override
    public void save(Campaign campaign) {
        // Map the domain entity to a JPA entity
        CampaignJpaEntity campaignEntity = campaignMapper.mapToJpaEntity(campaign);

        // Save using the Spring Data repository
        campaignRepository.save(campaignEntity);
    }

    @Override
    public Optional<Campaign> findById(CampaignId campaignId) {
        // Find the JPA entity
        Optional<CampaignJpaEntity> campaignEntity = campaignRepository.findById(campaignId.getValue());

        // Map to domain entity if found
        return campaignEntity.map(campaignMapper::mapToDomainEntity);
    }

    @Override
    public List<Campaign> findByStatus(CampaignStatus status) {
        // Find JPA entities by status
        List<CampaignJpaEntity> campaignEntities = campaignRepository.findByStatus(status.name());

        // Map to domain entities
        return campaignEntities.stream()
            .map(campaignMapper::mapToDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public List<Campaign> findValidOnDate(LocalDate date) {
        // Find JPA entities valid on the specified date
        List<CampaignJpaEntity> campaignEntities = campaignRepository.findByStartDateLessThanEqualAndEndDateGreaterThanEqual(date, date);

        // Map to domain entities
        return campaignEntities.stream()
            .map(campaignMapper::mapToDomainEntity)
            .collect(Collectors.toList());
    }

    @Override
    public boolean delete(CampaignId campaignId) {
        // Check if the entity exists
        if (campaignRepository.existsById(campaignId.getValue())) {
            campaignRepository.deleteById(campaignId.getValue());
            return true;
        }
        return false;
    }
}

// Spring Data JPA repository interface
interface SpringDataCampaignRepository extends JpaRepository<CampaignJpaEntity, String> {
    List<CampaignJpaEntity> findByStatus(String status);
    List<CampaignJpaEntity> findByStartDateLessThanEqualAndEndDateGreaterThanEqual(LocalDate date, LocalDate sameDate);
}

// Mapper class
@Component
public class CampaignMapper {

    public CampaignJpaEntity mapToJpaEntity(Campaign campaign) {
        // Mapping logic omitted for brevity
        CampaignJpaEntity entity = new CampaignJpaEntity();
        entity.setId(campaign.getId().getValue());
        entity.setName(campaign.getName());
        // ... map other properties
        return entity;
    }

    public Campaign mapToDomainEntity(CampaignJpaEntity entity) {
        // Mapping logic omitted for brevity
        CampaignId id = new CampaignId(entity.getId());
        DateRange validityPeriod = new DateRange(entity.getStartDate(), entity.getEndDate());
        Campaign campaign = new Campaign(id, entity.getName(), validityPeriod);
        // ... map other properties
        return campaign;
    }
}
```

### Configuration Example
```java
// PersistenceConfig.java in adapter.config package
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
}

// WebConfig.java in adapter.config package
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

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
}
```

## Rules
1. Adapters should only depend on the application's port interfaces, not on domain or application implementation details
2. Inbound adapters should implement the interfaces defined in the application.port.in package
3. Outbound adapters should implement the interfaces defined in the application.port.out package
4. Configuration classes should be placed in the config package
5. Technology-specific code should be contained within this package and not leak into the application or domain layers
