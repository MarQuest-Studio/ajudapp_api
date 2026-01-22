package com.markish.ajudapp_api.common;

public final class ApiConstants {
    private ApiConstants() { /* utility */ }

    public static final class ObjectStorage {
        private ObjectStorage() {}
        public static final String PROFILES_PREFIX = "profiles/";
        public static final String KEY_FORMAT = PROFILES_PREFIX + "%s-%s";
    }
}
