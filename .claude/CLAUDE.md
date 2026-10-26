# Quy ước cho AI khi làm việc trong repo này

## Rà Sonar trước khi commit (bắt buộc)
Code của repo được đồng bộ sang nhánh `staging` (Bitbucket PROMOTION) và bị bot `/sonar` quét với bộ rule riêng.
Để không phát sinh issue mới:

- **Trước mỗi lần `git commit`** có thay đổi file `.java`, chạy skill **`sonar-precommit`**
  (`.claude/skills/sonar-precommit/SKILL.md`): rà phần code đang thay đổi theo `references/rules.md`,
  sửa vi phạm **không đổi hành vi**, compile + chạy test liên quan, rồi mới commit.
- Không dùng `@SuppressWarnings` / `// NOSONAR` để né rule.
- Vi phạm nào không sửa được an toàn → ghi rõ trong báo cáo / mô tả commit để người duyệt quyết.
