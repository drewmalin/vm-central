package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

import java.util.List;

public class GetAllUsersTask
    extends Task<List<UserDTO>> {

    private final UserRepository userRepository;

    public GetAllUsersTask(final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);
        this.userRepository = new UserRepositorySql(getSqlPool());
    }

    @Override
    public Future<List<UserDTO>> onSubmit(final Principal principal) {
        if (!isAuthorized(principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'userRepository.selectAll'".formatted(principal.id()));
        }

        return this.userRepository.getAll();
    }

    private boolean isAuthorized(final Principal principal) {
        // Admin only!
        return principal.role().equals(Role.ADMIN);
    }
}
