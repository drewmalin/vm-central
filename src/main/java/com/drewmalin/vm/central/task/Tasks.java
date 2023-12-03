package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.model.VmDTO;
import io.vertx.core.Vertx;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class Tasks {

    private static final Logger LOGGER = LoggerFactory.getLogger(Tasks.class);

    private Tasks() {

    }

    public static Task<UserDTO> getUser(final GetUserTask.Input input, final ServiceContext context) {
        return new GetUserTask(input, context.sqlPool(), context.vertx());
    }

    public static Task<List<UserDTO>> getAllUsers(final ServiceContext context) {
        return new GetAllUsersTask(context.sqlPool(), context.vertx());
    }

    public static Task<UserDTO> createUser(final CreateUserTask.Input input, final ServiceContext context) {
        return createUser(input, context.sqlPool(), context.vertx());
    }

    public static Task<UserDTO> createUser(final CreateUserTask.Input input, final Pool sqlPool, final Vertx vertx) {
        return new CreateUserTask(input, sqlPool, vertx);
    }

    public static Task<Void> deleteUser(final DeleteUserTask.Input input, final ServiceContext context) {
        return new DeleteUserTask(input, context.sqlPool(), context.vertx());
    }

    public static Task<List<VmDTO>> getAllVms(final ServiceContext context) {
        return new GetAllVmsTask(context.sqlPool(), context.vertx());
    }

    public static Task<List<VmDTO>> bulkUpdateVms(final BulkUpdateVmsTask.Input input, final ServiceContext context) {
        return new BulkUpdateVmsTask(input, context.sqlPool(), context.vertx());
    }
}
