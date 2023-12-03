package com.drewmalin.vm.central.data.repository;

import java.security.SecureRandom;
import java.util.Base64;

public class IdUtils {

    private static final int BYTE_LENGTH = 20;
    private static final SecureRandom RANDOM;
    private static final Base64.Encoder ENCODER;

    static {
        RANDOM = new SecureRandom();

        ENCODER = Base64.getUrlEncoder().withoutPadding();
    }

    private IdUtils() {

    }

    public static String nextId() {
        final byte[] buffer = new byte[BYTE_LENGTH];

        RANDOM.nextBytes(buffer);

        return ENCODER.encodeToString(buffer);
    }
}
