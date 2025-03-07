package com.bank.yankiservice.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void validatePhoneNumber_validPhoneNumber_shouldNotThrowException() {
        assertDoesNotThrow(() -> ValidationUtil.validatePhoneNumber("987654321"));
        assertDoesNotThrow(() -> ValidationUtil.validatePhoneNumber("9876543210"));
    }

    @Test
    void validatePhoneNumber_invalidPhoneNumber_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePhoneNumber(null));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePhoneNumber("12345"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePhoneNumber("123456789012"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validatePhoneNumber("phone123"));
    }

    @Test
    void validateDocumentNumber_validDocumentNumber_shouldNotThrowException() {
        assertDoesNotThrow(() -> ValidationUtil.validateDocumentNumber("12345678"));
        assertDoesNotThrow(() -> ValidationUtil.validateDocumentNumber("123456789012"));
    }

    @Test
    void validateDocumentNumber_invalidDocumentNumber_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateDocumentNumber(null));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateDocumentNumber("1234"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateDocumentNumber("12345678901234"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateDocumentNumber("document123"));
    }

    @Test
    void validateImei_validImei_shouldNotThrowException() {
        assertDoesNotThrow(() -> ValidationUtil.validateImei("123456789012345"));
    }

    @Test
    void validateImei_invalidImei_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateImei(null));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateImei("12345"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateImei("12345678901234"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateImei("imei123456789"));
    }

    @Test
    void validateEmail_validEmail_shouldNotThrowException() {
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("user@example.com"));
        assertDoesNotThrow(() -> ValidationUtil.validateEmail("user.name+tag@example.co.uk"));
    }

    @Test
    void validateEmail_invalidEmail_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEmail(null));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEmail("plainaddress"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEmail("@missingusername.com"));
        assertThrows(IllegalArgumentException.class, () -> ValidationUtil.validateEmail("user@.com"));
    }
}