package com.mytravel.common.util;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class EmailValidator {

    private static final List<String> ALLOWED_DOMAINS = Arrays.asList(
            "qq.com", "163.com", "126.com", "gmail.com",
            "outlook.com", "foxmail.com", "sina.com",
            "yahoo.com", "hotmail.com"
    );

    public boolean isAllowed(String email) {
        String domain = email.substring(email.indexOf("@") + 1);
        return ALLOWED_DOMAINS.stream()
                .anyMatch(d -> domain.equalsIgnoreCase(d));
    }
}
