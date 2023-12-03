package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.data.repository.VmRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

import java.util.List;

public class BulkUpdateVmsTask
    extends Task<List<VmDTO>> {

    private final Input input;
    private final VmRepositorySql vmRepository;

    BulkUpdateVmsTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);
        this.input = input;
        this.vmRepository = new VmRepositorySql(getSqlPool());
    }

    @Override
    public Future<List<VmDTO>> onSubmit(final Principal principal) {
        if (!isAuthorized(principal)) {
            throw new UnauthorizedException("principal %s is unauthorized to perform 'vmRepository.updateVms'".formatted(principal.id()));
        }

        if (this.input.vms().isEmpty()) {
            // nothing to do!
            return Future.succeededFuture();
        }

        return this.vmRepository.putAll(this.input.vms).map(updatedVms -> {
            return updatedVms;
        });
    }

    private boolean isAuthorized(final Principal principal) {
        return principal.role().equals(Role.ADMIN);
    }

    public record Input(List<VmDTO> vms) {

    }
}
