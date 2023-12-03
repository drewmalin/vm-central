package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.data.repository.VmRepositorySql;
import com.drewmalin.vm.central.security.Principal;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;

import java.util.List;
import java.util.stream.Collectors;

public class GetAllVmsTask
    extends Task<List<VmDTO>> {

    private final VmRepositorySql vmRepository;

    public GetAllVmsTask(final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);
        this.vmRepository = new VmRepositorySql(getSqlPool());
    }

    @Override
    public Future<List<VmDTO>> onSubmit(final Principal principal) {
        return this.vmRepository.getAll().map(allVms -> {

            return allVms.stream()
                .filter(vm -> isAuthorized(vm, principal))
                .collect(Collectors.toList());
        });
    }

    private boolean isAuthorized(final VmDTO vm, final Principal principal) {
        if (principal.role().equals(Role.ADMIN)) {
            return true;
        }
        return vm.owner().id().equals(principal.id());
    }

    public record Input(String provider) {

    }
}
