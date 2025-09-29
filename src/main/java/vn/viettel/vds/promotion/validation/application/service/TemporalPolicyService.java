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
import vn.viettel.vds.promotion.validation.adapter.out.persistence.repository.TemporalPolicyRepository;
import vn.viettel.vds.promotion.validation.domain.entity.TemporalPolicy;

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
    private static final Pattern RRULE_PATTERN = Pattern.compile(
        "^FREQ=(YEARLY|MONTHLY|WEEKLY|DAILY|HOURLY|MINUTELY|SECONDLY)" +
        "(;[A-Z]+=[^;]+)*$"
    );

    private final TemporalPolicyRepository temporalPolicyRepository;

    public TemporalPolicyService(TemporalPolicyRepository temporalPolicyRepository) {
        this.temporalPolicyRepository = temporalPolicyRepository;
    }

    /**
     * Create a new temporal policy
     */
    public TemporalPolicy createTemporalPolicy(String tenantId, String name, String timezone,
                                              String rrule, List<String> rdate, String exrule,
                                              List<String> exdate, List<TemporalPolicy.TimeOfDayWindow> timeWindows) {
        logger.info("Creating temporal policy: tenant={}, name={}", tenantId, name);

        // Check if policy with same name already exists
        if (temporalPolicyRepository.existsByTenantIdAndName(tenantId, name)) {
            throw new BusinessException(new ResponseInfo("TEMPORAL_POLICY_EXISTS",
                "Temporal policy with name '" + name + "' already exists for tenant " + tenantId, 400));
        }

        // Validate timezone
        validateTimezone(timezone);

        // Validate RRULE
        if (rrule != null) {
            validateRRule(rrule);
        }

        TemporalPolicy policy = new TemporalPolicy();
        policy.setId(generatePolicyId(tenantId, name));
        policy.setTenantId(tenantId);
        policy.setName(name);
        policy.setTz(timezone);
        policy.setRrule(rrule);
        policy.setRdate(rdate);
        policy.setExrule(exrule);
        policy.setExdate(exdate);
        policy.setTimeOfDayWindows(timeWindows);
        policy.setCreatedAt(Instant.now());
        policy.setUpdatedAt(Instant.now());

        TemporalPolicy saved = temporalPolicyRepository.save(policy);

        logger.info("Temporal policy created successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Update an existing temporal policy
     */
    public TemporalPolicy updateTemporalPolicy(String policyId, String name, String timezone,
                                              String rrule, List<String> rdate, String exrule,
                                              List<String> exdate, List<TemporalPolicy.TimeOfDayWindow> timeWindows) {
        logger.info("Updating temporal policy: id={}", policyId);

        TemporalPolicy policy = getTemporalPolicyById(policyId);

        if (name != null) {
            // Check if new name conflicts with existing policy
            if (!policy.getName().equals(name) &&
                temporalPolicyRepository.existsByTenantIdAndName(policy.getTenantId(), name)) {
                throw new BusinessException(new ResponseInfo("TEMPORAL_POLICY_EXISTS",
                    "Temporal policy with name '" + name + "' already exists", 400));
            }
            policy.setName(name);
        }

        if (timezone != null) {
            validateTimezone(timezone);
            policy.setTz(timezone);
        }

        if (rrule != null) {
            validateRRule(rrule);
            policy.setRrule(rrule);
        }

        if (rdate != null) {
            policy.setRdate(rdate);
        }

        if (exrule != null) {
            policy.setExrule(exrule);
        }

        if (exdate != null) {
            policy.setExdate(exdate);
        }

        if (timeWindows != null) {
            policy.setTimeOfDayWindows(timeWindows);
        }

        policy.setUpdatedAt(Instant.now());

        TemporalPolicy saved = temporalPolicyRepository.save(policy);

        logger.info("Temporal policy updated successfully: id={}", saved.getId());
        return saved;
    }

    /**
     * Get temporal policy by ID
     */
    @Transactional(readOnly = true)
    public TemporalPolicy getTemporalPolicyById(String policyId) {
        return temporalPolicyRepository.findById(policyId)
            .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Get temporal policy by tenant and name
     */
    @Transactional(readOnly = true)
    public TemporalPolicy getTemporalPolicyByName(String tenantId, String name) {
        return temporalPolicyRepository.findByTenantIdAndName(tenantId, name)
            .orElseThrow(() -> new ResourceNotFoundException());
    }

    /**
     * Find temporal policies with filters
     */
    @Transactional(readOnly = true)
    public Page<TemporalPolicy> findTemporalPolicies(String tenantId, String timezone,
                                                    String namePattern, Pageable pageable) {
        return temporalPolicyRepository.findWithFilters(tenantId, timezone, namePattern, pageable);
    }

    /**
     * Get all temporal policies for tenant
     */
    @Transactional(readOnly = true)
    public List<TemporalPolicy> getAllTemporalPolicies(String tenantId) {
        return temporalPolicyRepository.findByTenantIdOrderByNameAsc(tenantId);
    }

    /**
     * Get temporal policies by timezone
     */
    @Transactional(readOnly = true)
    public List<TemporalPolicy> getTemporalPoliciesByTimezone(String tenantId, String timezone) {
        return temporalPolicyRepository.findByTenantIdAndTz(tenantId, timezone);
    }

    /**
     * Preview time windows for a temporal policy
     */
    @Transactional(readOnly = true)
    public List<TimeWindow> previewTimeWindows(String policyId, Instant from, Instant to, String timezone) {
        logger.info("Previewing time windows: policy={}, from={}, to={}", policyId, from, to);

        TemporalPolicy policy = getTemporalPolicyById(policyId);

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
            TemporalPolicy policy = getTemporalPolicyById(policyId);
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
                for (TemporalPolicy.TimeOfDayWindow window : policy.getTimeOfDayWindows()) {
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

        for (TemporalPolicy.TimeOfDayWindow window : policy.getTimeOfDayWindows()) {
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

    // Time window class for preview results
    public static class TimeWindow {
        private final Instant start;
        private final Instant end;

        public TimeWindow(Instant start, Instant end) {
            this.start = start;
            this.end = end;
        }

        public Instant getStart() { return start; }
        public Instant getEnd() { return end; }
    }
}