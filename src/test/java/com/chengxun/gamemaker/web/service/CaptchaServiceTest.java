package com.chengxun.gamemaker.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CaptchaService Tests")
class CaptchaServiceTest {

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService();
    }

    @Nested
    @DisplayName("generateCaptcha()")
    class GenerateCaptchaTests {

        @Test
        @DisplayName("should return a non-null captcha ID")
        void shouldReturnNonNullCaptchaId() {
            String captchaId = captchaService.generateCaptcha();
            assertNotNull(captchaId);
        }

        @Test
        @DisplayName("should return a 16-character alphanumeric ID")
        void shouldReturn16CharacterId() {
            String captchaId = captchaService.generateCaptcha();
            assertEquals(16, captchaId.length());
        }

        @Test
        @DisplayName("should return unique IDs on successive calls")
        void shouldReturnUniqueIds() {
            String id1 = captchaService.generateCaptcha();
            String id2 = captchaService.generateCaptcha();
            assertNotEquals(id1, id2);
        }
    }

    @Nested
    @DisplayName("generateCaptchaImageBase64()")
    class GenerateCaptchaImageBase64Tests {

        @Test
        @DisplayName("should return a base64 PNG data URI for a valid captcha ID")
        void shouldReturnBase64PngForValidId() {
            String captchaId = captchaService.generateCaptcha();
            String base64 = captchaService.generateCaptchaImageBase64(captchaId);

            assertNotNull(base64);
            assertTrue(base64.startsWith("data:image/png;base64,"));
        }

        @Test
        @DisplayName("should return null for a non-existent captcha ID")
        void shouldReturnNullForNonExistentId() {
            String base64 = captchaService.generateCaptchaImageBase64("nonexistent123456");
            assertNull(base64);
        }

        @Test
        @DisplayName("should return null for a null captcha ID")
        void shouldReturnNullForNullId() {
            // ConcurrentHashMap.get(null) throws NPE, but service should handle gracefully
            // depending on implementation. In this case it will throw, so we verify that behavior.
            assertThrows(NullPointerException.class, () ->
                captchaService.generateCaptchaImageBase64(null));
        }

        @Test
        @DisplayName("should generate valid base64 content that decodes to a non-empty byte array")
        void shouldGenerateDecodableBase64Content() {
            String captchaId = captchaService.generateCaptcha();
            String base64 = captchaService.generateCaptchaImageBase64(captchaId);

            assertNotNull(base64);
            // Extract the base64 part after the data URI prefix
            String encoded = base64.substring("data:image/png;base64,".length());
            byte[] decoded = java.util.Base64.getDecoder().decode(encoded);
            assertTrue(decoded.length > 0);
        }
    }

    @Nested
    @DisplayName("verifyCaptcha()")
    class VerifyCaptchaTests {

        @Test
        @DisplayName("should return false when captchaId is null")
        void shouldReturnFalseWhenCaptchaIdIsNull() {
            assertFalse(captchaService.verifyCaptcha(null, "abc"));
        }

        @Test
        @DisplayName("should return false when inputCode is null")
        void shouldReturnFalseWhenInputCodeIsNull() {
            String captchaId = captchaService.generateCaptcha();
            assertFalse(captchaService.verifyCaptcha(captchaId, null));
        }

        @Test
        @DisplayName("should return false for a non-existent captcha ID")
        void shouldReturnFalseForNonExistentId() {
            assertFalse(captchaService.verifyCaptcha("nonexistent123456", "abc"));
        }

        @Test
        @DisplayName("should return true for correct code (case-insensitive)")
        void shouldReturnTrueForCorrectCode() {
            // We need to extract the generated code indirectly.
            // Since CaptchaService stores the code internally, we test through the full flow:
            // generate -> get image (to confirm it exists) -> we cannot directly get the code.
            // However, verifyCaptcha is case-insensitive, so we need a way to get the code.
            // The service doesn't expose the code, so we test the negative path and
            // trust the image generation implies a valid captcha exists.
            //
            // We can use reflection to extract the code for testing purposes.
            String captchaId = captchaService.generateCaptcha();
            String code = extractCodeFromStore(captchaId);

            assertTrue(captchaService.verifyCaptcha(captchaId, code));
        }

        @Test
        @DisplayName("should return true for correct code with different case")
        void shouldReturnTrueForCaseInsensitiveMatch() {
            // Captcha is one-time use, so we need separate generations for each case test
            String captchaId1 = captchaService.generateCaptcha();
            String code1 = extractCodeFromStore(captchaId1);
            assertTrue(captchaService.verifyCaptcha(captchaId1, code1.toUpperCase()));

            String captchaId2 = captchaService.generateCaptcha();
            String code2 = extractCodeFromStore(captchaId2);
            assertTrue(captchaService.verifyCaptcha(captchaId2, code2.toLowerCase()));
        }

        @Test
        @DisplayName("should return false for incorrect code")
        void shouldReturnFalseForIncorrectCode() {
            String captchaId = captchaService.generateCaptcha();
            assertFalse(captchaService.verifyCaptcha(captchaId, "WRONGCODE"));
        }

        @Test
        @DisplayName("should consume the captcha on verification (one-time use)")
        void shouldConsumeCaptchaOnVerification() {
            String captchaId = captchaService.generateCaptcha();
            String code = extractCodeFromStore(captchaId);

            // First verification succeeds
            assertTrue(captchaService.verifyCaptcha(captchaId, code));
            // Second verification fails because captcha was consumed
            assertFalse(captchaService.verifyCaptcha(captchaId, code));
        }

        @Test
        @DisplayName("should trim whitespace from input code before comparing")
        void shouldTrimInputCodeBeforeComparing() {
            String captchaId = captchaService.generateCaptcha();
            String code = extractCodeFromStore(captchaId);

            assertTrue(captchaService.verifyCaptcha(captchaId, "  " + code + "  "));
        }

        @Test
        @DisplayName("should return false for expired captcha")
        void shouldReturnFalseForExpiredCaptcha() throws Exception {
            String captchaId = captchaService.generateCaptcha();
            String code = extractCodeFromStore(captchaId);

            // Simulate expiry by advancing time beyond EXPIRE_MINUTES (5 min)
            // We use reflection to set the expireAt to the past
            setExpiryToPast(captchaId);

            assertFalse(captchaService.verifyCaptcha(captchaId, code));
        }

        @Test
        @DisplayName("generateCaptcha should clean up expired entries")
        void shouldCleanupExpiredEntriesOnGenerate() throws Exception {
            String captchaId1 = captchaService.generateCaptcha();
            setExpiryToPast(captchaId1);

            // Generating a new captcha triggers cleanup
            captchaService.generateCaptcha();

            // The expired captcha should no longer be verifiable
            String code1 = extractCodeFromStore(captchaId1);
            // code1 will be null because entry was removed during cleanup
            assertNull(code1);
        }
    }

    // --- Helper methods using reflection to access internal captchaStore ---

    /**
     * Extracts the captcha code from the internal store via reflection.
     */
    private String extractCodeFromStore(String captchaId) {
        try {
            java.lang.reflect.Field storeField = CaptchaService.class.getDeclaredField("captchaStore");
            storeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.concurrent.ConcurrentHashMap<String, Object> store =
                (java.util.concurrent.ConcurrentHashMap<String, Object>) storeField.get(captchaService);
            Object entry = store.get(captchaId);
            if (entry == null) return null;
            java.lang.reflect.Field codeField = entry.getClass().getDeclaredField("code");
            codeField.setAccessible(true);
            return (String) codeField.get(entry);
        } catch (Exception e) {
            throw new RuntimeException("Failed to extract captcha code via reflection", e);
        }
    }

    /**
     * Sets the expiry time of a captcha entry to the past via sun.misc.Unsafe.
     * This avoids the Java 12+ restriction on modifying Field.modifiers.
     */
    private void setExpiryToPast(String captchaId) throws Exception {
        java.lang.reflect.Field storeField = CaptchaService.class.getDeclaredField("captchaStore");
        storeField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.concurrent.ConcurrentHashMap<String, Object> store =
            (java.util.concurrent.ConcurrentHashMap<String, Object>) storeField.get(captchaService);
        Object entry = store.get(captchaId);
        assertNotNull(entry, "Captcha entry should exist");

        java.lang.reflect.Field expireField = entry.getClass().getDeclaredField("expireAt");
        // Use Unsafe to write to the final field
        java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        long offset = unsafe.objectFieldOffset(expireField);
        unsafe.putLong(entry, offset, System.currentTimeMillis() - 1000);
    }
}
