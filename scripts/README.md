# MongoDB Sample Data Scripts for Validation Module

This directory contains MongoDB scripts to generate sample data for the validation module.

## Scripts

### 1. `mongo-sample-data.js`

Complete sample data script with comprehensive test data including:

- 8 Reason Codes (error codes and messages)
- 10 Operators (validation operators like equals, greater_than, etc.)
- 8 Validation Rules (various business rules)
- Rule Versions (version history)
- Temporal Policies (time-based rules)
- Assignments (rule-to-campaign mappings)
- Publish Jobs (deployment history)
- Audit Logs (change history)
- Validation Rule Instances (runtime rules)
- Rule Temporal Links
- Outbox Events (event-driven architecture)

### 2. `mongo-sample-data-minimal.js`

Minimal sample data for quick testing:

- 2 Reason Codes
- 2 Operators
- 1 Validation Rule

## Usage

### Prerequisites

- MongoDB installed and running
- Default connection: `localhost:27017`
- Database name: `validation_db`

### Running the Scripts

#### Full Sample Data

```bash
# Without authentication
mongosh localhost:27017/validation_db < mongo-sample-data.js

# With authentication
mongosh -u username -p password localhost:27017/validation_db < mongo-sample-data.js

# Custom MongoDB host/port
mongosh mongodb://host:port/validation_db < mongo-sample-data.js
```

#### Minimal Sample Data

```bash
# Quick test data
mongosh localhost:27017/validation_db < mongo-sample-data-minimal.js
```

### Docker MongoDB

If using Docker:

```bash
# Start MongoDB container
docker run -d --name mongo-validation -p 27017:27017 mongo:latest

# Run script
docker exec -i mongo-validation mongosh validation_db < mongo-sample-data.js

# Or copy script and run
docker cp mongo-sample-data.js mongo-validation:/tmp/
docker exec mongo-validation mongosh validation_db /tmp/mongo-sample-data.js
```

## Data Overview

### Collections Created

| Collection            | Description                                 | Sample Count |
|-----------------------|---------------------------------------------|--------------|
| `rules`               | Validation rule definitions                 | 8            |
| `operators`           | Validation operators (equals, gt, lt, etc.) | 10           |
| `reason_codes`        | Error codes and messages                    | 8            |
| `rule_versions`       | Rule version history                        | 3            |
| `temporal_policies`   | Time-based policies                         | 3            |
| `assignments`         | Rule assignments to campaigns/promotions    | 5            |
| `publish_jobs`        | Rule deployment jobs                        | 5            |
| `audit_logs`          | Audit trail                                 | 8            |
| `validation_rules`    | Active rule instances                       | 3            |
| `rule_temporal_links` | Rule-policy associations                    | 3            |
| `outbox_events`       | Event sourcing outbox                       | 4            |

### Sample Rules

1. **RULE_CUSTOMER_ELIGIBILITY** - Checks customer eligibility (status, age, segment)
2. **RULE_ORDER_VALIDATION** - Validates order amounts and item counts
3. **RULE_PROMOTION_TIME** - Checks promotion time windows
4. **RULE_LOCATION_CHECK** - Location-based eligibility
5. **RULE_PRODUCT_ELIGIBILITY** - Product promotion eligibility
6. **RULE_BUDGET_CHECK** - Budget availability validation
7. **RULE_CUSTOMER_LIMIT** - Customer usage limits
8. **RULE_PAYMENT_METHOD** - Payment method validation (archived)

### Sample Operators

- `equals` - Exact match comparison
- `greater_than` - Numeric greater than
- `less_than` - Numeric less than
- `between` - Range check
- `in_list` - Value in list
- `contains` - String contains
- `regex_match` - Pattern matching
- `is_null` - Null check
- `date_before` - Date comparison
- `date_after` - Date comparison

### Sample Tenant

All data uses tenant ID: `TENANT_001`

## Verification

After running the script, verify data:

```javascript
// Connect to MongoDB
mongosh localhost:27017/validation_db

// Check collections
show collections

// Count documents
db.rules.countDocuments()
db.operators.countDocuments()
db.reason_codes.countDocuments()

// View sample rule
db.rules.findOne()

// View rules by state
db.rules.find({ state: "PUBLISHED" })

// View active operators
db.operators.find({ status: "ACTIVE" })
```

## Cleanup

To remove all sample data:

```javascript
// Connect to database
use validation_db

// Drop all collections
db.dropDatabase()
```

## Notes

- All timestamps are generated relative to current date
- UUIDs are generated using MongoDB ObjectId
- Sample data includes various rule states: DRAFT, PUBLISHED, ARCHIVED
- Includes both successful and failed publish jobs
- Audit logs track all major operations
- Outbox events support event-driven architecture patterns

## Customization

To modify the sample data:

1. Edit the JavaScript files directly
2. Adjust tenant IDs, date ranges, or data volumes
3. Add more complex rule configurations
4. Extend with additional test scenarios