# Slash Commands for Parallel Task Execution

## 🎯 Mục đích

Các slash commands này cho phép bạn chạy **5 Claude Code sessions đồng thời** để cùng fix validation module.

## 📋 Danh sách Commands

| Command | Task | Có thể chạy song song? | Thời gian |
|---------|------|------------------------|-----------|
| `/fix-task-01` | Fix Persistence Ports | ✅ Yes | 20 min |
| `/fix-task-03` | Fix Domain Events | ✅ Yes | 20 min |
| `/fix-task-04` | Fix DTOs | ✅ Yes | 15 min |
| `/fix-task-02` | Fix RuleJpaAdapter | ❌ No (needs task-01) | 25 min |
| `/fix-task-05` | Fix DeploymentService | ✅ Yes | 20 min |

## 🚀 Cách sử dụng

### Bước 1: Mở 5 Terminal Windows

Mở 5 cửa sổ terminal riêng biệt, mỗi cửa sổ cd vào validation:

```bash
# Terminal 1
cd /Users/hoanglam/promix/validation

# Terminal 2
cd /Users/hoanglam/promix/validation

# Terminal 3
cd /Users/hoanglam/promix/validation

# Terminal 4
cd /Users/hoanglam/promix/validation

# Terminal 5
cd /Users/hoanglam/promix/validation
```

### Bước 2: Khởi động Claude Code ở mỗi terminal

```bash
# Trong mỗi terminal
claude
```

### Bước 3: Chạy slash commands - Phase 1 (Song song)

**Terminal 1**:
```
/fix-task-01
```

**Terminal 2**:
```
/fix-task-03
```

**Terminal 3**:
```
/fix-task-04
```

**Terminal 4**: Đợi (sẽ dùng sau)

**Terminal 5**: Đợi (sẽ dùng sau)

### Bước 4: Đợi Phase 1 hoàn thành

Khi cả 3 terminals (1, 2, 3) báo "Task completed successfully", tiếp tục Phase 2.

### Bước 5: Chạy Phase 2 (Song song)

**Terminal 4**:
```
/fix-task-02
```
(Task này cần task-01 hoàn thành trước)

**Terminal 5**:
```
/fix-task-05
```

## 📊 Progress Tracking

Dùng checklist này để theo dõi:

```
Phase 1 (Parallel):
[ ] Task 01 - Persistence Ports (Terminal 1)
[ ] Task 03 - Domain Events (Terminal 2)
[ ] Task 04 - DTOs (Terminal 3)

Phase 2 (After Phase 1):
[ ] Task 02 - RuleJpaAdapter (Terminal 4) - needs Task 01
[ ] Task 05 - DeploymentService (Terminal 5)
```

## 🎓 Workflow Diagram

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Terminal 1  │     │ Terminal 2  │     │ Terminal 3  │
│ /fix-task-01│────▶│ /fix-task-03│────▶│ /fix-task-04│
│   (20 min)  │     │   (20 min)  │     │   (15 min)  │
└──────┬──────┘     └─────────────┘     └─────────────┘
       │
       │ ✅ Complete
       │
       ▼
┌─────────────┐     ┌─────────────┐
│ Terminal 4  │     │ Terminal 5  │
│ /fix-task-02│     │ /fix-task-05│
│   (25 min)  │     │   (20 min)  │
└─────────────┘     └─────────────┘
```

## ✅ Verification

Sau khi tất cả tasks hoàn thành, chạy build:

```bash
cd /Users/hoanglam/promix/validation
mvn clean compile -DskipTests
```

**Expected**: Compilation errors giảm từ 89 xuống còn ~40-50.

## 🔧 Available Commands

### Đã có sẵn:
- ✅ `/fix-task-01` - Fix Persistence Ports
- ✅ `/fix-task-03` - Fix Domain Events
- ✅ `/fix-task-04` - Fix DTOs

### Cần tạo thêm:
- ⬜ `/fix-task-02` - Fix RuleJpaAdapter
- ⬜ `/fix-task-05` - Fix DeploymentService
- ⬜ `/fix-task-06` - Fix CommandHandler
- ⬜ `/fix-task-07` - Fix Repositories
- ⬜ `/fix-task-08` - Fix PublishService
- ⬜ `/fix-task-09` - Final Verification

## 💡 Tips

1. **Chạy song song tối đa**: Phase 1 có thể chạy 3 tasks cùng lúc
2. **Monitor progress**: Mỗi Claude session sẽ báo cáo progress
3. **Verify incrementally**: Sau mỗi phase, chạy `mvn compile` để check
4. **Rollback if needed**: Mỗi terminal làm việc độc lập, dễ rollback

## ⚠️ Important Notes

- **Task 02 phải đợi Task 01** hoàn thành
- Mỗi terminal chạy **độc lập**, không ảnh hưởng lẫn nhau
- Nếu có conflict (hiếm khi xảy ra), Git sẽ báo merge conflict
- Recommend: Commit sau mỗi phase hoàn thành

## 🆘 Troubleshooting

### Nếu command không hoạt động:
```bash
# Check slash commands available
ls -la .claude/commands/

# Should see:
# fix-task-01.md
# fix-task-03.md
# fix-task-04.md
```

### Nếu Claude Code không nhìn thấy commands:
1. Restart Claude Code session
2. Hoặc chạy: `claude --reload`

### Nếu nhiều sessions conflict:
1. Commit changes sau mỗi task hoàn thành
2. Hoặc work trên branches riêng

## 📝 Next Steps

Sau khi 5 tasks này xong, tiếp tục với:
- Task 06: Fix CommandHandler
- Task 07: Fix Repositories
- Task 08: Fix PublishService
- Task 09: Final Verification

---

**Tổng thời gian dự kiến**:
- Sequential: ~2 hours
- Parallel (5 sessions): ~45-60 minutes ⚡
