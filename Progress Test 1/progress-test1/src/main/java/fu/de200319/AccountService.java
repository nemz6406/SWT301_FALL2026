package fu.de200319;

import java.time.LocalDate;
import java.util.*;

public class AccountService {
    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final int PASSWORD_HISTORY_SIZE = 3;
    public static final int MIN_AGE = 18;

    private final Map<String, Account> accounts = new HashMap<>();
    private final Map<String, String> emails = new HashMap<>();

    public AccountService() {}

    public ResultCode register(String username, String email, String password,
                               String confirmPassword, LocalDate dateOfBirth, String phone) {
        LocalDate today = LocalDate.now();

        // 1. BR-REG-01
        if (isBlank(username) || isBlank(email) || isBlank(password) || isBlank(confirmPassword)
                || dateOfBirth == null || dateOfBirth.isAfter(today)) {
            return ResultCode.INVALID_INPUT;
        }

        // 2. BR-REG-02
        if (!AccountValidator.isValidUsername(username)) {
            return ResultCode.INVALID_USERNAME;
        }

        // 3. BR-REG-04
        if (!AccountValidator.isValidEmail(email)) {
            return ResultCode.INVALID_EMAIL;
        }

        // 4. BR-REG-06
        if (!AccountValidator.isValidPassword(password, username)) {
            return ResultCode.WEAK_PASSWORD;
        }

        // 5. BR-REG-07
        if (!password.equals(confirmPassword)) {
            return ResultCode.PASSWORD_MISMATCH;
        }

        // 6. BR-REG-08
        if (AccountValidator.calculateAge(dateOfBirth, today) < MIN_AGE) {
            return ResultCode.UNDERAGE;
        }

        // 7. BR-REG-09
        if (phone != null && !phone.isEmpty()) {
            if (!AccountValidator.isValidPhone(phone)) {
                return ResultCode.INVALID_PHONE;
            }
        }

        // 8. BR-REG-03
        String userKey = key(username);
        if (accounts.containsKey(userKey)) {
            return ResultCode.DUPLICATE_USERNAME;
        }

        // 9. BR-REG-05
        String emailKey = key(email);
        if (emails.containsKey(emailKey)) {
            return ResultCode.DUPLICATE_EMAIL;
        }

        // 10. BR-REG-10
        String salt = PasswordHasher.generateSalt();
        String hash = PasswordHasher.hash(salt, password);
        Account acc = new Account(username, emailKey, dateOfBirth, phone, salt, hash);

        accounts.put(userKey, acc);
        emails.put(emailKey, userKey);
        return ResultCode.SUCCESS;
    }

    // Cài đặt TODO-6: Logic đăng nhập theo bảng quyết định
    public ResultCode login(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            return ResultCode.INVALID_INPUT;
        }

        Account acc = accounts.get(key(username));
        // Rule 1: User không tồn tại -> Không tiết lộ lý do
        if (acc == null) {
            return ResultCode.INVALID_CREDENTIALS;
        }

        // Rule 2: Tài khoản bị vô hiệu hóa
        if (acc.getStatus() == AccountStatus.DISABLED) {
            return ResultCode.ACCOUNT_DISABLED;
        }

        // Rule 3: Đang bị khóa -> Không tăng bộ đếm, trả ACCOUNT_LOCKED
        if (acc.isLocked()) {
            return ResultCode.ACCOUNT_LOCKED;
        }

        // Kiểm tra mật khẩu (Rule 4, 5, 6)
        if (!PasswordHasher.matches(acc.getSalt(), password, acc.getCurrentPasswordHash())) {
            acc.incrementFailedAttempts();
            // Lần sai thứ 5 (>= 5) thì khóa tài khoản (Rule 5)
            if (acc.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
                acc.lock();
                return ResultCode.ACCOUNT_LOCKED;
            }
            // Sai từ 1 đến 4 lần (Rule 4)
            return ResultCode.INVALID_CREDENTIALS;
        }

        // Rule 6: Đăng nhập thành công -> Đặt bộ đếm về 0
        acc.resetFailedAttempts();
        return ResultCode.SUCCESS;
    }

    // BR-ADM-03: Mở khóa tài khoản, đặt lại failedAttempts = 0
    public ResultCode unlockAccount(String username) {
        if (isBlank(username)) return ResultCode.USER_NOT_FOUND;
        Account acc = accounts.get(key(username));
        if (acc == null) return ResultCode.USER_NOT_FOUND;
        acc.unlock();
        return ResultCode.SUCCESS;
    }

    // Vô hiệu hóa tài khoản
    public ResultCode disableAccount(String username) {
        if (isBlank(username)) return ResultCode.USER_NOT_FOUND;
        Account acc = accounts.get(key(username));
        if (acc == null) return ResultCode.USER_NOT_FOUND;
        acc.setStatus(AccountStatus.DISABLED);
        return ResultCode.SUCCESS;
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