package fu.de200319;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountValidatorTest {

    // --- USERNAME TESTS ---
    @ParameterizedTest(name = "[{index}] Username hop le: {0}")
    @ValueSource(strings = {"alice", "Alice_01", "Z____"})
    void isValidUsername_ValidValues_ReturnsTrue(String username) {
        assertTrue(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] Username khong hop le: {0}")
    @ValueSource(strings = {"ab_1", "1alice", "_alice", "ali ce", "alice!", "alice-01"})
    void isValidUsername_InvalidFormats_ReturnsFalse(String username) {
        assertFalse(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] Username null hoac khoang trang")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void isValidUsername_NullOrBlank_ReturnsFalse(String username) {
        assertFalse(AccountValidator.isValidUsername(username));
    }

    @ParameterizedTest(name = "[{index}] Bien do dai username {0} -> {1}")
    @MethodSource("usernameLengths")
    void isValidUsername_BoundaryLength(int length, boolean expected) {
        assertEquals(expected, AccountValidator.isValidUsername("a".repeat(length)));
    }

    static Stream<Arguments> usernameLengths() {
        return Stream.of(
                Arguments.of(4, false),
                Arguments.of(5, true),
                Arguments.of(6, true),
                Arguments.of(19, true),
                Arguments.of(20, true),
                Arguments.of(21, false)
        );
    }

    // --- EMAIL TESTS ---
    @ParameterizedTest(name = "[{index}] {2}")
    @CsvSource(delimiter = '|', value = {
            "user@domain.com       | true  | Format chuan",
            "u.ser+tag@d-main.vn   | true  | Ky tu hop le o local va hyphen o domain",
            "user@domain           | false | Thieu TLD",
            "user@domain.c         | false | TLD < 2 ky tu",
            "@domain.com           | false | Thieu local part",
            "user@.com             | false | Nhan domain rong"
    })
    void isValidEmail_Partitions(String email, boolean expected, String desc) {
        assertEquals(expected, AccountValidator.isValidEmail(email));
    }

    @ParameterizedTest(name = "[{index}] Bien do dai email {0} -> {1}")
    @CsvSource({
            "99, true",
            "100, true",
            "101, false"
    })
    void isValidEmail_BoundaryLength(int totalLen, boolean expected) {
        String base = "@b.co"; // 5 ky tu
        String local = "a".repeat(totalLen - base.length());
        assertEquals(expected, AccountValidator.isValidEmail(local + base));
    }

    // --- PASSWORD TESTS ---
    @ParameterizedTest(name = "[{index}] {3}")
    @CsvSource(delimiter = '|', value = {
            "Secret@123    | alice_01 | true  | Hop le day du 4 nhom",
            "secret@123    | alice_01 | false | Thieu chu hoa",
            "SECRET@123    | alice_01 | false | Thieu chu thuong",
            "Secret!@#$    | alice_01 | false | Thieu chu so",
            "Secret1234    | alice_01 | false | Thieu ky tu dac biet",
            "'Secret @123' | alice_01 | false | Chua khoang trang",
            "Secret~123    | alice_01 | false | Chua ky tu khong cho phep (~)",
            "Xalice_01@1   | alice_01 | false | Chua username",
            "Xalice_01@1   |          | true  | Username null bo qua kiem tra",
            "Secret@123    | ''       | true  | Username blank bo qua kiem tra"
    })
    void isValidPassword_Partitions(String pw, String user, boolean expected, String desc) {
        assertEquals(expected, AccountValidator.isValidPassword(pw, user));
    }

    @ParameterizedTest(name = "[{index}] Bien do dai mat khau {0} -> {1}")
    @CsvSource({
            "7, false",
            "8, true",
            "32, true",
            "33, false"
    })
    void isValidPassword_BoundaryLength(int length, boolean expected) {
        String base = "Aa1@" + "a".repeat(length - 4);
        assertEquals(expected, AccountValidator.isValidPassword(base, "user"));
    }

    // --- PHONE TESTS ---
    @ParameterizedTest(name = "[{index}] SDT hop le: {0}")
    @ValueSource(strings = {"0312345678", "0512345678", "0712345678", "0812345678", "0912345678"})
    void isValidPhone_ValidPrefixes_ReturnsTrue(String phone) {
        assertTrue(AccountValidator.isValidPhone(phone));
    }

    @ParameterizedTest(name = "[{index}] SDT khong hop le: {0}")
    @ValueSource(strings = {"0212345678", "091234567", "09123456789", "09abcdefgh"})
    void isValidPhone_InvalidFormats_ReturnsFalse(String phone) {
        assertFalse(AccountValidator.isValidPhone(phone));
    }

    // --- AGE TESTS ---
    @ParameterizedTest(name = "[{index}] Sinh ngay {0}, ngay xet {1} -> {2} tuoi")
    @CsvSource({
            "2008-09-28, 2026-09-28, 18",
            "2008-09-29, 2026-09-28, 17",
            "2008-02-29, 2026-02-28, 17",
            "2008-02-29, 2026-03-01, 18"
    })
    void calculateAge_Boundaries(LocalDate dob, LocalDate today, int expected) {
        assertEquals(expected, AccountValidator.calculateAge(dob, today));
    }
}