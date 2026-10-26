#!/usr/bin/env bash
# Lọc THÔ các dòng mới (trong git diff) có dấu hiệu vi phạm rule dễ nhận bằng grep.
# Chỉ là gợi ý để đọc kỹ — không phải kết luận. Rule cần hiểu ngữ nghĩa phải đọc code (xem SKILL.md).
# Dùng: bash .claude/skills/sonar-precommit/scripts/suspect.sh [base-ref]
set -uo pipefail
BASE="${1:-HEAD}"

check() { # $1=rule  $2=regex (ERE)  $3=mô tả
  local hits
  hits=$(grep -nE -e "$2" <<<"$ADDED" | sed -E "s/^[0-9]+://" || true)
  [ -n "$hits" ] && { echo "### $1 — $3"; echo "$hits"; echo; }
}

# Dòng thêm mới kèm "file:dòng" — từ diff với BASE và file chưa track
ADDED=$( {
  git diff -U0 "$BASE" -- '*.java' | awk '
    /^\+\+\+ b\// {f=substr($0,7); next}
    /^@@/ {match($0,/\+[0-9]+/); n=substr($0,RSTART+1,RLENGTH-1)+0; next}
    /^\+/ && !/^\+\+\+/ {print f ":" n ": " substr($0,2); n++; next}
    /^ / {n++}'
  git ls-files --others --exclude-standard -- '*.java' | while read -r f; do
    awk -v f="$f" '{print f ":" NR ": " $0}' "$f"
  done
} )

[ -z "$ADDED" ] && { echo "Không có dòng Java mới nào để rà."; exit 0; }

check S108  '(catch\s*\([^)]*\)|else|\)|try|finally|do)\s*\{\s*\}\s*$' 'khối rỗng {} (catch/if/else) — thêm comment hoặc báo nếu thiếu logic'
check S125  '^[^:]+:[0-9]+:\s*//\s*[A-Za-z_][A-Za-z0-9_.]*(\(.*\)|\s*=.*);\s*$' 'nghi code bị comment'
check S1612 '[(, ]([a-z][A-Za-z0-9]*)\s*->\s*\1\.[a-zA-Z0-9]+\(\)\s*[),]' 'lambda x -> x.method() (đổi sang method reference?)'
check S1612 '([a-z][A-Za-z0-9]*)\s*->\s*\1\s+instanceof\s+[A-Z][A-Za-z0-9]*\s*[),]'    'lambda x -> x instanceof Foo (đổi sang Foo.class::isInstance)'
check S1710 '@ApiResponses\s*\('                       'bọc @ApiResponses — viết lặp @ApiResponse'
check S1130 'throws\s+Exception\s*\{'                   'throws Exception — kiểm thân method có ném thật không'
check S3358 '\?[^;:]*:[^;]*\?[^;]*:'                    'nghi ternary lồng'
check S3358 ':[0-9]+:\s*:\s[^;]*\?[^;]*:'              'nghi ternary lồng (dòng tiếp nối bắt đầu bằng ":")'
check S1075 '"(https?://|/api/|/v[0-9]+/)[^"]*"'        'URI viết cứng (không tính path trong @RequestMapping)'
check S1452 '(public|protected)\s+[A-Za-z]+<\?[^>]*>\s+[a-z]' 'kiểu trả về dùng wildcard <?>'
check S1874 '@Deprecated'                               'chạm API deprecated — kiểm nơi gọi'

echo "-- Hết lọc thô. Rule S3776, S135, S1192, S1068, S1172, S1144, S1128, S1117, S107, S1141 cần đọc code."
