package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.repository.IdUtils;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import com.drewmalin.vm.central.http.auth.Crypto;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

public class CreateUserTask
    extends Task<UserDTO> {

    private static final String ID_PREFIX = "USER:";

    private final Input input;
    private final UserRepository userRepository;

    public CreateUserTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.userRepository = new UserRepositorySql(getSqlPool());
    }

    @Override
    public Future<UserDTO> onSubmit(final Principal principal) {
        if (!isAuthorized(principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'userRepository.insertIdentity'".formatted(principal.id()));
        }

        final var tempCreds = Crypto.encode(this.input.password());

        final var newUser = UserDTO.builder()
            .id(ID_PREFIX + IdUtils.nextId())
            .username(this.input.username())
            .firstName(this.input.firstName())
            .lastName(this.input.lastName())
            .hashedPassword(tempCreds.hash())
            .salt(tempCreds.salt())
            .roleId(this.input.role.getId())
            .build();

        return this.userRepository.add(newUser, this.input.failOnDuplicate);
    }

    private boolean isAuthorized(final Principal principal) {
        // Admin only!
        return principal.role().equals(Role.ADMIN);
    }

    /**
     * Input for {@link CreateUserTask}.
     *
     * @param username        the {@link String} username
     * @param password        the {@link String} raw password (the task will take care of hashing)
     * @param firstName       the {@link String} first name
     * @param lastName        the {@link String} last name
     * @param failOnDuplicate if true, and if this operation produces a duplicate user, an error will be generated
     */
    public record Input(String username,
                        String password,
                        String firstName,
                        String lastName,
                        Role role,
                        boolean failOnDuplicate) {

        public Input(String username,
                     String password,
                     String firstName,
                     String lastName,
                     Role role) {
            this(username, password, firstName, lastName, role, true);
        }
    }
}
