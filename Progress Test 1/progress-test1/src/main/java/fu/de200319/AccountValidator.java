package fu.de200319;

import java.time.LocalDate;
import java.time.Period;
import java.util.Locale;
import java.util.regex.Pattern;

public final class AccountValidator {

    // BR-REG-02: Bắt đầu bằng chữ cái, theo sau là chữ cái/số/gạch dưới, tổng dài từ 5 đến 20 ký tự
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{4,19}$");

    // BR-REG-04: local@domain.tld, TLD >= 2 chữ cái, nhãn domain không rỗng, tổng dài <= 100 ký tự
    private static final Pattern EMAIL =
            Pattern.compile("^[A-Za-z0-9._%+-]+@(?:[A-Za-z0-9-]+\\.)+[A-Za-z]{2,}$");

    // Đầu số 03, 05, 07, 08, 09 kèm đúng 8 chữ số tiếp theo
    private static final Pattern PHONE = Pattern.compile("^0[35789]\\d{8}$");

    // Danh sách ký tự đặc biệt hợp lệ theo đặc tả
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=";

    // Lớp tiện ích: constructor private để ngăn khởi tạo
    private AccountValidator() {}

    public static boolean isValidUsername(String username) {
        return username != null && USERNAME.matcher(username).matches();
    }

    public static boolean isValidEmail(String email) {
        if (email == null || email.length() > 100) {
            return false;
        }
        return EMAIL.matcher(email).matches();
    }

    public static boolean isValidPassword(String password, String username) {
        if (password == null || password.length() < 8 || password.length() > 32) {
            return false;
        }

        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        boolean special = false;

        for (char c : password.toCharArray()) {
            if (c >= 'A' && c <= 'Z') upper = true;
            else if (c >= 'a' && c <= 'z') lower = true;
            else if (c >= '0' && c <= '9') digit = true;
            else if (SPECIAL_CHARS.indexOf(c) >= 0) special = true;
            else return false; // Khoảng trắng hoặc ký tự lạ ngoài danh sách cho phép
        }

        // Kiểm tra đủ cả 4 nhóm ký tự bắt buộc
        if (!upper || !lower || !digit || !special) {
            return false;
        }

        // Không chứa username (không phân biệt hoa/thường), bỏ qua nếu username null hoặc rỗng
        if (username != null && !username.isBlank()) {
            String cleanUser = username.toLowerCase(Locale.ROOT);
            if (password.toLowerCase(Locale.ROOT).contains(cleanUser)) {
                return false;
            }
        }

        return true;
    }

    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE.matcher(phone).matches();
    }

    public static int calculateAge(LocalDate dob, LocalDate today) {
        if (dob == null || today == null) {
            return 0;
        }
        return Period.between(dob, today).getYears();
    }
}