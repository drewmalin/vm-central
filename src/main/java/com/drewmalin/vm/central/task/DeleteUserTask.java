package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import com.drewmalin.vm.central.security.Principal;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

public class DeleteUserTask
    extends Task<Void> {

    private final Input input;
    private final UserRepository userRepository;

    public DeleteUserTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.userRepository = new UserRepositorySql(getSqlPool());
    }

    @Override
    public Future<Void> onSubmit(final Principal principal) {

        if (!isAuthorized(this.input, principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'userRepository.delete'".formatted(principal.id()));
        }

        return this.userRepository.delete(this.input.userId());
    }

    private boolean isAuthorized(final Input input, final Principal principal) {
        if (input.userId().equals(principal.id())) {
            // Nobody can delete themselves
            return false;
        }

        if (principal.role().equals(Role.ADMIN)) {
            // Admin can do whatever they want
            return true;
        }

        // Not an admin!
        return false;
    }

    public record Input(String userId) {

    }
}
