package com.drewmalin.vm.central.http.auth;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;

/**
 * A utility class for cryptography -- primarily meant for use in generating secure password salts and hashes.
 */
public class Crypto {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA1";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final SecretKeyFactory KEY_FACTORY;

    static {
        try {
            KEY_FACTORY = SecretKeyFactory.getInstance(ALGORITHM);
        }
        catch (final NoSuchAlgorithmException e) {
            // Crash!
            throw new RuntimeException(e);
        }
    }

    /**
     * Encodes the provided raw String into a new {@link EncodedPassword} object.
     *
     * @param password the raw {@link String} password
     *
     * @return a new, cryptographically secure {@link EncodedPassword} containing the hash and salt, ready for database
     * storage.
     */
    public static EncodedPassword encode(final String password) {
        return new EncodedPasswordBuilder(password).build();
    }

    public static EncodedPasswordBuilder builder(final String password) {
        return new EncodedPasswordBuilder(password);
    }

    /**
     * A simple tuple wrapping the generated password contents: the salt, and the hash.
     *
     * @param hash {@link byte[]} hashed password
     * @param salt {@link byte[]} password salt
     */
    public record EncodedPassword(byte[] hash, byte[] salt) {

    }

    /**
     * A builder of {@link EncodedPassword}s.
     */
    public static class EncodedPasswordBuilder {

        private static final int DEFAULT_SALT_LENGTH = 16;
        private static final int DEFAULT_ITERATION_COUNT = 65536;
        private static final int DEFAULT_KEY_LENGTH = 128;

        private final String password;

        private int saltLength;
        private int iterationCount;
        private int keyLength;
        private byte[] salt;

        private EncodedPasswordBuilder(final String password) {
            this.password = password;
        }

        public EncodedPasswordBuilder salt(final byte[] salt) {
            this.salt = salt;
            return this;
        }

        public EncodedPasswordBuilder saltLength(final int saltLength) {
            this.saltLength = saltLength;
            return this;
        }

        public EncodedPasswordBuilder iterationCount(final int iterationCount) {
            this.iterationCount = iterationCount;
            return this;
        }

        public EncodedPasswordBuilder keyLength(final int keyLength) {
            this.keyLength = keyLength;
            return this;
        }

        public EncodedPassword build() {
            if (this.saltLength <= 0) {
                this.saltLength = DEFAULT_SALT_LENGTH;
            }
            if (this.iterationCount <= 0) {
                this.iterationCount = DEFAULT_ITERATION_COUNT;
            }
            if (this.keyLength <= 0) {
                this.keyLength = DEFAULT_KEY_LENGTH;
            }

            /*
             * Nothing fancy for the salt -- just use the SecureRandom to generate some bytes.
             */
            final byte[] finalSalt;
            if (this.salt == null) {
                finalSalt = new byte[this.saltLength];
                RANDOM.nextBytes(finalSalt);
            }
            else {
                finalSalt = this.salt;
            }

            /*
             * Generate a secure hash of the provided raw password string using the salt, iteration count, and key length.
             */
            final var spec = new PBEKeySpec(this.password.toCharArray(), finalSalt, this.iterationCount, this.keyLength);
            try {
                final byte[] hash = KEY_FACTORY.generateSecret(spec).getEncoded();
                return new EncodedPassword(hash, finalSalt);
            }
            catch (final InvalidKeySpecException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
