package vn.viettel.vds.promotion.validation;

import vn.viettel.vds.promotion.validation.domain.enums.OutboxEventStatus;
import vn.viettel.vds.promotion.validation.domain.model.*;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleCode;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleId;
import vn.viettel.vds.promotion.validation.domain.valueobject.RuleName;
import vn.viettel.vds.promotion.validation.domain.valueobject.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public final class TestFixtures {

    private TestFixtures() {
    }

    // --- RuleAggregate ---

    public static RuleAggregate draftRule(String code, String name) {
        return new RuleAggregate(
                RuleCode.of(code),
                RuleName.of(name),
                LogicType.AND,
                "test-user"
        );
    }

    public static RuleAggregate publishedRule(String code, String name) {
        RuleAggregate rule = RuleAggregate.builder()
                .id(RuleId.generate())
                .code(RuleCode.of(code))
                .name(RuleName.of(name))
                .logicType(LogicType.AND)
                .status(RuleStatus.PUBLISHED)
                .version(Version.of(1, 1, 0))
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .createdBy("test-user")
                .updatedBy("test-user")
                .publishedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .publishedBy("test-user")
                .build();
        return rule;
    }

    public static RuleAggregate ruleWithStatus(String code, String name, RuleStatus status) {
        return RuleAggregate.builder()
                .id(RuleId.generate())
                .code(RuleCode.of(code))
                .name(RuleName.of(name))
                .logicType(LogicType.AND)
                .status(status)
                .version(Version.initial())
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-01T00:00:00Z"))
                .createdBy("test-user")
                .updatedBy("test-user")
                .build();
    }

    // --- Rule (authoritative model) ---

    public static Rule activeRule(String id, String ruleCode) {
        return Rule.builder()
                .id(id)
                .code(ruleCode)
                .ruleCode(ruleCode)
                .name("Test Rule")
                .description("Test rule description")
                .state(Rule.RuleState.PUBLISHED)
                .active(true)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .updatedAt(Instant.parse("2026-01-02T00:00:00Z"))
                .createdBy("test-user")
                .updatedBy("test-user")
                .build();
    }

    public static Rule inactiveRule(String id, String ruleCode) {
        return Rule.builder()
                .id(id)
                .code(ruleCode)
                .ruleCode(ruleCode)
                .name("Inactive Rule")
                .active(false)
                .state(Rule.RuleState.DRAFT)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    public static Rule ruleWithSegments(String id, String ruleCode, Set<String> segments) {
        return Rule.builder()
                .id(id)
                .code(ruleCode)
                .ruleCode(ruleCode)
                .name("Segment Rule")
                .active(true)
                .state(Rule.RuleState.PUBLISHED)
                .targetSegments(segments)
                .createdAt(Instant.parse("2026-01-01T00:00:00Z"))
                .build();
    }

    // --- RuleNode ---

    public static RuleNode leafNode(String field, String operator, Object value) {
        return RuleNode.builder()
                .nodeId("node-1")
                .field(field)
                .operator(operator)
                .value(value)
                .build();
    }

    public static RuleNode parentNode(LogicType logicType, List<RuleNode> children) {
        return RuleNode.builder()
                .nodeId("parent-1")
                .logicType(logicType)
                .children(children)
                .build();
    }

    // --- ValidationContext ---

    public static ValidationContext validContext() {
        return ValidationContext.builder()
                .customer(ValidationContext.CustomerContext.builder()
                        .customerId("cust-1")
                        .segment("VIP")
                        .tier("GOLD")
                        .totalPurchaseAmount(BigDecimal.valueOf(500000))
                        .transactionCount(10)
                        .build())
                .order(ValidationContext.OrderContext.builder()
                        .orderId("ord-1")
                        .orderValue(BigDecimal.valueOf(100000))
                        .itemCount(5)
                        .channel("ONLINE")
                        .build())
                .requestTime(Instant.now())
                .build();
    }

    public static ValidationContext contextWithCustomerOnly(String customerId, String segment) {
        return ValidationContext.builder()
                .customer(ValidationContext.CustomerContext.builder()
                        .customerId(customerId)
                        .segment(segment)
                        .build())
                .requestTime(Instant.now())
                .build();
    }

    // --- ValidationRequest ---

    public static ValidationRequest validRequest() {
        return ValidationRequest.builder()
                .transactionId("txn-1")
                .promotionId("promo-1")
                .customerId("cust-1")
                .timestamp(Instant.now())
                .orderValue(BigDecimal.valueOf(50000))
                .validationContext(validContext())
                .build();
    }

    public static ValidationRequest expiredRequest() {
        return ValidationRequest.builder()
                .transactionId("txn-expired")
                .timestamp(Instant.parse("2020-01-01T00:00:00Z"))
                .validationContext(validContext())
                .build();
    }

    public static ValidationRequest highValueRequest() {
        return ValidationRequest.builder()
                .transactionId("txn-high")
                .timestamp(Instant.now())
                .orderValue(BigDecimal.valueOf(2000000))
                .validationContext(validContext())
                .build();
    }

    // --- OutboxEvent ---

    public static OutboxEvent pendingEvent(String id) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateType("Rule")
                .aggregateId("rule-1")
                .eventType("rule.published")
                .status(OutboxEventStatus.PENDING)
                .attempts(0)
                .maxAttempts(3)
                .createdAt(Instant.now())
                .build();
    }

    public static OutboxEvent failedEvent(String id, int attempts, int maxAttempts) {
        return OutboxEvent.builder()
                .id(id)
                .aggregateType("Rule")
                .aggregateId("rule-1")
                .eventType("rule.published")
                .status(OutboxEventStatus.FAILED)
                .attempts(attempts)
                .maxAttempts(maxAttempts)
                .lastError("Connection timeout")
                .createdAt(Instant.now())
                .build();
    }
}
