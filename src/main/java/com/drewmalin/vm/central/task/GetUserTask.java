package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

public class GetUserTask
    extends Task<UserDTO> {

    private final Input input;
    private final UserRepository userRepository;

    public GetUserTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.userRepository = new UserRepositorySql(getSqlPool());
    }

    @Override
    public Future<UserDTO> onSubmit(final Principal principal) {

        if (!isAuthorized(this.input, principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'userRepository.selectAll'".formatted(principal.id()));
        }

        return this.userRepository.get(this.input.userId());
    }

    private boolean isAuthorized(final Input input, final Principal principal) {
        if (principal.role().equals(Role.ADMIN)) {
            // Admin can do whatever they want
            return true;
        }

        // Not an admin! can only see themselves
        final var processingUserId = principal.id();
        if (processingUserId.equals(input.userId())) {
            return true;
        }

        return false;
    }

    /**
     * Input for the {@link GetUserTask}
     *
     * @param userId the int user ID
     */
    public record Input(String userId) {

    }
}
