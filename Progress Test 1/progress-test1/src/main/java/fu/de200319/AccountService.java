package fu.de200319;

import java.time.LocalDate;
import java.util.*;

public class AccountService {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    // Lưu trữ tài khoản và email bằng key chữ thường (lowercase)
    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, String> emails = new HashMap<>();

    public AccountService() {}

    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // 1. BR-REG-01: Kiểm tra trường bắt buộc rỗng hoặc ngày sinh tương lai
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }

        // 2. BR-REG-02: Định dạng username
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // 3. BR-REG-04: Định dạng email
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // 4. BR-REG-06: Độ mạnh mật khẩu
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // 5. BR-REG-07: Xác nhận mật khẩu khớp
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // 6. BR-REG-08: Đủ tuổi (>= 18 tuổi)
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }

        // 7. BR-REG-09: Số điện thoại (tùy chọn, nhưng nếu có nhập ký tự thì phải đúng định dạng)
        if (phone != null && !phone.isEmpty()) {
            if (!AccountValidator.isValidPhone(phone)) {
                return ResultCode.INVALID_PHONE;
            }
        }

        // 8. BR-REG-03: Trùng username (không phân biệt hoa thường)
        String userKey = key(username);
        if (accounts.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // 9. BR-REG-05: Trùng email (không phân biệt hoa thường)
        String emailKey = key(email);
        if (emails.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // 10. BR-REG-10: Đăng ký thành công, sinh salt riêng và băm mật khẩu
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(salt, password);
        Account acc = new Account(username, emailKey, dateOfBirth, phone, salt, hash);

        accounts.put(userKey, acc);
        emails.put(emailKey, userKey);
        return ResultCode.SUCCESS;
    }

    public ResultCode login(String username, String password) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode unlockAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode disableAccount(String username) {
        throw new UnsupportedOperationException("TODO");
    }

    public Optional<Account> findByUsername(String username) {
        if (isBlank(username)) return Optional.empty();
        return Optional.ofNullable(accounts.get(key(username)));
    }

    public boolean isLocked(String username) {
        if (isBlank(username)) return false;
        Account acc = accounts.get(key(username));
        return acc != null && acc.isLocked();
    }

    public ResultCode changePassword(String username, String oldPassword, String newPassword, String confirmPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    public TokenResult requestPasswordReset(String email) {
        throw new UnsupportedOperationException("TODO");
    }

    public ResultCode resetPassword(String token, String newPassword, String confirmPassword) {
        throw new UnsupportedOperationException("TODO");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String key(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}