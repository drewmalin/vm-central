package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import org.apache.commons.lang3.StringUtils;

public class UpdateUserTask
    extends Task<UserDTO> {

    private final Input input;
    private final UserRepository userRepository;

    public UpdateUserTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.userRepository = new UserRepositorySql(getSqlPool());
    }

    @Override
    public Future<UserDTO> onSubmit(final Principal principal) {
        if (!isAuthorized(this.input, principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'userRepository.update'".formatted(principal.id()));
        }

        return this.userRepository.get(this.input.userId())
            .flatMap(user -> {

                final var firstName = StringUtils.isEmpty(this.input.firstName())
                    ? user.firstName()
                    : this.input.firstName();

                final var lastName = StringUtils.isEmpty(this.input.lastName())
                    ? user.lastName()
                    : this.input.lastName();

                final var dto = UserDTO.builder(user)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();

                return this.userRepository.put(dto);
            });
    }

    private boolean isAuthorized(final Input input, final Principal principal) {
        if (principal.role().equals(Role.ADMIN)) {
            // Admin can do whatever they want
            return true;
        }

        // Not an admin! can only update themselves
        final var processingUserId = principal.id();
        if (processingUserId.equals(input.userId())) {
            return true;
        }

        return false;
    }

    public record Input(String userId, String firstName, String lastName) {

    }
}
