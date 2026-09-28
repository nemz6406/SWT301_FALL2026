package fu.de200319;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.time.LocalDate;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AccountServiceTest {

    private AccountService service;

    private static final String USER = "alice_01";
    private static final String EMAIL = "alice@example.com";
    private static final String PASS = "Secret@123";
    private static final LocalDate DOB = LocalDate.now().minusYears(20);
    private static final String PHONE = "0912345678";

    @BeforeEach
    void setUp() {
        service = new AccountService(); // Mỗi test tạo một service mới độc lập
    }

    @Nested
    class Register {

        @Test
        void register_ValidData_SuccessAndStateCorrect() {
            ResultCode result = service.register(USER, "Alice@Example.COM", PASS, PASS, DOB, PHONE);
            assertEquals(ResultCode.SUCCESS, result);

            // Kiểm tra trạng thái lưu trữ của tài khoản
            Optional<Account> opt = service.findByUsername(USER);
            assertTrue(opt.isPresent());
            Account acc = opt.get();
            assertEquals("alice@example.com", acc.getEmail()); // Email phải lưu dạng lowercase
            assertEquals(AccountStatus.ACTIVE, acc.getStatus());
            assertEquals(0, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
            assertNotEquals(PASS, acc.getCurrentPasswordHash()); // Không lưu mật khẩu rõ
        }

        @Test
        void register_EmptyOrNullPhone_Success() {
            assertEquals(ResultCode.SUCCESS, service.register("bob_01", "bob@example.com", PASS, PASS, DOB, ""));
            assertEquals(ResultCode.SUCCESS, service.register("carol_01", "carol@example.com", PASS, PASS, DOB, null));
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("fu.de200319.AccountServiceTest#invalidRegisterInputs")
        void register_InvalidInput_ReturnsExpectedCode(String desc, String u, String e, String p, String c,
                                                       LocalDate dob, String phone, ResultCode expected) {
            assertEquals(expected, service.register(u, e, p, c, dob, phone));
            if (u != null) {
                assertTrue(service.findByUsername(u).isEmpty()); // Đăng ký lỗi thì không tạo tài khoản
            }
        }

        @ParameterizedTest(name = "[{index}] today - {0} năm + {1} ngày -> {2}")
        @CsvSource({
                "18, 0, SUCCESS",     // Đúng 18 tuổi tròn
                "18, 1, UNDERAGE",    // 18 tuổi thiếu 1 ngày
                "0, 1, INVALID_INPUT" // Ngày sinh tương lai
        })
        void register_AgeBoundary(int yearsAgo, int plusDays, ResultCode expected) {
            LocalDate birth = LocalDate.now().minusYears(yearsAgo).plusDays(plusDays);
            assertEquals(expected, service.register("user_age", "age@domain.com", PASS, PASS, birth, null));
        }

        @ParameterizedTest(name = "[{index}] Trùng username (không phân biệt hoa/thường): {0}")
        @ValueSource(strings = {"alice_01", "ALICE_01", "Alice_01"})
        void register_DuplicateUsername_CaseInsensitive(String duplicateUser) {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
            ResultCode res = service.register(duplicateUser, "other@domain.com", PASS, PASS, DOB, PHONE);
            assertEquals(ResultCode.DUPLICATE_USERNAME, res);
        }

        @ParameterizedTest(name = "[{index}] Trùng email (không phân biệt hoa/thường): {0}")
        @ValueSource(strings = {"alice@example.com", "ALICE@example.COM", "Alice@Example.com"})
        void register_DuplicateEmail_CaseInsensitive(String duplicateEmail) {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
            ResultCode res = service.register("other_user", duplicateEmail, PASS, PASS, DOB, PHONE);
            assertEquals(ResultCode.DUPLICATE_EMAIL, res);
        }
    }

    // Provider cho các trường hợp dữ liệu sai và thứ tự ưu tiên kiểm tra
    static Stream<Arguments> invalidRegisterInputs() {
        return Stream.of(
                Arguments.of("REG-01: Username null", null, EMAIL, PASS, PASS, DOB, PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("REG-01: Dob tuong lai", USER, EMAIL, PASS, PASS, LocalDate.now().plusDays(1), PHONE, ResultCode.INVALID_INPUT),
                Arguments.of("REG-02: Username sai định dạng", "1alice", EMAIL, PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("REG-04: Email sai định dạng", USER, "bademail", PASS, PASS, DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("REG-06: Mật khẩu yếu", USER, EMAIL, "weak", "weak", DOB, PHONE, ResultCode.WEAK_PASSWORD),
                Arguments.of("REG-07: Xác nhận mật khẩu không khớp", USER, EMAIL, PASS, "NotMatch@123", DOB, PHONE, ResultCode.PASSWORD_MISMATCH),
                Arguments.of("REG-08: Chưa đủ 18 tuổi", USER, EMAIL, PASS, PASS, LocalDate.now().minusYears(17), PHONE, ResultCode.UNDERAGE),
                Arguments.of("REG-09: Phone sai định dạng", USER, EMAIL, PASS, PASS, DOB, "   ", ResultCode.INVALID_PHONE),
                // 3 test kiểm tra thứ tự ưu tiên (Priority order) theo yêu cầu đề bài
                Arguments.of("Ưu tiên 1: Username sai + Email sai -> Báo INVALID_USERNAME", "1alice", "bademail", PASS, PASS, DOB, PHONE, ResultCode.INVALID_USERNAME),
                Arguments.of("Ưu tiên 2: Email sai + MK yếu -> Báo INVALID_EMAIL", USER, "bademail", "weak", "weak", DOB, PHONE, ResultCode.INVALID_EMAIL),
                Arguments.of("Ưu tiên 3: MK yếu + Confirm lệch -> Báo WEAK_PASSWORD", USER, EMAIL, "weak", "x", DOB, PHONE, ResultCode.WEAK_PASSWORD)
        );
    }
    @Nested
    class Login {

        private static final String WRONG = "Wrong@123";

        @BeforeEach
        void registerUser() {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
        }

        // Rule 6: Đăng nhập đúng -> SUCCESS và reset bộ đếm về 0
        @Test
        void login_CorrectCredentials_SuccessAndResetsCounter() {
            service.login(USER, WRONG); // Tạo 1 lần sai trước đó
            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
            assertEquals(0, service.findByUsername(USER).get().getFailedAttempts());
        }

        // Rule 1: User không tồn tại -> INVALID_CREDENTIALS
        @Test
        void login_UserNotFound_ReturnsInvalidCredentials() {
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login("not_exist", PASS));
        }

        // Rule 2: Tài khoản DISABLED -> ACCOUNT_DISABLED (dù đúng hay sai mật khẩu)
        @ParameterizedTest(name = "[{index}] Disabled user login với pass: {0}")
        @ValueSource(strings = {PASS, WRONG})
        void login_DisabledAccount_ReturnsAccountDisabled(String pw) {
            service.disableAccount(USER);
            assertEquals(ResultCode.ACCOUNT_DISABLED, service.login(USER, pw));
        }

        // Rule 4: Sai từ 1 đến 4 lần -> INVALID_CREDENTIALS, tăng bộ đếm, chưa khóa
        @ParameterizedTest(name = "[{index}] Sai {0} lần -> INVALID_CREDENTIALS")
        @ValueSource(ints = {1, 2, 3, 4})
        void login_FailedAttemptsUnderThreshold_ReturnsInvalidCredentials(int times) {
            for (int i = 0; i < times; i++) {
                assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, WRONG));
            }
            Account acc = service.findByUsername(USER).get();
            assertEquals(times, acc.getFailedAttempts());
            assertFalse(acc.isLocked());
        }

        // Rule 5: Sai lần thứ 5 -> ACCOUNT_LOCKED và khóa tài khoản
        @Test
        void login_FifthFailedAttempt_LocksAccount() {
            for (int i = 0; i < 4; i++) service.login(USER, WRONG);
            ResultCode fifth = service.login(USER, WRONG);
            assertEquals(ResultCode.ACCOUNT_LOCKED, fifth);
            assertTrue(service.isLocked(USER));
        }

        // Rule 3: Đang khóa -> nhập đúng hay sai đều ACCOUNT_LOCKED, bộ đếm không đổi
        @Test
        void login_AlreadyLocked_DoesNotIncreaseCounter() {
            for (int i = 0; i < 5; i++) service.login(USER, WRONG);
            int attempts = service.findByUsername(USER).get().getFailedAttempts();

            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, WRONG));
            assertEquals(ResultCode.ACCOUNT_LOCKED, service.login(USER, PASS));
            assertEquals(attempts, service.findByUsername(USER).get().getFailedAttempts());
        }

        // Biên số lần đăng nhập sai: 4 lần rồi đúng -> SUCCESS; 5 lần rồi đúng -> ACCOUNT_LOCKED
        @ParameterizedTest(name = "[{index}] Sai {0} lần rồi nhập đúng -> {1}, locked={2}")
        @CsvSource({
                "4, SUCCESS, false",
                "5, ACCOUNT_LOCKED, true"
        })
        void login_CorrectPasswordAfterFailures(int failures, ResultCode expected, boolean locked) {
            for (int i = 0; i < failures; i++) service.login(USER, WRONG);
            assertEquals(expected, service.login(USER, PASS));
            assertEquals(locked, service.isLocked(USER));
        }

        // Username không phân biệt hoa/thường
        @ParameterizedTest(name = "[{index}] Case insensitivity username: {0}")
        @ValueSource(strings = {"ALICE_01", "Alice_01", "alice_01"})
        void login_UsernameCaseInsensitive_Success(String inputUser) {
            assertEquals(ResultCode.SUCCESS, service.login(inputUser, PASS));
        }

        // Mật khẩu phân biệt hoa/thường
        @Test
        void login_PasswordCaseSensitive_Fails() {
            assertEquals(ResultCode.INVALID_CREDENTIALS, service.login(USER, "secret@123"));
        }

        // Input rỗng hoặc null
        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void login_BlankInput_ReturnsInvalidInput(String bad) {
            assertEquals(ResultCode.INVALID_INPUT, service.login(bad, PASS));
            assertEquals(ResultCode.INVALID_INPUT, service.login(USER, bad));
        }
    }

    @Nested
    class Admin {

        private static final String WRONG = "Wrong@123";

        @BeforeEach
        void registerUser() {
            service.register(USER, EMAIL, PASS, PASS, DOB, PHONE);
        }

        // Mở khóa: reset bộ đếm về 0 và cho phép đăng nhập lại
        @Test
        void unlockAccount_ValidUser_UnlocksAndResetsCounter() {
            for (int i = 0; i < 5; i++) service.login(USER, WRONG);
            assertTrue(service.isLocked(USER));

            assertEquals(ResultCode.SUCCESS, service.unlockAccount(USER));
            assertFalse(service.isLocked(USER));
            assertEquals(0, service.findByUsername(USER).get().getFailedAttempts());

            assertEquals(ResultCode.SUCCESS, service.login(USER, PASS));
        }

        @Test
        void unlockAccount_NotFound_ReturnsUserNotFound() {
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount("ghost"));
            assertEquals(ResultCode.USER_NOT_FOUND, service.unlockAccount(""));
        }

        @Test
        void disableAccount_NotFound_ReturnsUserNotFound() {
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount("ghost"));
            assertEquals(ResultCode.USER_NOT_FOUND, service.disableAccount(null));
        }

        @Test
        void isLocked_UnknownUserOrNull_ReturnsFalse() {
            assertFalse(service.isLocked("ghost"));
            assertFalse(service.isLocked(null));
        }
    }
}