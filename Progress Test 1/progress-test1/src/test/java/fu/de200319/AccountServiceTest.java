package fu.de200319;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
}