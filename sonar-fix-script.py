#!/usr/bin/env python3
"""
Bulk SonarQube issue fixer for validation module
Systematically fixes common SonarQube issues
"""

import re
import os
from pathlib import Path

BASE_PATH = Path("/Users/hoanglam/promix/validation/src/main/java")

def fix_file(filepath, fixes):
    """Apply fixes to a file"""
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        original = content
        for pattern, replacement, description in fixes:
            content = re.sub(pattern, replacement, content, flags=re.MULTILINE)

        if content != original:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(content)
            return True
        return False
    except Exception as e:
        print(f"Error fixing {filepath}: {e}")
        return False

# Fix S1858 - Remove redundant toString() calls
def fix_s1858_redundant_tostring():
    """Fix redundant toString() calls"""
    files = [
        "vn/viettel/vds/promotion/validation/adapter/out/messaging/ValidationEventPublisher.java"
    ]

    fixes = [
        (r'\.getType\(\)\.toString\(\)', '.getType()', 'Remove redundant toString() on getType()'),
    ]

    for file in files:
        filepath = BASE_PATH / file
        if filepath.exists():
            if fix_file(filepath, fixes):
                print(f"Fixed S1858 in {file}")

# Fix S6204 - Replace Stream.collect(Collectors.toList()) with Stream.toList()
def fix_s6204_stream_tolist():
    """Replace Stream.collect(Collectors.toList()) with Stream.toList()"""
    files = [
        "vn/viettel/vds/promotion/validation/application/fact/mapper/LimitsFactMapper.java"
    ]

    fixes = [
        (r'\.collect\(Collectors\.toList\(\)\)', '.toList()', 'Replace collect(toList()) with toList()'),
    ]

    for file in files:
        filepath = BASE_PATH / file
        if filepath.exists():
            if fix_file(filepath, fixes):
                print(f"Fixed S6204 in {file}")

# Fix S1168 - Return empty collections instead of null
def fix_s1168_empty_collections():
    """Return empty collections instead of null"""
    files = [
        "vn/viettel/vds/promotion/validation/adapter/out/integration/dto/WarmupResponse.java",
        "vn/viettel/vds/promotion/validation/application/fact/mapper/CustomerFactMapper.java"
    ]

    fixes = [
        (r'return null;(\s*//.*attributes.*)', r'return Map.of();\1', 'Return empty map'),
        (r'return null;(\s*//.*Tags.*)', r'return List.of();\1', 'Return empty list'),
    ]

    for file in files:
        filepath = BASE_PATH / file
        if filepath.exists():
            if fix_file(filepath, fixes):
                print(f"Fixed S1168 in {file}")

# Fix S1126 - Replace if-then-else with single return
def fix_s1126_simplify_boolean_return():
    """Simplify boolean returns"""
    file = "vn/viettel/vds/promotion/validation/application/fact/policy/FetchPolicyRules.java"

    fixes = [
        (r'if\s*\((.*?)\)\s*\{\s*return true;\s*\}\s*else\s*\{\s*return false;\s*\}',
         r'return \1;',
         'Simplify boolean return'),
    ]

    filepath = BASE_PATH / file
    if filepath.exists():
        if fix_file(filepath, fixes):
            print(f"Fixed S1126 in {file}")

# Fix S1125 - Remove unnecessary boolean literals
def fix_s1125_boolean_literal():
    """Remove unnecessary boolean literals"""
    file = "vn/viettel/vds/promotion/validation/application/service/OperatorService.java"

    fixes = [
        (r'== true', '', 'Remove == true'),
        (r'== false', '', 'Remove == false'),
    ]

    filepath = BASE_PATH / file
    if filepath.exists():
        if fix_file(filepath, fixes):
            print(f"Fixed S1125 in {file}")

if __name__ == "__main__":
    print("Starting SonarQube fixes...")
    fix_s1858_redundant_tostring()
    fix_s6204_stream_tolist()
    fix_s1168_empty_collections()
    fix_s1126_simplify_boolean_return()
    fix_s1125_boolean_literal()
    print("Done!")
