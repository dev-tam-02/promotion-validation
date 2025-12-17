package vn.viettel.vds.promotion.validation.application.service;

import com.promix.platform.core.error.ResponseInfo;
import com.promix.platform.core.exception.BusinessException;
import com.promix.platform.core.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.viettel.vds.promotion.validation.application.port.out.TemporalPolicyPersistencePort;
import vn.viettel.vds.promotion.validation.domain.model.TemporalPolicy;
import vn.viettel.vds.promotion.validation.domain.model.TimeOfDayWindow;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Transactional
public class TemporalPolicyService {

    private static final Logger logger = LoggerFactory.getLogger(TemporalPolicyService.class);

    // Basic RRULE validation pattern
    // Using atomic grouping (?>...) to prevent catastrophic backtracking
    private static final Pattern RRULE_PATTERN = Pattern.compile(
            "^FREQ=(YEARLY|MONTHLY|WEEKLY|DAILY|HOURLY|MINUTELY|SECONDLY)" +
                    "(?:;[A-Z]+=[^;]+)*+$"
    );

    private final TemporalPolicyPersistencePort temporalPolicyPersistencePort;
    private final vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort ruleTemporalLinkPersistencePort;
    private final TemporalPolicyService self;

    public TemporalPolicyService(TemporalPolicyPersistencePort temporalPolicyPersistencePort,
                                 vn.viettel.vds.promotion.validation.application.port.out.RuleTemporalLinkPersistencePort ruleTemporalLinkPersistencePort,
                                 @org.springframework.context.annotation.Lazy TemporalPolicyService self) {
        this.temporalPolicyPersistencePort = temporalPolicyPersistencePort;
        this.ruleTemporalLinkPersistencePort = ruleTemporalLinkPersistencePort;
        this.self = self;
    }

    /**
     * Create a new temporal policy
     */
    public TemporalPolicy createTemporalPolicy(String tenantId, TemporalPolicyRequest request) {
        if (logger.isInfoEnabled()) {
            logger.info("Creating temporal policy: tenant={}, name={}", tenantId, request.name());
        }

        // Check if policy with same name already exists
        if (temporalPolicyPersistencePort.existsByTenantIdAndName(tenantId, request.name())) {
            throw new BusinessException(new ResponseInfo("TEMPORAL_POLICY_EXISTS",
                    "Temporal policy with name '" + request.name() + "' already exists for tenant " + tenantId, 400));
        }

        // Validate timezone
        validateTimezone(request.timezone());

        // Validate RRULE
        if (request.rrule() != null) {
            validateRRule(request.rrule());
        }

        Instant now = Instant.now();
        TemporalPolicy policy = TemporalPolicy.builder()
                .id(generatePolicyId(tenantId, request.name()))
                .name(request.name())
                .tz(request.timezone())
                .rrule(request.rrule())
                .rdate(request.rdate())
                .exrule(request.exrule())
                .exdate(request.exdate())
                .timeOfDayWindows(request.timeWindows())
                .createdAt(now)
                .updatedAt(now)
                .version(0L)
                .build();

        TemporalPolicy saved = temporalPolicyPersistencePort.save(policy);

        logger.info("Temporal policy created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing temporal policy
     */
    public TemporalPolicy updateTemporalPolicy(String policyId, TemporalPolicyRequest request) {
        logger.info("Updating temporal policy: id={}", policyId);

        TemporalPolicy policy = self.getTemporalPolicyById(policyId);

        // Use toBuilder() to create a mutable copy
        TemporalPolicy.TemporalPolicyBuilder builder = policy.toBuilder();

        if (request.name() != null) {
            // Check if new name conflicts with existing policy
            if (!policy.getName().equals(request.name()) &&
                    temporalPolicyPersistencePort.existsByTenantIdAndName(null, request.name())) {
                throw new BusinessException(new ResponseInfo("TEMPORAL_POLICY_EXISTS",
                        "Temporal policy with name '" + request.name() + "' already exists", 400));
            }
            builder.name(request.name());
        }

        if (request.timezone() != null) {
            validateTimezone(request.timezone());
            builder.tz(request.timezone());
        }

        if (request.rrule() != null) {
            validateRRule(request.rrule());
            builder.rrule(request.rrule());
        }

        if (request.rdate() != null) {
            builder.rdate(request.rdate());
        }

        if (request.exrule() != null) {
            builder.exrule(request.exrule());
        }

        if (request.exdate() != null) {
            builder.exdate(request.exdate());
        }

        if (request.timeWindows() != null) {
            builder.timeOfDayWindows(request.timeWindows());
        }

        builder.updatedAt(Instant.now());
        policy = builder.build();

        TemporalPolicy saved = temporalPolicyPersistencePort.save(policy);

        logger.info("Temporal policy updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get temporal policy by ID
     */
    @Transactional(readOnly = true)
    public TemporalPolicy getTemporalPolicyById(String policyId) {
        return temporalPolicyPersistencePort.findById(policyId)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Get temporal policy by tenant and name
     */
    @Transactional(readOnly = true)
    public TemporalPolicy getTemporalPolicyByName(String tenantId, String name) {
        return temporalPolicyPersistencePort.findByTenantIdAndName(tenantId, name)
                .orElseThrow(ResourceNotFoundException::new);
    }

    /**
     * Find temporal policies with filters
     */
    @Transactional(readOnly = true)
    public Page<TemporalPolicy> findTemporalPolicies(String tenantId, String timezone,
                                                     String namePattern, Pageable pageable) {
        return temporalPolicyPersistencePort.findWithFilters(tenantId, timezone, namePattern, pageable);
    }

    /**
     * Get all temporal policies for tenant
     */
    @Transactional(readOnly = true)
    public List<TemporalPolicy> getAllTemporalPolicies(String tenantId) {
        return temporalPolicyPersistencePort.findByTenantIdOrderByNameAsc(tenantId);
    }

    /**
     * Get temporal policies by timezone
     */
    @Transactional(readOnly = true)
    public List<TemporalPolicy> getTemporalPoliciesByTimezone(String tenantId, String timezone) {
        return temporalPolicyPersistencePort.findByTenantIdAndTz(tenantId, timezone);
    }

    /**
     * Preview time windows for a temporal policy
     */
    @Transactional(readOnly = true)
    public List<TimeWindow> previewTimeWindows(String policyId, Instant from, Instant to, String timezone) {
        logger.info("Previewing time windows: policy={}, from={}, to={}", policyId, from, to);

        TemporalPolicy policy = self.getTemporalPolicyById(policyId);

        // This is a simplified preview - in a real implementation you would use
        // a proper RRULE library like ical4j or similar
        return generateTimeWindowsFromPolicy(policy, from, to, timezone);
    }

    /**
     * Validate if a timestamp falls within the temporal policy
     */
    @Transactional(readOnly = true)
    public boolean isTimeValidForPolicy(String policyId, Instant timestamp, String timezone) {
        try {
            TemporalPolicy policy = self.getTemporalPolicyById(policyId);
            return evaluateTemporalPolicy(policy, timestamp, timezone);
        } catch (Exception e) {
            logger.error("Error validating time for policy {}", policyId, e);
            return false;
        }
    }

    private void validateTimezone(String timezone) {
        try {
            ZoneId.of(timezone);
        } catch (Exception e) {
            throw new BusinessException(new ResponseInfo("INVALID_TIMEZONE", "Invalid timezone: " + timezone, 400));
        }
    }

    private void validateRRule(String rrule) {
        if (!RRULE_PATTERN.matcher(rrule).matches()) {
            throw new BusinessException(new ResponseInfo("INVALID_RRULE", "Invalid RRULE format: " + rrule, 400));
        }
    }

    private String generatePolicyId(String tenantId, String name) {
        return "tp_" + tenantId + "_" + name.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private List<TimeWindow> generateTimeWindowsFromPolicy(TemporalPolicy policy, Instant from, Instant to, String timezone) {
        // Simplified implementation - real implementation would use proper RRULE parsing
        // For now, return a basic time window based on the policy's time-of-day windows

        List<TimeWindow> windows = new java.util.ArrayList<>();

        if (policy.getTimeOfDayWindows() != null && !policy.getTimeOfDayWindows().isEmpty()) {
            ZoneId zoneId = ZoneId.of(timezone);
            ZonedDateTime start = from.atZone(zoneId);
            ZonedDateTime end = to.atZone(zoneId);

            // Generate daily windows for the range
            ZonedDateTime current = start.toLocalDate().atStartOfDay(zoneId);
            while (current.isBefore(end)) {
                for (TimeOfDayWindow window : policy.getTimeOfDayWindows()) {
                    // Parse time strings (assumed format: HH:mm)
                    String[] startParts = window.getStart().split(":");
                    String[] endParts = window.getEnd().split(":");

                    ZonedDateTime windowStart = current
                            .withHour(Integer.parseInt(startParts[0]))
                            .withMinute(Integer.parseInt(startParts[1]))
                            .withSecond(0);

                    ZonedDateTime windowEnd = current
                            .withHour(Integer.parseInt(endParts[0]))
                            .withMinute(Integer.parseInt(endParts[1]))
                            .withSecond(0);

                    if (windowStart.isBefore(end) && windowEnd.isAfter(start)) {
                        windows.add(new TimeWindow(windowStart.toInstant(), windowEnd.toInstant()));
                    }
                }
                current = current.plusDays(1);
            }
        }

        return windows;
    }

    private boolean evaluateTemporalPolicy(TemporalPolicy policy, Instant timestamp, String timezone) {
        // Simplified evaluation - real implementation would use proper RRULE evaluation
        if (policy.getTimeOfDayWindows() == null || policy.getTimeOfDayWindows().isEmpty()) {
            return true; // No time restrictions
        }

        ZonedDateTime zonedTime = timestamp.atZone(ZoneId.of(timezone));

        for (TimeOfDayWindow window : policy.getTimeOfDayWindows()) {
            String[] startParts = window.getStart().split(":");
            String[] endParts = window.getEnd().split(":");

            int startHour = Integer.parseInt(startParts[0]);
            int startMinute = Integer.parseInt(startParts[1]);
            int endHour = Integer.parseInt(endParts[0]);
            int endMinute = Integer.parseInt(endParts[1]);

            int currentHour = zonedTime.getHour();
            int currentMinute = zonedTime.getMinute();

            // Check if current time falls within this window
            int currentTotalMinutes = currentHour * 60 + currentMinute;
            int startTotalMinutes = startHour * 60 + startMinute;
            int endTotalMinutes = endHour * 60 + endMinute;

            if (currentTotalMinutes >= startTotalMinutes && currentTotalMinutes <= endTotalMinutes) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get temporal policies by object type and object ID.
     * This method retrieves all temporal policies associated with a specific entity/object
     * through the assignment-temporal_policy relationship.
     *
     * @param objectType The type of the object/entity (e.g., "CAMPAIGN", "DISCOUNT")
     * @param objectId The ID of the object/entity
     * @return List of temporal policies associated with the object
     */
    @Transactional(readOnly = true)
    public List<TemporalPolicyWithMode> getTemporalPoliciesByObjectTypeAndId(String objectType, String objectId) {
        if (logger.isDebugEnabled()) {
            logger.debug("Getting temporal policies for objectType={}, objectId={}", objectType, objectId);
        }

        // Find all temporal links for this object through assignments
        List<vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink> temporalLinks =
                ruleTemporalLinkPersistencePort.findByEntityTypeAndEntityId(objectType, objectId);

        if (temporalLinks.isEmpty()) {
            logger.info("No temporal policies found for objectType={}, objectId={}", objectType, objectId);
            return List.of();
        }

        // Map to TemporalPolicyWithMode by fetching each temporal policy
        List<TemporalPolicyWithMode> result = new java.util.ArrayList<>();
        for (vn.viettel.vds.promotion.validation.domain.model.RuleTemporalLink link : temporalLinks) {
            try {
                TemporalPolicy policy = self.getTemporalPolicyById(link.getTemporalPolicyId());
                result.add(new TemporalPolicyWithMode(policy, link.getMode()));
            } catch (Exception e) {
                logger.warn("Failed to fetch temporal policy id={}, skipping", link.getTemporalPolicyId(), e);
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Found {} temporal policies for objectType={}, objectId={}", result.size(), objectType, objectId);
        }
        return result;
    }

    // Time window class for preview results
    public static class TimeWindow {
        private final Instant start;
        private final Instant end;

        public TimeWindow(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() {
            return start;
        }

        public Instant getEnd() {
            return end;
        }
    }

    /**
     * Find entity IDs by entity type and time range.
     * Delegates to the repository to find all entities that have temporal policies
     * overlapping with the specified time range.
     *
     * @param entityType The type of the entity (e.g., "CASHBACK", "DISCOUNT_COUPON")
     * @param startTs Start timestamp of the query range (nullable)
     * @param endTs End timestamp of the query range (nullable)
     * @return List of distinct entity IDs
     */
    @Transactional(readOnly = true)
    public List<String> findEntityIdsByTypeAndTimeRange(String entityType, Instant startTs, Instant endTs) {
        logger.info("Finding entity IDs by type and time range: entityType={}, startTs={}, endTs={}",
                entityType, startTs, endTs);

        List<String> entityIds = ruleTemporalLinkPersistencePort.findEntityIdsByEntityTypeAndTimeRange(
                entityType, startTs, endTs);

        logger.info("Found {} entity IDs for entityType={}", entityIds.size(), entityType);
        return entityIds;
    }

    /**
     * DTO class to represent a temporal policy with its link mode
     */
    public static class TemporalPolicyWithMode {
        private final TemporalPolicy temporalPolicy;
        private final String mode; // "ALLOW" or "DENY"

        public TemporalPolicyWithMode(TemporalPolicy temporalPolicy, String mode) {
            this.temporalPolicy = temporalPolicy;
            this.mode = mode;
        }

        public TemporalPolicy getTemporalPolicy() {
            return temporalPolicy;
        }

        public String getMode() {
            return mode;
        }
    }
}