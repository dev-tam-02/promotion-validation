// MongoDB Sample Data Script for Validation Module
// Run this script using: mongosh localhost:27017/validation_db < mongo-sample-data.js

// Switch to the validation database
db = db.getSiblingDB('validation_db');

// Clear existing collections
print("Clearing existing collections...");
db.rules.drop();
db.operators.drop();
db.reason_codes.drop();
db.rule_versions.drop();
db.temporal_policies.drop();
db.assignments.drop();
db.publish_jobs.drop();
db.audit_logs.drop();

// Helper function to generate UUIDs
function generateId() {
    return ObjectId().toString();
}

// Helper function to get current timestamp
function currentTimestamp() {
    return new Date();
}

// Helper function to get past timestamp
function pastTimestamp(daysAgo) {
    var date = new Date();
    date.setDate(date.getDate() - daysAgo);
    return date;
}

// ===========================
// 1. Create Reason Codes
// ===========================
print("\nCreating reason codes...");
const reasonCodes = [
    {
        _id: "RC001",
        tenantId: "TENANT_001",
        category: "ELIGIBILITY",
        severity: "ERROR",
        labels: {
            "vi": "Khách hàng không đủ điều kiện",
            "en": "Customer not eligible"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC002",
        tenantId: "TENANT_001",
        category: "LIMIT",
        severity: "WARN",
        labels: {
            "vi": "Đã đạt giới hạn sử dụng",
            "en": "Usage limit reached"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC003",
        tenantId: "TENANT_001",
        category: "BUDGET",
        severity: "ERROR",
        labels: {
            "vi": "Ngân sách khuyến mãi đã hết",
            "en": "Promotion budget exhausted"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC004",
        tenantId: "TENANT_001",
        category: "TIME",
        severity: "INFO",
        labels: {
            "vi": "Chương trình chưa bắt đầu",
            "en": "Program not yet started"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC005",
        tenantId: "TENANT_001",
        category: "TIME",
        severity: "ERROR",
        labels: {
            "vi": "Chương trình đã kết thúc",
            "en": "Program has ended"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC006",
        tenantId: "TENANT_001",
        category: "PRODUCT",
        severity: "WARN",
        labels: {
            "vi": "Sản phẩm không áp dụng khuyến mãi",
            "en": "Product not eligible for promotion"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC007",
        tenantId: "TENANT_001",
        category: "LOCATION",
        severity: "ERROR",
        labels: {
            "vi": "Khu vực không được hỗ trợ",
            "en": "Location not supported"
        },
        createdAt: pastTimestamp(30)
    },
    {
        _id: "RC008",
        tenantId: "TENANT_001",
        category: "SEGMENT",
        severity: "WARN",
        labels: {
            "vi": "Phân khúc khách hàng không phù hợp",
            "en": "Customer segment mismatch"
        },
        createdAt: pastTimestamp(30)
    }
];

db.reason_codes.insertMany(reasonCodes);
print(`Created ${reasonCodes.length} reason codes`);

// ===========================
// 2. Create Operators
// ===========================
print("\nCreating operators...");
const operators = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "equals",
        version: 1,
        context: "general",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: ["string", "number", "boolean"] }
            },
            required: ["field", "value"]
        },
        compilerId: "COMP_EQ_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "greater_than",
        version: 1,
        context: "numeric",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: "number" }
            },
            required: ["field", "value"]
        },
        compilerId: "COMP_GT_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "less_than",
        version: 1,
        context: "numeric",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: "number" }
            },
            required: ["field", "value"]
        },
        compilerId: "COMP_LT_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "in_list",
        version: 1,
        context: "general",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                values: {
                    type: "array",
                    items: { type: ["string", "number"] }
                }
            },
            required: ["field", "values"]
        },
        compilerId: "COMP_IN_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "between",
        version: 1,
        context: "numeric",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                min: { type: "number" },
                max: { type: "number" }
            },
            required: ["field", "min", "max"]
        },
        compilerId: "COMP_BTW_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "contains",
        version: 1,
        context: "string",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: "string" }
            },
            required: ["field", "value"]
        },
        compilerId: "COMP_CONT_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "regex_match",
        version: 1,
        context: "string",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                pattern: { type: "string" }
            },
            required: ["field", "pattern"]
        },
        compilerId: "COMP_REGEX_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "is_null",
        version: 1,
        context: "general",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" }
            },
            required: ["field"]
        },
        compilerId: "COMP_NULL_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "date_before",
        version: 1,
        context: "date",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                date: { type: "string", format: "date-time" }
            },
            required: ["field", "date"]
        },
        compilerId: "COMP_DATEBEF_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "date_after",
        version: 1,
        context: "date",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                date: { type: "string", format: "date-time" }
            },
            required: ["field", "date"]
        },
        compilerId: "COMP_DATEAFT_001",
        status: "ACTIVE",
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    }
];

db.operators.insertMany(operators);
print(`Created ${operators.length} operators`);

// ===========================
// 3. Create Validation Rules
// ===========================
print("\nCreating validation rules...");
const rules = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_CUSTOMER_ELIGIBILITY",
        name: "Customer Eligibility Check",
        state: "PUBLISHED",
        latestVersion: 2,
        logic: "ALL",
        limits: {
            maxUsagePerCustomer: 5,
            maxUsagePerDay: 100,
            maxBudget: 10000000
        },
        nodes: [
            {
                id: "node_1",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_1_1",
                        type: "COND",
                        operatorName: "equals",
                        params: {
                            field: "customer.status",
                            value: "ACTIVE"
                        },
                        reasonCode: "RC001"
                    },
                    {
                        id: "node_1_2",
                        type: "COND",
                        operatorName: "greater_than",
                        params: {
                            field: "customer.accountAge",
                            value: 30
                        },
                        reasonCode: "RC001"
                    },
                    {
                        id: "node_1_3",
                        type: "COND",
                        operatorName: "in_list",
                        params: {
                            field: "customer.segment",
                            values: ["VIP", "GOLD", "SILVER"]
                        },
                        reasonCode: "RC008"
                    }
                ]
            }
        ],
        notes: "Basic customer eligibility rule for promotions",
        createdAt: pastTimestamp(20),
        createdBy: "admin",
        updatedAt: pastTimestamp(5),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_ORDER_VALIDATION",
        name: "Order Validation Rule",
        state: "PUBLISHED",
        latestVersion: 1,
        logic: "ALL",
        limits: {
            minOrderAmount: 100000,
            maxOrderAmount: 50000000
        },
        nodes: [
            {
                id: "node_2",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_2_1",
                        type: "COND",
                        operatorName: "between",
                        params: {
                            field: "order.totalAmount",
                            min: 100000,
                            max: 50000000
                        },
                        reasonCode: "RC002"
                    },
                    {
                        id: "node_2_2",
                        type: "COND",
                        operatorName: "greater_than",
                        params: {
                            field: "order.itemCount",
                            value: 0
                        },
                        reasonCode: "RC006"
                    }
                ]
            }
        ],
        notes: "Validates order meets minimum requirements",
        createdAt: pastTimestamp(15),
        createdBy: "admin",
        updatedAt: pastTimestamp(15),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_PROMOTION_TIME",
        name: "Promotion Time Window Check",
        state: "PUBLISHED",
        latestVersion: 1,
        logic: "ALL",
        limits: {},
        nodes: [
            {
                id: "node_3",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_3_1",
                        type: "COND",
                        operatorName: "date_after",
                        params: {
                            field: "currentDate",
                            date: "2025-01-01T00:00:00Z"
                        },
                        reasonCode: "RC004"
                    },
                    {
                        id: "node_3_2",
                        type: "COND",
                        operatorName: "date_before",
                        params: {
                            field: "currentDate",
                            date: "2025-12-31T23:59:59Z"
                        },
                        reasonCode: "RC005"
                    }
                ]
            }
        ],
        notes: "Checks if current date is within promotion period",
        createdAt: pastTimestamp(10),
        createdBy: "admin",
        updatedAt: pastTimestamp(10),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_LOCATION_CHECK",
        name: "Location Eligibility",
        state: "PUBLISHED",
        latestVersion: 1,
        logic: "ANY",
        limits: {},
        nodes: [
            {
                id: "node_4",
                type: "GROUP",
                groupLogic: "ANY",
                children: [
                    {
                        id: "node_4_1",
                        type: "COND",
                        operatorName: "in_list",
                        params: {
                            field: "customer.location.city",
                            values: ["Hanoi", "HoChiMinh", "DaNang", "HaiPhong", "CanTho"]
                        },
                        reasonCode: "RC007"
                    },
                    {
                        id: "node_4_2",
                        type: "COND",
                        operatorName: "equals",
                        params: {
                            field: "customer.location.region",
                            value: "URBAN"
                        },
                        reasonCode: "RC007"
                    }
                ]
            }
        ],
        notes: "Check if customer is in eligible location",
        createdAt: pastTimestamp(8),
        createdBy: "admin",
        updatedAt: pastTimestamp(8),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_PRODUCT_ELIGIBILITY",
        name: "Product Eligibility Check",
        state: "DRAFT",
        latestVersion: 1,
        logic: "ALL",
        limits: {},
        nodes: [
            {
                id: "node_5",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_5_1",
                        type: "COND",
                        operatorName: "in_list",
                        params: {
                            field: "product.category",
                            values: ["ELECTRONICS", "FASHION", "HOME", "BEAUTY"]
                        },
                        reasonCode: "RC006"
                    },
                    {
                        id: "node_5_2",
                        type: "COND",
                        operatorName: "equals",
                        params: {
                            field: "product.promotionEligible",
                            value: true
                        },
                        reasonCode: "RC006"
                    },
                    {
                        id: "node_5_3",
                        type: "COND",
                        operatorName: "greater_than",
                        params: {
                            field: "product.stock",
                            value: 0
                        },
                        reasonCode: "RC006"
                    }
                ]
            }
        ],
        notes: "Validates product eligibility for promotions",
        createdAt: pastTimestamp(5),
        createdBy: "admin",
        updatedAt: pastTimestamp(5),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_BUDGET_CHECK",
        name: "Budget Availability Check",
        state: "PUBLISHED",
        latestVersion: 1,
        logic: "ALL",
        limits: {
            dailyBudget: 5000000,
            monthlyBudget: 100000000
        },
        nodes: [
            {
                id: "node_6",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_6_1",
                        type: "COND",
                        operatorName: "less_than",
                        params: {
                            field: "promotion.usedBudget",
                            value: 100000000
                        },
                        reasonCode: "RC003"
                    },
                    {
                        id: "node_6_2",
                        type: "COND",
                        operatorName: "less_than",
                        params: {
                            field: "promotion.dailyUsedBudget",
                            value: 5000000
                        },
                        reasonCode: "RC003"
                    }
                ]
            }
        ],
        notes: "Ensures promotion budget is available",
        createdAt: pastTimestamp(7),
        createdBy: "admin",
        updatedAt: pastTimestamp(2),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_CUSTOMER_LIMIT",
        name: "Customer Usage Limit Check",
        state: "PUBLISHED",
        latestVersion: 1,
        logic: "ALL",
        limits: {
            maxUsagePerCustomer: 3,
            cooldownPeriodDays: 7
        },
        nodes: [
            {
                id: "node_7",
                type: "GROUP",
                groupLogic: "ALL",
                children: [
                    {
                        id: "node_7_1",
                        type: "COND",
                        operatorName: "less_than",
                        params: {
                            field: "customer.promotionUsageCount",
                            value: 3
                        },
                        reasonCode: "RC002"
                    },
                    {
                        id: "node_7_2",
                        type: "COND",
                        operatorName: "date_before",
                        params: {
                            field: "customer.lastPromotionDate",
                            date: "{{currentDate - 7days}}"
                        },
                        reasonCode: "RC002"
                    }
                ]
            }
        ],
        notes: "Limits customer promotion usage",
        createdAt: pastTimestamp(6),
        createdBy: "admin",
        updatedAt: pastTimestamp(6),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        code: "RULE_PAYMENT_METHOD",
        name: "Payment Method Validation",
        state: "ARCHIVED",
        latestVersion: 1,
        logic: "ANY",
        limits: {},
        nodes: [
            {
                id: "node_8",
                type: "GROUP",
                groupLogic: "ANY",
                children: [
                    {
                        id: "node_8_1",
                        type: "COND",
                        operatorName: "in_list",
                        params: {
                            field: "payment.method",
                            values: ["CREDIT_CARD", "DEBIT_CARD", "E_WALLET"]
                        },
                        reasonCode: "RC001"
                    },
                    {
                        id: "node_8_2",
                        type: "COND",
                        operatorName: "equals",
                        params: {
                            field: "payment.verified",
                            value: true
                        },
                        reasonCode: "RC001"
                    }
                ]
            }
        ],
        notes: "Old payment method validation rule - archived",
        createdAt: pastTimestamp(25),
        createdBy: "admin",
        updatedAt: pastTimestamp(15),
        updatedBy: "admin"
    }
];

db.rules.insertMany(rules);
print(`Created ${rules.length} validation rules`);

// ===========================
// 4. Create Rule Versions
// ===========================
print("\nCreating rule versions...");
const ruleVersions = [
    {
        _id: generateId(),
        ruleId: rules[0]._id,
        tenantId: "TENANT_001",
        version: 1,
        content: {
            logic: "ALL",
            nodes: rules[0].nodes,
            limits: rules[0].limits
        },
        status: "ARCHIVED",
        publishedAt: pastTimestamp(20),
        createdAt: pastTimestamp(20),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        ruleId: rules[0]._id,
        tenantId: "TENANT_001",
        version: 2,
        content: {
            logic: "ALL",
            nodes: rules[0].nodes,
            limits: rules[0].limits
        },
        status: "ACTIVE",
        publishedAt: pastTimestamp(5),
        createdAt: pastTimestamp(5),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        ruleId: rules[1]._id,
        tenantId: "TENANT_001",
        version: 1,
        content: {
            logic: "ALL",
            nodes: rules[1].nodes,
            limits: rules[1].limits
        },
        status: "ACTIVE",
        publishedAt: pastTimestamp(15),
        createdAt: pastTimestamp(15),
        createdBy: "admin"
    }
];

db.rule_versions.insertMany(ruleVersions);
print(`Created ${ruleVersions.length} rule versions`);

// ===========================
// 5. Create Temporal Policies
// ===========================
print("\nCreating temporal policies...");
const temporalPolicies = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "Standard Business Hours",
        type: "RECURRING",
        schedule: {
            daysOfWeek: ["MON", "TUE", "WED", "THU", "FRI"],
            startTime: "09:00:00",
            endTime: "18:00:00",
            timezone: "Asia/Ho_Chi_Minh"
        },
        active: true,
        createdAt: pastTimestamp(30),
        updatedAt: pastTimestamp(30)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "Weekend Special",
        type: "RECURRING",
        schedule: {
            daysOfWeek: ["SAT", "SUN"],
            startTime: "00:00:00",
            endTime: "23:59:59",
            timezone: "Asia/Ho_Chi_Minh"
        },
        active: true,
        createdAt: pastTimestamp(25),
        updatedAt: pastTimestamp(25)
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        name: "Holiday Season 2025",
        type: "FIXED",
        schedule: {
            startDate: "2025-12-20T00:00:00Z",
            endDate: "2025-12-31T23:59:59Z"
        },
        active: false,
        createdAt: pastTimestamp(20),
        updatedAt: pastTimestamp(20)
    }
];

db.temporal_policies.insertMany(temporalPolicies);
print(`Created ${temporalPolicies.length} temporal policies`);

// ===========================
// 6. Create Assignments (Rule to Campaign/Promotion mapping)
// ===========================
print("\nCreating assignments...");
const assignments = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[0]._id,
        targetType: "CAMPAIGN",
        targetId: "CAMP_2025_001",
        priority: 1,
        active: true,
        effectiveFrom: pastTimestamp(10),
        effectiveTo: new Date("2025-12-31"),
        metadata: {
            campaignName: "New Year 2025 Campaign",
            description: "Customer eligibility for NY campaign"
        },
        createdAt: pastTimestamp(10),
        createdBy: "admin",
        updatedAt: pastTimestamp(10),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[1]._id,
        targetType: "CAMPAIGN",
        targetId: "CAMP_2025_001",
        priority: 2,
        active: true,
        effectiveFrom: pastTimestamp(10),
        effectiveTo: new Date("2025-12-31"),
        metadata: {
            campaignName: "New Year 2025 Campaign",
            description: "Order validation for NY campaign"
        },
        createdAt: pastTimestamp(10),
        createdBy: "admin",
        updatedAt: pastTimestamp(10),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[2]._id,
        targetType: "PROMOTION",
        targetId: "PROMO_FLASH_001",
        priority: 1,
        active: true,
        effectiveFrom: currentTimestamp(),
        effectiveTo: new Date("2025-06-30"),
        metadata: {
            promotionName: "Flash Sale Q1-Q2 2025",
            description: "Time window check for flash sale"
        },
        createdAt: pastTimestamp(5),
        createdBy: "admin",
        updatedAt: pastTimestamp(5),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[3]._id,
        targetType: "PROMOTION",
        targetId: "PROMO_REGIONAL_001",
        priority: 3,
        active: true,
        effectiveFrom: currentTimestamp(),
        effectiveTo: new Date("2025-12-31"),
        metadata: {
            promotionName: "Regional Promotion 2025",
            description: "Location eligibility check"
        },
        createdAt: pastTimestamp(8),
        createdBy: "admin",
        updatedAt: pastTimestamp(8),
        updatedBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[5]._id,
        targetType: "CAMPAIGN",
        targetId: "CAMP_LOYALTY_001",
        priority: 1,
        active: true,
        effectiveFrom: pastTimestamp(7),
        effectiveTo: new Date("2025-12-31"),
        metadata: {
            campaignName: "Loyalty Rewards 2025",
            description: "Budget check for loyalty campaign"
        },
        createdAt: pastTimestamp(7),
        createdBy: "admin",
        updatedAt: pastTimestamp(7),
        updatedBy: "admin"
    }
];

db.assignments.insertMany(assignments);
print(`Created ${assignments.length} assignments`);

// ===========================
// 7. Create Publish Jobs
// ===========================
print("\nCreating publish jobs...");
const publishJobs = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[0]._id,
        version: 2,
        status: "COMPLETED",
        bundleId: "BUNDLE_001_v2",
        startedAt: pastTimestamp(5),
        completedAt: pastTimestamp(5),
        metadata: {
            compilationTime: 1250,
            deploymentTargets: ["prod-cluster-1", "prod-cluster-2"],
            success: true
        },
        createdAt: pastTimestamp(5),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[1]._id,
        version: 1,
        status: "COMPLETED",
        bundleId: "BUNDLE_002_v1",
        startedAt: pastTimestamp(15),
        completedAt: pastTimestamp(15),
        metadata: {
            compilationTime: 980,
            deploymentTargets: ["prod-cluster-1", "prod-cluster-2"],
            success: true
        },
        createdAt: pastTimestamp(15),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[2]._id,
        version: 1,
        status: "COMPLETED",
        bundleId: "BUNDLE_003_v1",
        startedAt: pastTimestamp(10),
        completedAt: pastTimestamp(10),
        metadata: {
            compilationTime: 750,
            deploymentTargets: ["prod-cluster-1"],
            success: true
        },
        createdAt: pastTimestamp(10),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[4]._id,
        version: 1,
        status: "PENDING",
        bundleId: null,
        startedAt: null,
        completedAt: null,
        metadata: {
            scheduledFor: new Date("2025-02-01T00:00:00Z"),
            retryCount: 0
        },
        createdAt: currentTimestamp(),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[5]._id,
        version: 1,
        status: "FAILED",
        bundleId: null,
        startedAt: pastTimestamp(3),
        completedAt: pastTimestamp(3),
        error: {
            code: "COMPILATION_ERROR",
            message: "Failed to compile rule: invalid operator reference",
            details: "Operator 'custom_check' not found in registry"
        },
        metadata: {
            retryCount: 3,
            lastRetryAt: pastTimestamp(2)
        },
        createdAt: pastTimestamp(3),
        createdBy: "admin"
    }
];

db.publish_jobs.insertMany(publishJobs);
print(`Created ${publishJobs.length} publish jobs`);

// ===========================
// 8. Create Audit Logs
// ===========================
print("\nCreating audit logs...");
const auditLogs = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "RULE",
        entityId: rules[0]._id,
        action: "CREATE",
        userId: "admin",
        timestamp: pastTimestamp(20),
        changes: {
            before: null,
            after: {
                code: "RULE_CUSTOMER_ELIGIBILITY",
                name: "Customer Eligibility Check",
                state: "DRAFT"
            }
        },
        metadata: {
            ip: "192.168.1.100",
            userAgent: "Mozilla/5.0"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "RULE",
        entityId: rules[0]._id,
        action: "UPDATE",
        userId: "admin",
        timestamp: pastTimestamp(15),
        changes: {
            before: { state: "DRAFT", version: 1 },
            after: { state: "PUBLISHED", version: 2 }
        },
        metadata: {
            ip: "192.168.1.100",
            userAgent: "Mozilla/5.0"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "OPERATOR",
        entityId: operators[0]._id,
        action: "CREATE",
        userId: "system",
        timestamp: pastTimestamp(30),
        changes: {
            before: null,
            after: {
                name: "equals",
                version: 1,
                status: "ACTIVE"
            }
        },
        metadata: {
            source: "system_init"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "ASSIGNMENT",
        entityId: assignments[0]._id,
        action: "CREATE",
        userId: "admin",
        timestamp: pastTimestamp(10),
        changes: {
            before: null,
            after: {
                ruleId: rules[0]._id,
                targetType: "CAMPAIGN",
                targetId: "CAMP_2025_001",
                active: true
            }
        },
        metadata: {
            ip: "192.168.1.101",
            userAgent: "Mozilla/5.0"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "PUBLISH_JOB",
        entityId: publishJobs[0]._id,
        action: "COMPLETE",
        userId: "system",
        timestamp: pastTimestamp(5),
        changes: {
            before: { status: "IN_PROGRESS" },
            after: { status: "COMPLETED", bundleId: "BUNDLE_001_v2" }
        },
        metadata: {
            duration: 1250,
            source: "deployment_pipeline"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "RULE",
        entityId: rules[7]._id,
        action: "ARCHIVE",
        userId: "admin",
        timestamp: pastTimestamp(15),
        changes: {
            before: { state: "PUBLISHED" },
            after: { state: "ARCHIVED" }
        },
        metadata: {
            reason: "Replaced with new payment validation logic",
            ip: "192.168.1.102"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "TEMPORAL_POLICY",
        entityId: temporalPolicies[0]._id,
        action: "CREATE",
        userId: "admin",
        timestamp: pastTimestamp(30),
        changes: {
            before: null,
            after: {
                name: "Standard Business Hours",
                type: "RECURRING",
                active: true
            }
        },
        metadata: {
            ip: "192.168.1.100"
        }
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        entityType: "RULE",
        entityId: rules[5]._id,
        action: "VALIDATION_FAILED",
        userId: "system",
        timestamp: pastTimestamp(2),
        changes: {
            validationErrors: [
                {
                    field: "nodes[0].children[0].params.field",
                    error: "Field 'promotion.usedBudget' not found in schema"
                }
            ]
        },
        metadata: {
            source: "validation_engine",
            severity: "ERROR"
        }
    }
];

db.audit_logs.insertMany(auditLogs);
print(`Created ${auditLogs.length} audit logs`);

// ===========================
// 9. Create Validation Rule Instances (Runtime)
// ===========================
print("\nCreating validation rule instances...");
const validationRules = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[0]._id,
        ruleVersion: 2,
        bundleId: "BUNDLE_001_v2",
        campaignId: "CAMP_2025_001",
        priority: 1,
        status: "ACTIVE",
        metadata: {
            compiledAt: pastTimestamp(5),
            lastExecuted: currentTimestamp(),
            executionCount: 1245,
            successRate: 0.78
        },
        createdAt: pastTimestamp(5),
        updatedAt: currentTimestamp()
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[1]._id,
        ruleVersion: 1,
        bundleId: "BUNDLE_002_v1",
        campaignId: "CAMP_2025_001",
        priority: 2,
        status: "ACTIVE",
        metadata: {
            compiledAt: pastTimestamp(15),
            lastExecuted: currentTimestamp(),
            executionCount: 892,
            successRate: 0.92
        },
        createdAt: pastTimestamp(15),
        updatedAt: currentTimestamp()
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[2]._id,
        ruleVersion: 1,
        bundleId: "BUNDLE_003_v1",
        promotionId: "PROMO_FLASH_001",
        priority: 1,
        status: "ACTIVE",
        metadata: {
            compiledAt: pastTimestamp(10),
            lastExecuted: currentTimestamp(),
            executionCount: 3421,
            successRate: 0.65
        },
        createdAt: pastTimestamp(10),
        updatedAt: currentTimestamp()
    }
];

db.validation_rules.insertMany(validationRules);
print(`Created ${validationRules.length} validation rule instances`);

// ===========================
// 10. Create Rule Temporal Links
// ===========================
print("\nCreating rule temporal links...");
const ruleTemporalLinks = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[0]._id,
        temporalPolicyId: temporalPolicies[0]._id,
        active: true,
        createdAt: pastTimestamp(10),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[2]._id,
        temporalPolicyId: temporalPolicies[1]._id,
        active: true,
        createdAt: pastTimestamp(8),
        createdBy: "admin"
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        ruleId: rules[3]._id,
        temporalPolicyId: temporalPolicies[0]._id,
        active: false,
        createdAt: pastTimestamp(8),
        createdBy: "admin"
    }
];

db.rule_temporal_links.insertMany(ruleTemporalLinks);
print(`Created ${ruleTemporalLinks.length} rule temporal links`);

// ===========================
// 11. Create Outbox Events (for event-driven architecture)
// ===========================
print("\nCreating outbox events...");
const outboxEvents = [
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        aggregateId: rules[0]._id,
        aggregateType: "RULE",
        eventType: "RULE_PUBLISHED",
        payload: {
            ruleId: rules[0]._id,
            ruleCode: "RULE_CUSTOMER_ELIGIBILITY",
            version: 2,
            publishedAt: pastTimestamp(5)
        },
        status: "PROCESSED",
        createdAt: pastTimestamp(5),
        processedAt: pastTimestamp(5),
        retryCount: 0
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        aggregateId: assignments[0]._id,
        aggregateType: "ASSIGNMENT",
        eventType: "ASSIGNMENT_CREATED",
        payload: {
            assignmentId: assignments[0]._id,
            ruleId: rules[0]._id,
            targetType: "CAMPAIGN",
            targetId: "CAMP_2025_001"
        },
        status: "PROCESSED",
        createdAt: pastTimestamp(10),
        processedAt: pastTimestamp(10),
        retryCount: 0
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        aggregateId: publishJobs[4]._id,
        aggregateType: "PUBLISH_JOB",
        eventType: "PUBLISH_JOB_FAILED",
        payload: {
            jobId: publishJobs[4]._id,
            ruleId: rules[5]._id,
            error: "COMPILATION_ERROR",
            failedAt: pastTimestamp(3)
        },
        status: "PENDING",
        createdAt: pastTimestamp(3),
        processedAt: null,
        retryCount: 2
    },
    {
        _id: generateId(),
        tenantId: "TENANT_001",
        aggregateId: rules[7]._id,
        aggregateType: "RULE",
        eventType: "RULE_ARCHIVED",
        payload: {
            ruleId: rules[7]._id,
            ruleCode: "RULE_PAYMENT_METHOD",
            reason: "Replaced with new logic",
            archivedAt: pastTimestamp(15)
        },
        status: "PROCESSED",
        createdAt: pastTimestamp(15),
        processedAt: pastTimestamp(15),
        retryCount: 0
    }
];

db.outbox_events.insertMany(outboxEvents);
print(`Created ${outboxEvents.length} outbox events`);

// ===========================
// Create Indexes
// ===========================
print("\nCreating indexes...");

// Rules indexes
db.rules.createIndex({ "tenantId": 1, "code": 1 }, { unique: true });
db.rules.createIndex({ "tenantId": 1, "state": 1, "updatedAt": -1 });
db.rules.createIndex({ "tenantId": 1, "createdAt": -1 });

// Operators indexes
db.operators.createIndex({ "tenantId": 1, "name": 1, "version": -1 }, { unique: true });
db.operators.createIndex({ "tenantId": 1, "context": 1, "status": 1 });

// Rule versions indexes
db.rule_versions.createIndex({ "ruleId": 1, "version": -1 });
db.rule_versions.createIndex({ "tenantId": 1, "status": 1 });

// Assignments indexes
db.assignments.createIndex({ "tenantId": 1, "ruleId": 1 });
db.assignments.createIndex({ "tenantId": 1, "targetType": 1, "targetId": 1 });
db.assignments.createIndex({ "active": 1, "effectiveFrom": 1, "effectiveTo": 1 });

// Audit logs indexes
db.audit_logs.createIndex({ "tenantId": 1, "timestamp": -1 });
db.audit_logs.createIndex({ "entityType": 1, "entityId": 1 });
db.audit_logs.createIndex({ "userId": 1, "timestamp": -1 });

// Outbox events indexes
db.outbox_events.createIndex({ "status": 1, "createdAt": 1 });
db.outbox_events.createIndex({ "aggregateType": 1, "aggregateId": 1 });

// Publish jobs indexes
db.publish_jobs.createIndex({ "tenantId": 1, "ruleId": 1 });
db.publish_jobs.createIndex({ "status": 1, "createdAt": -1 });

// Temporal policies indexes
db.temporal_policies.createIndex({ "tenantId": 1, "active": 1 });

// Validation rules indexes
db.validation_rules.createIndex({ "tenantId": 1, "status": 1 });
db.validation_rules.createIndex({ "campaignId": 1 });
db.validation_rules.createIndex({ "promotionId": 1 });

print("Index creation completed!");

// ===========================
// Display Summary
// ===========================
print("\n========================================");
print("Sample Data Creation Summary");
print("========================================");
print(`Database: validation_db`);
print(`Collections created:`);
print(`  - rules: ${db.rules.countDocuments()} documents`);
print(`  - operators: ${db.operators.countDocuments()} documents`);
print(`  - reason_codes: ${db.reason_codes.countDocuments()} documents`);
print(`  - rule_versions: ${db.rule_versions.countDocuments()} documents`);
print(`  - temporal_policies: ${db.temporal_policies.countDocuments()} documents`);
print(`  - assignments: ${db.assignments.countDocuments()} documents`);
print(`  - publish_jobs: ${db.publish_jobs.countDocuments()} documents`);
print(`  - audit_logs: ${db.audit_logs.countDocuments()} documents`);
print(`  - validation_rules: ${db.validation_rules.countDocuments()} documents`);
print(`  - rule_temporal_links: ${db.rule_temporal_links.countDocuments()} documents`);
print(`  - outbox_events: ${db.outbox_events.countDocuments()} documents`);
print("========================================");
print("Sample data creation completed successfully!");
print("\nTo use this data:");
print("1. Ensure MongoDB is running on localhost:27017");
print("2. Run: mongosh localhost:27017/validation_db < mongo-sample-data.js");
print("3. Or if using authentication: mongosh -u username -p password localhost:27017/validation_db < mongo-sample-data.js");