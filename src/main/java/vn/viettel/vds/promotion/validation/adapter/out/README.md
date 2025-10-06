# Adapter Out Package

## Description

The adapter.out package contains outbound adapters that implement the interfaces defined in the application.port.out
package. These adapters handle the interaction between the application and external systems or services.

## Purpose

The purpose of this package is to isolate the application core from the details of external systems, such as databases,
message brokers, or third-party services, allowing the application to remain focused on business logic.

## Usage

This package typically contains:

- Database repositories
- HTTP clients for external APIs
- Message producers (Kafka, RabbitMQ, etc.)
- File system adapters
- Email senders
- Cache adapters

Each adapter implements one or more output port interfaces defined in the application.port.out package.

## Examples

### JPA Repository Adapter Example

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

// JPA Entity
@Entity
@Table(name = "campaigns")
@Data
@NoArgsConstructor
public class CampaignJpaEntity {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String targetAudience;

    @ElementCollection
    @CollectionTable(name = "campaign_geographic_areas", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "geographic_area")
    private Set<String> geographicRestrictions = new HashSet<>();

    @Column(nullable = false)
    private int maxUsagePerCustomer;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PromotionJpaEntity> promotions = new ArrayList<>();
}

// Mapper class
@Component
public class CampaignMapper {

    private final PromotionMapper promotionMapper;

    public CampaignMapper(PromotionMapper promotionMapper) {
        this.promotionMapper = promotionMapper;
    }

    public CampaignJpaEntity mapToJpaEntity(Campaign campaign) {
        CampaignJpaEntity entity = new CampaignJpaEntity();
        entity.setId(campaign.getId().getValue());
        entity.setName(campaign.getName());
        entity.setDescription(campaign.getDescription());
        entity.setStartDate(campaign.getValidityPeriod().getStartDate());
        entity.setEndDate(campaign.getValidityPeriod().getEndDate());
        entity.setStatus(campaign.getStatus().name());
        entity.setTargetAudience(campaign.getTargetAudience().name());
        entity.setGeographicRestrictions(campaign.getGeographicRestrictions().stream()
            .map(GeographicArea::getName)
            .collect(Collectors.toSet()));
        entity.setMaxUsagePerCustomer(campaign.getMaxUsagePerCustomer());

        // Map promotions
        List<PromotionJpaEntity> promotionEntities = campaign.getPromotions().stream()
            .map(promotion -> {
                PromotionJpaEntity promotionEntity = promotionMapper.mapToJpaEntity(promotion);
                promotionEntity.setCampaign(entity);
                return promotionEntity;
            })
            .collect(Collectors.toList());
        entity.setPromotions(promotionEntities);

        return entity;
    }

    public Campaign mapToDomainEntity(CampaignJpaEntity entity) {
        CampaignId id = new CampaignId(entity.getId());
        DateRange validityPeriod = new DateRange(entity.getStartDate(), entity.getEndDate());

        Campaign campaign = new Campaign(id, entity.getName(), validityPeriod);
        campaign.setDescription(entity.getDescription());
        campaign.setStatus(CampaignStatus.valueOf(entity.getStatus()));
        campaign.setTargetAudience(TargetAudience.valueOf(entity.getTargetAudience()));
        campaign.setGeographicRestrictions(entity.getGeographicRestrictions().stream()
            .map(GeographicArea::new)
            .collect(Collectors.toSet()));
        campaign.setMaxUsagePerCustomer(entity.getMaxUsagePerCustomer());

        // Map promotions
        Set<Promotion> promotions = entity.getPromotions().stream()
            .map(promotionMapper::mapToDomainEntity)
            .collect(Collectors.toSet());
        campaign.setPromotions(promotions);

        return campaign;
    }
}
```

### REST API Client Example

```java
// ProductCatalogRestClient.java in adapter.out.api package
@Service
@RequiredArgsConstructor
public class ProductCatalogRestClient implements ProductCatalogClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${product-catalog.base-url}")
    private String baseUrl;

    @Value("${product-catalog.api-key}")
    private String apiKey;

    @Override
    public Optional<Product> getProduct(ProductId productId) {
        try {
            // Prepare headers
            HttpHeaders headers = createHeaders();

            // Create request entity
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // Make the request
            ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl + "/products/" + productId.getValue(),
                HttpMethod.GET,
                requestEntity,
                ProductDto.class
            );

            // Map the response to a domain entity
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Optional.of(mapToDomainEntity(response.getBody()));
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error fetching product with ID: {}", productId.getValue(), e);
            throw new ExternalServiceException("Error fetching product from catalog", e);
        }
    }

    @Override
    public Map<ProductId, Product> getProducts(Set<ProductId> productIds) {
        try {
            // Prepare headers
            HttpHeaders headers = createHeaders();

            // Prepare request body
            Map<String, List<String>> requestBody = Map.of(
                "productIds", productIds.stream().map(ProductId::getValue).collect(Collectors.toList())
            );

            // Create request entity
            HttpEntity<Map<String, List<String>>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make the request
            ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                baseUrl + "/products/batch",
                HttpMethod.POST,
                requestEntity,
                new ParameterizedTypeReference<List<ProductDto>>() {}
            );

            // Map the response to domain entities
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                    .map(this::mapToDomainEntity)
                    .collect(Collectors.toMap(Product::getId, Function.identity()));
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            log.error("Error fetching products in batch", e);
            throw new ExternalServiceException("Error fetching products from catalog", e);
        }
    }

    @Override
    public List<Product> getProductsByCategory(CategoryId categoryId) {
        try {
            // Prepare headers
            HttpHeaders headers = createHeaders();

            // Create request entity
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // Make the request
            ResponseEntity<List<ProductDto>> response = restTemplate.exchange(
                baseUrl + "/categories/" + categoryId.getValue() + "/products",
                HttpMethod.GET,
                requestEntity,
                new ParameterizedTypeReference<List<ProductDto>>() {}
            );

            // Map the response to domain entities
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().stream()
                    .map(this::mapToDomainEntity)
                    .collect(Collectors.toList());
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error fetching products for category: {}", categoryId.getValue(), e);
            throw new ExternalServiceException("Error fetching products by category from catalog", e);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-API-Key", apiKey);
        return headers;
    }

    private Product mapToDomainEntity(ProductDto dto) {
        return new Product(
            new ProductId(dto.getId()),
            dto.getName(),
            dto.getDescription(),
            new Money(dto.getPrice(), Currency.getInstance(dto.getCurrency())),
            dto.getCategories().stream()
                .map(CategoryId::new)
                .collect(Collectors.toSet())
        );
    }
}
```

### Kafka Event Publisher Example

```java
// KafkaCampaignEventPublisher.java in adapter.out.messaging package
@Service
@RequiredArgsConstructor
public class KafkaCampaignEventPublisher implements CampaignEventPublisher {

    private final KafkaTemplate<String, CampaignEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.campaign-events}")
    private String campaignEventsTopic;

    @Override
    public void publishCampaignCreatedEvent(Campaign campaign) {
        CampaignEvent event = createCampaignEvent(campaign, CampaignEventType.CREATED);
        publishEvent(event);
    }

    @Override
    public void publishCampaignActivatedEvent(Campaign campaign) {
        CampaignEvent event = createCampaignEvent(campaign, CampaignEventType.ACTIVATED);
        publishEvent(event);
    }

    @Override
    public void publishCampaignCompletedEvent(Campaign campaign) {
        CampaignEvent event = createCampaignEvent(campaign, CampaignEventType.COMPLETED);
        publishEvent(event);
    }

    @Override
    public void publishPromotionAddedEvent(Campaign campaign, Promotion promotion) {
        PromotionEvent promotionEvent = new PromotionEvent(
            promotion.getId().getValue(),
            promotion.getName(),
            promotion.getDiscountMethod().name(),
            promotion.getDiscountValue(),
            campaign.getId().getValue()
        );

        CampaignEvent event = createCampaignEvent(campaign, CampaignEventType.PROMOTION_ADDED);
        event.setPromotion(promotionEvent);

        publishEvent(event);
    }

    private CampaignEvent createCampaignEvent(Campaign campaign, CampaignEventType eventType) {
        return CampaignEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(eventType)
            .campaignId(campaign.getId().getValue())
            .campaignName(campaign.getName())
            .campaignStatus(campaign.getStatus().name())
            .startDate(campaign.getValidityPeriod().getStartDate())
            .endDate(campaign.getValidityPeriod().getEndDate())
            .targetAudience(campaign.getTargetAudience().name())
            .timestamp(LocalDateTime.now())
            .build();
    }

    private void publishEvent(CampaignEvent event) {
        try {
            log.info("Publishing campaign event: {}", event);

            // Send the event to Kafka
            kafkaTemplate.send(campaignEventsTopic, event.getCampaignId(), event)
                .addCallback(
                    result -> log.debug("Event published successfully: {}", event.getEventId()),
                    ex -> log.error("Failed to publish event: {}", event.getEventId(), ex)
                );
        } catch (Exception e) {
            log.error("Error publishing campaign event: {}", event, e);
            throw new MessagingException("Failed to publish campaign event", e);
        }
    }
}
```

### Redis Cache Adapter Example

```java
// RedisCampaignCache.java in adapter.out.cache package
@Service
@RequiredArgsConstructor
public class RedisCampaignCache implements CampaignCache {

    private final RedisTemplate<String, byte[]> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String CACHE_KEY_PREFIX = "campaign:";

    @Override
    public Optional<Campaign> getCampaign(CampaignId campaignId) {
        try {
            String key = CACHE_KEY_PREFIX + campaignId.getValue();
            byte[] serializedCampaign = redisTemplate.opsForValue().get(key);

            if (serializedCampaign == null) {
                return Optional.empty();
            }

            Campaign campaign = objectMapper.readValue(serializedCampaign, Campaign.class);
            return Optional.of(campaign);
        } catch (Exception e) {
            log.warn("Error retrieving campaign from cache: {}", campaignId.getValue(), e);
            return Optional.empty();
        }
    }

    @Override
    public void putCampaign(Campaign campaign, int timeToLive) {
        try {
            String key = CACHE_KEY_PREFIX + campaign.getId().getValue();
            byte[] serializedCampaign = objectMapper.writeValueAsBytes(campaign);

            redisTemplate.opsForValue().set(key, serializedCampaign, Duration.ofSeconds(timeToLive));
            log.debug("Campaign cached successfully: {}", campaign.getId().getValue());
        } catch (Exception e) {
            log.warn("Error caching campaign: {}", campaign.getId().getValue(), e);
        }
    }

    @Override
    public void removeCampaign(CampaignId campaignId) {
        try {
            String key = CACHE_KEY_PREFIX + campaignId.getValue();
            redisTemplate.delete(key);
            log.debug("Campaign removed from cache: {}", campaignId.getValue());
        } catch (Exception e) {
            log.warn("Error removing campaign from cache: {}", campaignId.getValue(), e);
        }
    }

    @Override
    public void clearAllCampaigns() {
        try {
            Set<String> keys = redisTemplate.keys(CACHE_KEY_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Cleared all campaigns from cache, count: {}", keys.size());
            }
        } catch (Exception e) {
            log.warn("Error clearing all campaigns from cache", e);
        }
    }
}
```

## Rules

1. Outbound adapters should implement the interfaces defined in the application.port.out package
2. Adapters should handle the conversion between the application's output model and the external system's format
3. Technical details of the external system should be encapsulated within the adapter
4. Error handling specific to the external system should be implemented here
5. Adapters should not contain business logic
6. Each adapter should be focused on a specific external system or technology
7. Persistence adapters should use the repository pattern
8. External system credentials should not be hardcoded but retrieved from configuration
9. Adapters should be testable with mocks or test doubles
