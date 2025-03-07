package com.bank.yankiservice.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private ValidationUtil() {
    }

    public static void validatePhoneNumber(String phoneNumber) {
        if (phoneNumber == null || !Pattern.matches("\\d{9,10}", phoneNumber)) {
            throw new IllegalArgumentException("Phone number must contain between 9 and 10 digits");
        }
    }

    public static void validateDocumentNumber(String documentNumber) {
        if (documentNumber == null || !Pattern.matches("\\d{8,12}", documentNumber)) {
            throw new IllegalArgumentException("Document number must contain between 8 and 12 digits");
        }
    }

    public static void validateImei(String imei) {
        if (imei == null || !Pattern.matches("\\d{15}", imei)) {
            throw new IllegalArgumentException("IMEI must contain exactly 15 digits");
        }
    }

    public static void validateEmail(String email) {
        if (email == null || !Pattern.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$", email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
}