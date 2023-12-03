package com.drewmalin.vm.central.data.repository;

import com.drewmalin.vm.central.http.auth.Crypto;
import com.drewmalin.vm.central.task.UnauthorizedException;
import com.drewmalin.vm.central.data.model.UserDTO;
import io.vertx.core.Future;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.RowIterator;
import io.vertx.sqlclient.templates.RowMapper;
import io.vertx.sqlclient.templates.SqlTemplate;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

public class UserRepositorySql
    implements UserRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositorySql.class);

    private final Pool sqlPool;

    public UserRepositorySql(final Pool sqlPool) {
        this.sqlPool = sqlPool;
    }

    @Override
    public Future<List<UserDTO>> getAll() {
        final var query = """
            SELECT
                u.user_pk,
                u.user_id,
                u.username,
                u.hashed_password,
                u.salt,
                u.first_name,
                u.last_name,
                u.role_id
            FROM users AS u
            """;

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forQuery(sqlConnection, query)
            .mapTo(UserDTO.class)
            .execute(Collections.emptyMap())
            .map(rowSet -> {
                final List<UserDTO> users = new ArrayList<>();
                for (final UserDTO user : rowSet) {
                    users.add(user);
                }
                return users;
            }));
    }

    @Override
    public Future<UserDTO> add(final UserDTO user, final boolean ensureUnique) {
        final var subQuery = ensureUnique
            ? ""
            : "ON CONFLICT (username) DO NOTHING";

        final var query = """
            INSERT INTO users (
                user_id,
                username,
                hashed_password,
                salt,
                first_name,
                last_name,
                role_id
            )
            VALUES (
                #{user_id},
                #{username},
                #{hashed_password},
                #{salt},
                #{first_name},
                #{last_name},
                #{role_id}
            )
            %s
            RETURNING
                user_pk,
                user_id,
                username,
                hashed_password,
                salt,
                first_name,
                last_name,
                role_id
            """.formatted(subQuery);

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("user_id", user.id());
        parameters.put("username", user.username());
        parameters.put("hashed_password", user.hashedPassword());
        parameters.put("salt", user.salt());
        parameters.put("first_name", user.firstName());
        parameters.put("last_name", user.lastName());
        parameters.put("role_id", user.roleId());

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .mapTo(UserDTO.class)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<UserDTO> iterator = rowSet.iterator();

                if (iterator.hasNext()) {
                    return iterator.next();
                }
                else if (ensureUnique) {
                    /*
                     * No SQL error, but no user created -- caller has indicated that an exception should be thrown
                     * if a duplicate was encountered.
                     */
                    throw new IllegalArgumentException("");
                }
                else {
                    /*
                     * No SQL error, no user creation, but the caller wants to suppress duplication errors. Simply
                     * return the original user DTO.
                     */
                    return user;
                }
            })
        );
    }

    @Override
    public Future<UserDTO> put(final UserDTO user) {
        final var query = """
            UPDATE users
            SET
                first_name = #{first_name},
                last_name = #{last_name}
            WHERE user_pk = #{pk}
            RETURNING
                user_pk,
                user_id,
                username,
                hashed_password,
                salt,
                first_name,
                last_name,
                role_id
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("pk", user.pk());
        parameters.put("first_name", user.firstName());
        parameters.put("last_name", user.lastName());

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .mapTo(UserDTO.class)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<UserDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                else {
                    throw new SqlOperationException("failed to insert user %s".formatted(user.pk()));
                }
            })
            .onFailure(t -> {
                LOGGER.error(t.getMessage());
            }));
    }

    @Override
    public Future<List<UserDTO>> putAll(final List<UserDTO> identifiables) {
        throw new NotImplementedException();
    }

    @Override
    public Future<UserDTO> get(final String id) {
        final var query = """
            SELECT
                u.user_pk,
                u.user_id,
                u.username,
                u.hashed_password,
                u.salt,
                u.first_name,
                u.last_name,
                u.role_id
            FROM users AS u
            WHERE u.user_id = #{id}
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);

        final RowMapper<UserDTO> mapper = row -> {
            return UserDTO.builder()
                .pk(row.getInteger("user_pk"))
                .id(row.getString("user_id"))
                .username(row.getString("username"))
                .hashedPassword(row.getBuffer("hashed_password").getBytes())
                .salt(row.getBuffer("salt").getBytes())
                .firstName(row.getString("first_name"))
                .lastName(row.getString("last_name"))
                .roleId(row.getString("role_id"))
                .build();
        };

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forQuery(sqlConnection, query)
            .mapTo(mapper)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<UserDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                else {
                    throw new NoSuchElementException();
                }
            }));
    }

    @Override
    public Future<Void> delete(final String id) {
        final var query = """
            DELETE FROM users
            WHERE user_id = #{id}
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("id", id);

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forUpdate(sqlConnection, query)
            .execute(parameters)
            .map(result -> (Void) null)
            .onFailure(t -> {
                LOGGER.error(t.getMessage());
            }));
    }

    @Override
    public Future<UserDTO> get(final String username,
                               final String password) {
        final var query = """
            SELECT
                u.user_pk,
                u.user_id,
                u.username,
                u.hashed_password,
                u.salt,
                u.first_name,
                u.last_name,
                u.role_id
            FROM users AS u
            WHERE u.username = #{username}
            """;

        final Map<String, Object> parameters = new HashMap<>();
        parameters.put("username", username);

        final RowMapper<UserDTO> mapper = row -> {
            return UserDTO.builder()
                .pk(row.getInteger("user_pk"))
                .id(row.getString("user_id"))
                .username(row.getString("username"))
                .hashedPassword(row.getBuffer("hashed_password").getBytes())
                .salt(row.getBuffer("salt").getBytes())
                .firstName(row.getString("first_name"))
                .lastName(row.getString("last_name"))
                .roleId(row.getString("role_id"))
                .build();
        };

        return this.sqlPool.withConnection(sqlConnection -> SqlTemplate.forQuery(sqlConnection, query)
            .mapTo(mapper)
            .execute(parameters)
            .map(rowSet -> {
                final RowIterator<UserDTO> iterator = rowSet.iterator();
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                else {
                    throw new NoSuchElementException("No record for username: %s".formatted(username));
                }
            })
            .map(user -> {
                /*
                 * This user has the right username, but we now need to verify that the provided password hashes to the
                 * stored value. Use the stored salt and the provided password to generate the hash.
                 */
                final var salt = user.salt();
                final var encodedPassword = Crypto.builder(password).salt(salt).build();

                if (!Arrays.equals(user.hashedPassword(), encodedPassword.hash())) {
                    throw new UnauthorizedException("Invalid username/password combination");
                }

                return user;
            }));

    }
}
