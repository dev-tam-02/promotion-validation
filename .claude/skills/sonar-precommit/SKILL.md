---
name: sonar-precommit
description: >
  Rà code Java vừa sửa theo bộ rule Sonar mà bot promix_gitlab_bot đang quét trên nhánh staging
  (Bitbucket PROMOTION/promotion-*), sửa ngay trước khi commit để không phát sinh issue mới.
  Kích hoạt khi: chuẩn bị commit, "rà sonar", "check sonar trước commit", "sonar precommit",
  "fix sonar theo bot", hoặc khi CLAUDE.md của repo yêu cầu rà trước commit.
  Chỉ rà PHẦN CODE ĐANG THAY ĐỔI (git diff), không quét lại cả repo. Không đổi hành vi.
argument-hint: "[--all để rà cả file đã chạm, mặc định chỉ dòng thay đổi]"
allowed-tools: Bash(git *) Bash(bash *) Bash(mvn *) Bash(grep *) Read Edit Grep Glob
---

# Sonar pre-commit — rà theo rule của bot staging

Mục tiêu: code commit lên `release` → đồng bộ sang `staging` **không phát sinh issue Sonar mới**.
Bot `/sonar` trên staging dùng bộ rule riêng (có cả rule bộ "Sonar way" mặc định không bật, ví dụ S135),
nên **bám danh sách trong `references/rules.md`**, không dựa vào Sonar server khác.

## Nguyên tắc (bắt buộc)

1. **Không đổi hành vi.** Mỗi chỗ sửa phải cho kết quả runtime giống hệt: giá trị trả về, thứ tự gọi,
   exception, log, message lỗi, payload API, thứ tự side-effect. Nghi ngờ → không sửa, ghi vào báo cáo.
2. **Chỉ rà phần đang thay đổi**: dòng thêm/sửa trong `git diff` (staged + unstaged). Code cũ không chạm
   tới thì không sửa — tránh diff lan rộng, khó review, dễ conflict khi đồng bộ sang staging.
3. **Cấm**: `@SuppressWarnings` / `// NOSONAR` để né rule, đổi test cho qua, đổi tên API public,
   đổi chữ ký method public mà không sửa mọi nơi gọi.
4. Không tự commit / push. Skill chỉ rà + sửa + báo; commit do người dùng hoặc quy trình commit của repo.

## Quy trình

### B1. Lấy phạm vi rà
```bash
bash .claude/skills/sonar-precommit/scripts/changed-lines.sh          # file + dải dòng thay đổi
bash .claude/skills/sonar-precommit/scripts/suspect.sh                # dòng nghi vấn theo rule (lọc thô)
```
`suspect.sh` chỉ là **lọc thô bằng grep** (bắt nhanh S108, S125, S1612, S1710, S1130, S1075, S3358…).
Rule cần hiểu ngữ nghĩa (S3776, S135, S1192, S1068, S1172, S1144, S1128, S1117, S107, S1452, S1141)
phải **đọc code** từng file trong phạm vi B1.

### B2. Đối chiếu từng rule
Mở `references/rules.md`. Với mỗi file Java trong phạm vi: đi qua **toàn bộ** bảng rule, ghi lại
vi phạm (file:dòng, rule). Ưu tiên theo mức độ: CRITICAL (S3776, S1192, S1452) → MAJOR → MINOR.

### B3. Sửa
Sửa theo mục "Cách sửa an toàn" của từng rule. Sau mỗi file: tự kiểm lại điều kiện/nhánh/thứ tự
đúng như cũ (đặc biệt khi tách method, bỏ `break`/`continue`, tách ternary).

### B4. Kiểm
```bash
mvn -q -o compile test-compile          # bỏ -o nếu thiếu dependency
mvn -q -o test -Dtest='<Test liên quan>' # chạy test của class vừa sửa (hoặc cả module nếu nhanh)
```
Test fail mà trước khi sửa vẫn pass → hoàn tác chỗ sửa gây fail, ghi báo cáo.

### B5. Báo cáo (trả lời trực tiếp)
| File:dòng | Rule | Đã sửa / Bỏ qua | Lý do (nếu bỏ qua) |
Kèm: kết quả compile/test, và các vi phạm **không sửa** vì rủi ro đổi hành vi (để người duyệt quyết).

## Tham khảo
- `references/rules.md` — 19 rule bot đang bắt + cách nhận diện + cách sửa an toàn + bẫy hay gặp.
- Nguồn danh sách rule: kết quả `/sonar` của bot ngày 26/10/2026 cho promotion-vtm-bff (83),
  promotion-campaign (31), promotion-rule-engine (14), promotion-validation (6). Bot bắt thêm rule mới
  → bổ sung vào `rules.md`.
