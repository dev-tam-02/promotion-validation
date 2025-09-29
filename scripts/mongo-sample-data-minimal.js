// MongoDB Minimal Sample Data Script for Validation Module
// Quick test data for development
// Run: mongosh localhost:27017/validation_db < mongo-sample-data-minimal.js

db = db.getSiblingDB('validation_db');

// Clear existing collections
print("Clearing existing collections...");
db.rules.drop();
db.operators.drop();
db.reason_codes.drop();

// Create minimal reason codes
print("\nCreating reason codes...");
db.reason_codes.insertMany([
    {
        _id: "RC001",
        tenantId: "TENANT_001",
        category: "ELIGIBILITY",
        severity: "ERROR",
        labels: {
            "vi": "Không đủ điều kiện",
            "en": "Not eligible"
        },
        createdAt: new Date()
    },
    {
        _id: "RC002",
        tenantId: "TENANT_001",
        category: "LIMIT",
        severity: "WARN",
        labels: {
            "vi": "Giới hạn",
            "en": "Limit reached"
        },
        createdAt: new Date()
    }
]);

// Create basic operators
print("Creating operators...");
db.operators.insertMany([
    {
        _id: ObjectId().toString(),
        tenantId: "TENANT_001",
        name: "equals",
        version: 1,
        context: "general",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: ["string", "number", "boolean"] }
            }
        },
        compilerId: "COMP_EQ_001",
        status: "ACTIVE",
        createdAt: new Date(),
        updatedAt: new Date()
    },
    {
        _id: ObjectId().toString(),
        tenantId: "TENANT_001",
        name: "greater_than",
        version: 1,
        context: "numeric",
        jsonSchema: {
            type: "object",
            properties: {
                field: { type: "string" },
                value: { type: "number" }
            }
        },
        compilerId: "COMP_GT_001",
        status: "ACTIVE",
        createdAt: new Date(),
        updatedAt: new Date()
    }
]);

// Create simple validation rule
print("Creating validation rules...");
db.rules.insertOne({
    _id: ObjectId().toString(),
    tenantId: "TENANT_001",
    code: "TEST_RULE",
    name: "Test Validation Rule",
    state: "PUBLISHED",
    latestVersion: 1,
    logic: "ALL",
    limits: {
        maxUsage: 100
    },
    nodes: [
        {
            id: "node_1",
            type: "COND",
            operatorName: "equals",
            params: {
                field: "status",
                value: "ACTIVE"
            },
            reasonCode: "RC001"
        }
    ],
    notes: "Simple test rule",
    createdAt: new Date(),
    createdBy: "admin",
    updatedAt: new Date(),
    updatedBy: "admin"
});

print("\n========================================");
print("Minimal sample data created successfully!");
print(`- reason_codes: ${db.reason_codes.countDocuments()} documents`);
print(`- operators: ${db.operators.countDocuments()} documents`);
print(`- rules: ${db.rules.countDocuments()} documents`);
print("========================================");