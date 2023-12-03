package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.data.model.VmDTO;
import com.drewmalin.vm.central.data.repository.IdUtils;
import com.drewmalin.vm.central.data.repository.VmRepositorySql;
import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.drewmalin.vm.central.vertical.VmCloudWorker.EVENT_VM_CREATE;

public class CreateVmTask
    extends Task<VmDTO> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CreateVmTask.class);
    private static final String ID_PREFIX = "VM:";

    private final Input input;
    private final VmRepositorySql vmRepository;

    public CreateVmTask(final Input input, final Pool sqlPool, final Vertx vertx) {
        super(sqlPool, vertx);

        this.input = input;
        this.vmRepository = new VmRepositorySql(getSqlPool());
    }

    @Override
    public Future<VmDTO> onSubmit(final Principal principal) {

        if (!isAuthorized(principal)) {
            throw new UnauthorizedException("user %s is unauthorized to perform 'vmRepository.insertVm'".formatted(principal.id()));
        }

        return getUserRepository().get(principal.id()).compose(user -> {

            final var newVm = VmDTO.builder()
                .id(ID_PREFIX + IdUtils.nextId())
                .provider(this.input.provider())
                .status(VmDTO.Status.INITIALIZING)
                .owner(user)
                .build();

            return this.vmRepository
                .add(newVm, true)
                .onSuccess(this::requestInstantiation);
        });
    }

    private void requestInstantiation(final VmDTO vm) {

        getVertx().eventBus().request(EVENT_VM_CREATE, vm, responseMessage -> {

            if (!responseMessage.succeeded()) {
                LOGGER.error("Failed to create VM");
                return;
            }

            if (!(responseMessage.result().body() instanceof VmDTO responseVm)) {
                LOGGER.error("Invalid payload");
                return;
            }

            this.vmRepository.put(responseVm)
                .onSuccess(updatedVm -> {
                    LOGGER.info("yay");
                });
        });
    }

    private boolean isAuthorized(final Principal principal) {
        return principal.role().equals(Role.ADMIN) || principal.role().equals(Role.USER);
    }

    public record Input(String provider) {

    }
}
