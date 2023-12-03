package com.drewmalin.vm.central.http.auth;

import com.drewmalin.vm.central.configuration.Config;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.authentication.AuthenticationProvider;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * An Auth[orization|entication]Provider. The primary use for this class is to generate a {@link JWTAuth} for validation
 * (and generation!) of JWT tokens.
 */
public class AuthProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthProvider.class);

    private static final int TOKEN_EXPIRATION_MILLIS = 1000 * 60 * 60; // TODO: this should be much shorter (implement refresh tokens)

    private final Pool dbPool;
    private final JWTAuth jwtAuth;
    private final AuthenticationProvider authentication;
    private final UserRepository userRepository;

    public AuthProvider(final Pool dbPool, final Config.VmCentral config, final Vertx vertx) {
        this.dbPool = dbPool;

        this.jwtAuth = newJwtAuthorizationProvider(config, vertx);
        this.authentication = newAuthenticationProvider();
        this.userRepository = new UserRepositorySql(dbPool);
    }


    /*
     * This AuthenticationProvider is invoked on each call to create a new token. This implies that the provided payload
     * (which should have been validated by the calling context) contains a valid username/password.
     */
    private AuthenticationProvider newAuthenticationProvider() {
        return (credentials, handler) -> {

            /*
             * Step 1: retrieve the username/password contents from the credentials payload
             */
            final var username = credentials.getString("username");
            final var password = credentials.getString("password");
            if (username == null || password == null) {
                handler.handle(Future.failedFuture(new IllegalArgumentException("Invalid credentials")));
            }

            /*
             * Step 2: fetch the corresponding user
             */
            this.dbPool.withConnection(sqlConnection -> {
                    return this.userRepository.get(username, password);
                })
                .onSuccess(user -> {

                    /*
                     * Step 3: we did it! Wrap the minimal user details for use in the final JWT token (note that we
                     * need very little of the actual UserDTO, but the ID will be useful for authorization checks, and
                     * the username will be useful for logging and error messages.
                     */

                    final var contextUser = User.create(new JsonObject()
                        .put("userId", user.id())
                        .put("username", user.username()));

                    handler.handle(Future.succeededFuture(contextUser));
                })
                .onFailure(t -> {
                    handler.handle(Future.failedFuture(t));
                });
        };
    }

    /**
     * Instantiates a new {@link JWTAuth} which can be used directly as a request handler for vertx routes. Bearer
     * tokens will be passed to the JWTAuth where the signature will be validated against the RSA keypair gleaned by
     * this method.
     *
     * @param config the {@link Config.VmCentral} config containing the filenames for the public and private keypairs
     * @param vertx  {@link Vertx}
     *
     * @return a new {@link JWTAuth}
     */
    private JWTAuth newJwtAuthorizationProvider(final Config.VmCentral config, final Vertx vertx) {
        final KeyPair keyPair = getKeyPair(config);

        final var publicKeyPEM = publicKeyToPEM(keyPair.getPublic());
        final var privateKeyPEM = privateKeyToPEM(keyPair.getPrivate());

        return JWTAuth.create(vertx, new JWTAuthOptions()
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("RS256")
                .setBuffer(publicKeyPEM)
            )
            .addPubSecKey(new PubSecKeyOptions()
                .setAlgorithm("RS256")
                .setBuffer(privateKeyPEM)
            )
        );
    }

    /**
     * Generates, or retrieves, an RSA 2048-bit (256-byte) keypair for use in signing JWT tokens. If the filenames
     * referenced in the provided configuration do not exist (or do not contain any data) a new keypair will be
     * generated. Note that if this is done, and there are existing tokens in the wild, they will henceforth be
     * considered to be invalid! (This may be a handy way to invalidate all tokens if need be...)
     *
     * @param config the {@link Config.VmCentral} config containing the filenames for the public and private keypairs.
     *               If these files do not exist, or they are empty, a new keypair will be generated.
     *
     * @return the {@link KeyPair}
     */
    private KeyPair getKeyPair(final Config.VmCentral config) {

        final var algorithm = "RSA";
        final var keysize = 2048;

        final var publicKeyPath = Path.of(config.publicKeyFilename());
        final var privateKeyPath = Path.of(config.privateKeyFilename());

        try {
            if (noSuchFileOrFileIsEmpty(publicKeyPath) || noSuchFileOrFileIsEmpty(privateKeyPath)) {
                /*
                 * One or the other of the provided files is empty or nonexistent. Generate a new keypair and write the
                 * results to the provided file paths.
                 */
                final var keyPairGenerator = KeyPairGenerator.getInstance(algorithm);
                keyPairGenerator.initialize(keysize);

                final var keyPair = keyPairGenerator.generateKeyPair();

                final var publicKeyFile = publicKeyPath.toFile();
                final var privateKeyFile = privateKeyPath.toFile();

                try (final var fos = new FileOutputStream(publicKeyFile)) {
                    fos.write(keyPair.getPublic().getEncoded());
                }

                try (final var fos = new FileOutputStream(privateKeyFile)) {
                    fos.write(keyPair.getPrivate().getEncoded());
                }
            }

            /*
             * Read the keys from the filesystem (this may be unnecessary given we might have just generated them above,
             * but in order to be consistent, they should be read as if the files preexisted the call to this method.
             */
            final var keyFactory = KeyFactory.getInstance(algorithm);

            byte[] publicKeyBytes = Files.readAllBytes(publicKeyPath);
            byte[] privateKeyBytes = Files.readAllBytes(privateKeyPath);

            final var publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
            final var privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

            final var publicKey = keyFactory.generatePublic(publicKeySpec);
            final var privateKey = keyFactory.generatePrivate(privateKeySpec);

            return new KeyPair(publicKey, privateKey);
        }
        catch (final IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
            LOGGER.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean noSuchFileOrFileIsEmpty(final Path path) {
        final var file = path.toFile();
        return !file.exists() || file.length() <= 0;
    }

    /**
     * Converts the {@link PublicKey} into a PEM-file-compatible string.
     *
     * @param key the {@link PublicKey} to use as a source of bytes for the final string.
     *
     * @return a PEM-file-compatible public key {@link String}
     */
    private String publicKeyToPEM(final PublicKey key) {
        final var encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        return """
            -----BEGIN PUBLIC KEY-----
            %s
            -----END PUBLIC KEY-----
            """.formatted(encoded);
    }

    /**
     * Converts the {@link PrivateKey} into a PEM-file-compatible string.
     *
     * @param key the {@link PrivateKey} to use as a source of bytes for the final string.
     *
     * @return a PEM-file-compatible private key {@link String}
     */
    private String privateKeyToPEM(final PrivateKey key) {
        final var encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        return """
            -----BEGIN PRIVATE KEY-----
            %s
            -----END PRIVATE KEY-----
            """.formatted(encoded);
    }

    /**
     * The JWTAuth used to validate and generate tokens.
     */
    public JWTAuth getJwtAuth() {
        return this.jwtAuth;
    }

    /**
     * Create a new JWT for the provided user credentials. These credentials are passed directly to this AuthProvider's
     * {@link #authentication} for database lookup.
     */
    public Future<String> createToken(final UsernamePasswordCredentials credentials,
                                      final RoutingContext ctx) {
        return this.authentication.authenticate(credentials).map(user -> {

                /*
                 * Build a new JWT token using a JSON-ified user object and some JWTOptions.
                 */
                return getJwtAuth().generateToken(new JsonObject()
                        .put("userId", user.get("userId"))
                        .put("username", user.get("username")),
                    new JWTOptions()
                        .setAlgorithm("RS256")
                        .setExpiresInSeconds(TOKEN_EXPIRATION_MILLIS)

                );
            })
            .onFailure(ctx::fail);
    }

}
