#!/usr/bin/env bash
# In danh sách file Java đang thay đổi (staged + unstaged + file mới chưa track) và dải dòng mới.
# Dùng: bash .claude/skills/sonar-precommit/scripts/changed-lines.sh [base-ref]
#   base-ref mặc định HEAD (so working tree với commit hiện tại).
set -euo pipefail
BASE="${1:-HEAD}"

{
  git diff --name-only --diff-filter=AMR "$BASE" -- '*.java'
  git ls-files --others --exclude-standard -- '*.java'
} | sort -u | while read -r f; do
  [ -f "$f" ] || continue
  if git ls-files --error-unmatch "$f" >/dev/null 2>&1; then
    ranges=$(git diff -U0 "$BASE" -- "$f" | grep -E '^@@' \
      | sed -E 's/^@@ -[0-9,]+ \+([0-9]+)(,([0-9]+))? @@.*/\1 \3/' \
      | awk '{n=($2==""?1:$2); if(n>0) printf "%d-%d ", $1, $1+n-1}')
    echo "$f: ${ranges:-(chỉ xoá dòng)}"
  else
    echo "$f: (file mới — rà toàn bộ)"
  fi
done
