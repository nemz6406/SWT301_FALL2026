# SWT301 – ProgressTest 1: Unit Testing

## 1. Thông tin sinh viên & Cách chạy
- **MSSV:** DE200319
- **Cách chạy kiểm thử:**
    - Chạy toàn bộ test qua Maven: `mvn clean test`
    - Báo cáo JaCoCo xuất tại: `target/site/jacoco/index.html`

---

## 2. Báo cáo kiểm thử Mutation Testing (3 lỗi giả lập)

| # | File / Vị trí | Lỗi chèn (Mutation) | Test bắt được lỗi (Failed Test) | Trạng thái hoàn tác |
|---|---|---|---|---|
| M1 | `AccountService.java` | Sửa `>= MAX_FAILED_ATTEMPTS` thành `> MAX_FAILED_ATTEMPTS` | `Login.login_FifthFailedAttempt_LocksAccount` | Đã hoàn tác |
| M2 | `AccountService.java` | Bỏ qua nhánh `if (acc.isLocked())` | `Login.login_AlreadyLocked_DoesNotIncreaseCounter` | Đã hoàn tác |
| M3 | `AccountValidator.java` | Sửa regex USERNAME `{4,19}` thành `{4,20}` | `AccountValidatorTest.isValidUsername_BoundaryLength` (độ dài 21) | Đã hoàn tác |

---

## 3. Báo cáo độ bao phủ JaCoCo (Coverage Summary)
- **AccountValidator:** Line ≥ 80%, Branch ≥ 70%
- **AccountService:** Line ≥ 80%, Branch ≥ 70%