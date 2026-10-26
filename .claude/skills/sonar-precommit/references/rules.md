# Rule Sonar bot staging đang bắt (Java)

Thứ tự theo mức độ. Cột "Lần gặp" = số issue trong đợt quét 26/10/2026 (4 service), để biết rule nào hay vướng.

| Rule | Mức | Lần gặp | Nội dung |
|---|---|---|---|
| S3776 | CRITICAL | 12 | Cognitive Complexity của method > 15 |
| S1192 | CRITICAL | 12 | Chuỗi literal lặp ≥ 3 lần (hoặc trùng giá trị hằng đã có) |
| S1452 | CRITICAL | 2 | Kiểu trả về dùng wildcard generic (`List<?>`, `ResponseEntity<?>`) |
| S135 | MINOR | 20 | Vòng lặp có > 1 `break`/`continue` |
| S3358 | MAJOR | 16 | Ternary lồng nhau |
| S1612 | MINOR | 27 | Lambda thay được bằng method reference |
| S1710 | MINOR | 6 | Bọc `@ApiResponses({...})` khi annotation lặp được |
| S1141 | MAJOR | 5 | `try` lồng trong `try` |
| S1144 | MAJOR | 5 | Method/constructor private không dùng |
| S1068 | MAJOR | 5 | Field private không dùng |
| S1117 | MAJOR | 4 | Biến cục bộ trùng tên field (che field) |
| S1172 | MAJOR | 4 | Tham số method không dùng |
| S107 | MAJOR | 3 | Method/constructor > 7 tham số |
| S125 | MAJOR | 3 | Khối code bị comment |
| S1128 | MINOR | 5 | Import không dùng |
| S108 | MAJOR | 2 | Khối `{}` rỗng không có giải thích |
| S1075 | MINOR | 1 | URI/đường dẫn viết cứng |
| S1874 | MINOR | 1 | Dùng API `@Deprecated` |
| S1130 | MINOR | 1 | `throws Exception` khai báo nhưng thân method không ném |

Áp cho **cả `src/main` lẫn `src/test`** — bot tính issue trong test (44/134 issue đợt 26/10 nằm ở test).

---

## S3776 — Cognitive Complexity > 15
**Nhận diện:** method nhiều `if/else`, vòng lặp lồng, `&&`/`||` chuỗi dài, ternary, `catch`, `break/continue`.
Mỗi cấu trúc +1, lồng thêm +1 mỗi tầng.
**Sửa an toàn:**
- Tách khối logic thành method private có tên rõ (truyền tham số tường minh, không đổi thứ tự gọi).
- Early return / guard clause thay `if` lồng — **giữ nguyên thứ tự kiểm tra**.
- Gom điều kiện dài vào method boolean (`isEligible(x)`).
**Bẫy:** tách method trong `@Transactional` — gọi nội bộ không qua proxy, nhưng vì vẫn trong cùng
transaction ngoài nên hành vi giữ nguyên; **không** đặt `@Transactional` riêng lên method tách ra.
Biến được gán trong khối tách ra mà dùng phía sau → trả về giá trị, không dùng field tạm.

## S1192 — Literal lặp
**Nhận diện:** cùng chuỗi (`"campaignId"`, `"productId"`, `"không được để trống"`, `"REVOKED"`…) ≥ 3 lần
trong một file; hoặc literal trùng giá trị một hằng đã khai báo (dùng hằng đó).
**Sửa:** `private static final String FIELD_CAMPAIGN_ID = "campaignId";` đầu class, thay mọi chỗ.
**Bẫy:** chuỗi trong annotation (`@Query`, `@JsonProperty`) phải là compile-time constant — hằng
`static final String` vẫn dùng được. Không gộp hai literal giống chữ nhưng khác nghĩa nghiệp vụ vào một
hằng có tên sai nghĩa.

## S1452 — Wildcard trong kiểu trả về
**Nhận diện:** `public ResponseEntity<?> x()`, `List<? extends T> get()`.
**Sửa:** dùng kiểu cụ thể (`ResponseEntity<ApiResponse<RuleDto>>`), hoặc `ResponseEntity<Object>` nếu
thật sự nhiều kiểu. **Bẫy:** đổi kiểu trả về của API public → kiểm OpenAPI/Swagger và mọi nơi gọi.

## S135 — Vòng lặp > 1 break/continue
**Nhận diện:** `for/while` có từ 2 `continue`/`break` trở lên.
**Sửa an toàn (giữ đúng thứ tự lọc):**
- Gom các điều kiện `continue` thành một method `shouldSkip(item)` → còn **1** `continue`:
  ```java
  for (var it : items) {
      if (shouldSkip(it)) continue;   // giữ nguyên thứ tự các điều kiện cũ bên trong shouldSkip
      ...
  }
  ```
- Nhiều `break` để tìm phần tử đầu tiên → đổi sang method trả về sớm (`return`) hoặc stream `findFirst`.
**Bẫy:** nếu giữa các `continue` có **side-effect** (log, đếm, gọi service) thì `shouldSkip` phải giữ
side-effect đó đúng vị trí — nếu không làm được thì tách thân vòng lặp ra method `processOne(it)` dùng
`return` thay `continue` (thứ tự giữ nguyên tuyệt đối).

## S3358 — Ternary lồng
**Sửa:** tách biến + `if/else`, hoặc method nhỏ `resolveX(...)`; giữ nguyên thứ tự điều kiện và nhánh cuối.

## S1612 — Lambda → method reference
`x -> x.campaignId()` → `EligibleOffer::campaignId`; `c -> c instanceof Foo` → `Foo.class::isInstance`.
**Bẫy:** chỉ đổi khi lambda chỉ gọi đúng một method với đúng tham số đó.

## S1710 — `@ApiResponses` bọc
`@ApiResponses({@ApiResponse(...), @ApiResponse(...)})` → viết lặp `@ApiResponse(...)` trực tiếp
(annotation `@Repeatable`). Không đổi nội dung từng `@ApiResponse`.

## S1141 — try lồng
**Sửa:** tách khối `try` bên trong ra method private (giữ nguyên catch/finally và exception ném ra).
**Bẫy:** method tách ra phải ném lại đúng loại exception để `catch` bên ngoài vẫn bắt như cũ.

## S1144 / S1068 / S1172 — private method / field / tham số không dùng
**Kiểm trước khi xoá:** grep toàn repo (kể cả reflection, `@Value`, Lombok, test), xem field có dùng
trong constructor để tính giá trị khác không. Tham số của method **public / override / interface**
không xoá được → bỏ qua, ghi báo cáo. Field gán qua constructor injection mà chỉ dùng trong constructor
→ chuyển thành biến cục bộ của constructor.
**Bẫy nghiệp vụ:** trường/tham số "không dùng" đôi khi là **dấu hiệu thiếu logic** (ví dụ `now` truyền
vào mapper mà không dùng → có thể quên lọc theo thời gian). Báo người duyệt thay vì xoá ngay nếu nghi.

## S1117 — Biến che field
Đổi tên biến cục bộ (`now` → `fixedNow`, `root` → `specRoot`). Chỉ đổi tên, không đổi logic.

## S107 — > 7 tham số
Gom tham số liên quan vào record/DTO nội bộ, hoặc builder. Constructor Spring nhiều dependency → cân nhắc
tách bớt trách nhiệm; nếu rủi ro → bỏ qua, ghi báo cáo.

## S125 — Code bị comment
Xoá khối code comment. Nếu là giải thích quan trọng → viết lại thành câu văn, không để code.

## S1128 — Import thừa
Xoá import không dùng.

## S108 — Khối rỗng
**Nhận diện:** `catch (...) {}`, `if (...) {}`, `else {}` rỗng.
**Sửa:** thêm comment giải thích vì sao rỗng **nếu cố ý**; nếu là nhánh bị bỏ sót logic → **báo người
duyệt** (có thể là bug thật), không tự điền logic.

## S1075 — URI viết cứng
Đưa đường dẫn vào hằng hoặc cấu hình (`@Value("${...}")`). Path trong `@RequestMapping` của controller
là hợp lệ — rule bắt khi URI dùng như **chuỗi giá trị** (redirect, gọi ra ngoài).

## S1874 — API deprecated
Đổi sang API thay thế mà Javadoc `@deprecated` chỉ định. Trong test cũng tính.

## S1130 — `throws` thừa
Bỏ `throws Exception` khỏi method test/production nếu thân không ném checked exception.
