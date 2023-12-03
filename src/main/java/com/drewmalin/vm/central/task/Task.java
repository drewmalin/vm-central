package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.data.repository.UserRepository;
import com.drewmalin.vm.central.data.repository.UserRepositorySql;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.ext.web.RoutingContext;
import io.vertx.sqlclient.Pool;

public abstract class Task<T> {

    private static final String CTX_USER_ID = "userId";

    private final Pool sqlPool;
    private final Vertx vertx;
    private final UserRepositorySql userRepository;

    Task(final Pool sqlPool, final Vertx vertx) {
        this.sqlPool = sqlPool;
        this.vertx = vertx;

        this.userRepository = new UserRepositorySql(this.sqlPool);
    }

    /**
     * Submits this task for processing using a {@link RoutingContext} to finalize the resultant {@link Future}.
     */
    public Future<T> submit(final RoutingContext ctx) {
        return this.userRepository
            .get(ctx.user().get(CTX_USER_ID))
            .map(UserDTO::toPrincipal)
            .compose(this::submit)
            .onFailure(ctx::fail);
    }

    /**
     * Submits this task with a "known" internal user (e.g. a system user, the "job" system itself, etc.). Note that
     * this skips any fetching of the user from the database, so can be used to circumvent security!
     */
    public Future<T> submit(final Principal principal) {
        return onSubmit(principal);
    }

    public abstract Future<T> onSubmit(Principal principal);

    Pool getSqlPool() {
        return this.sqlPool;
    }

    Vertx getVertx() {
        return this.vertx;
    }

    UserRepository getUserRepository() {
        return this.userRepository;
    }
}
