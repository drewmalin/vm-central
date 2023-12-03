package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.data.repository.VmRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

public class GetVmTask
    extends Task<VmDTO> {

    private final Input input;
    private final VmRepositorySql vmRepository;

    public GetVmTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.vmRepository = new VmRepositorySql(getSqlPool());
    }

    @Override
    public Future<VmDTO> onSubmit(final Principal principal) {
        return this.vmRepository.get(this.input.vmId).map(vm -> {

            if (!isAuthorized(vm, principal)) {
                throw new UnauthorizedException("user %s is unauthorized to perform 'vmRepository.get'".formatted(principal.id()));
            }

            return vm;
        });
    }

    private boolean isAuthorized(final VmDTO vm, final Principal principal) {
        if (principal.role().equals(Role.ADMIN)) {
            return true;
        }
        return vm.owner().id().equals(principal.id());
    }

    public record Input(String vmId) {

    }
}
