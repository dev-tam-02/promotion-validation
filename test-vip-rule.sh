#!/bin/bash

# Test VIP Segment Validation Rule
# Rule ID: 01932b6f-0005-7000-8000-000000000010

echo "=========================================="
echo "Testing VIP Segment Validation Rule"
echo "=========================================="
echo ""

BASE_URL="http://localhost:16014"
RULE_ID="01932b6f-0005-7000-8000-000000000010"

# 1. Check if service is healthy
echo "1. Checking service health..."
curl -s "$BASE_URL/actuator/health" | python3 -m json.tool
echo ""

# 2. Get rule details
echo "2. Getting rule details..."
curl -s "$BASE_URL/promotion/promotion-validation/v1/rules/$RULE_ID" | python3 -m json.tool
echo ""

# 3. Skip node endpoint (not available)
echo "3. Nodes are embedded in rule details above"
echo ""

# 4. Verify no missing child references
echo "4. Verifying node structure..."
NODES_JSON=$(curl -s "$BASE_URL/promotion/promotion-validation/v1/rules/$RULE_ID")
echo "$NODES_JSON" | python3 << 'EOF'
import json
import sys

try:
    response = json.load(sys.stdin)

    # Extract nodes from response structure
    if "data" in response and "nodes" in response["data"]:
        nodes = response["data"]["nodes"]
    elif isinstance(response, list):
        nodes = response
    else:
        print("✗ Unexpected response format")
        sys.exit(1)

    # Build node map
    node_map = {node.get("nodeId") or node.get("id"): node for node in nodes}

    # Verify all children exist
    errors = []
    for node in nodes:
        node_id = node.get("nodeId") or node.get("id")
        if node.get("type") == "GROUP" and node.get("children"):
            for child_id in node.get("children", []):
                if child_id not in node_map:
                    errors.append(f"Node {node_id} references non-existent child: {child_id}")

    if errors:
        print("✗ Found node reference errors:")
        for error in errors:
            print(f"  - {error}")
        sys.exit(1)
    else:
        print("✓ All node references are valid!")
        print(f"✓ Total nodes: {len(nodes)}")
        for node in nodes:
            node_id = node.get("nodeId") or node.get("id")
            node_type = node.get("type")
            if node_type == "GROUP":
                children = node.get("children", [])
                print(f"  - Node {node_id} ({node_type}) -> children: {children}")
            else:
                operator = node.get("operatorName", "N/A")
                print(f"  - Node {node_id} ({node_type}) -> operator: {operator}")

except Exception as e:
    print(f"✗ Error: {e}")
    sys.exit(1)
EOF

echo ""
echo "=========================================="
echo "Test completed!"
echo "=========================================="
