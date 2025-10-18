# AVRO SCHEMAS SỬ DỤNG TRONG MODULE VALIDATION

## OUTBOUND (Validation → Services khác)

| Schema | Topic | Mục đích |
|--------|-------|----------|
| SettingValidationRuleEvent | promotion_validation_events | Trả kết quả setting validation rule về cho campaign service (success/failure, assignment/applicability/timeframe results) |
| ValidateStackableDiscountResultEvent | promotion-validation-results | Publish kết quả validation stackable discount |

## INBOUND (Services khác → Validation)

| Schema | Topic | Mục đích |
|--------|-------|----------|
| SettingValidationRuleCommand | promotion_validation_command | Nhận command gán validation rules cho campaign từ campaign service |
| RollbackValidationRuleCommand | promotion_validation_command | Nhận command rollback validation rules từ campaign service (saga compensation) |

---

**Tổng cộng**: 4 schemas đang sử dụng (2 outbound + 2 inbound)

**Pattern**: Command-Event Pattern với Saga Compensation support

**Generated**: 2025-10-18
