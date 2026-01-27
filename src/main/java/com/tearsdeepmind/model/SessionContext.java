package com.tearsdeepmind.model;

import org.openqa.selenium.Cookie;
import java.util.Set;

/**
 * Java 21 Record to store session information for reuse.
 */
public record SessionContext(Set<Cookie> cookies, String localStorageJson) {
    public boolean isValid() {
        return cookies != null && !cookies.isEmpty();
    }
}
