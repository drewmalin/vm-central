package com.drewmalin.vm.central.http.router;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.http.auth.AuthProvider;
import com.drewmalin.vm.central.http.model.VmHttpCreateRequest;
import com.drewmalin.vm.central.http.model.VmHttpGetAllResponse;
import com.drewmalin.vm.central.http.model.VmHttpResponse;
import com.drewmalin.vm.central.http.utils.ResponseUtils;
import com.drewmalin.vm.central.task.CreateVmTask;
import com.drewmalin.vm.central.task.GetAllVmsTask;
import com.drewmalin.vm.central.task.GetVmTask;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.JWTAuthHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import io.vertx.sqlclient.Pool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static io.vertx.ext.web.validation.RequestPredicate.BODY_REQUIRED;
import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.ext.web.validation.builder.Parameters.param;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class VirtualMachinesRouter
    extends RequestRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(VirtualMachinesRouter.class);

    private static final String HTTP_VMS_PATH = "/vms";

    private final AuthProvider authProvider;
    private final Pool sqlPool;

    private VirtualMachinesRouter(final AuthProvider authProvider,
                                  final Pool sqlPool,
                                  final Router router,
                                  final Vertx vertx) {
        super(router, vertx);

        this.authProvider = authProvider;
        this.sqlPool = sqlPool;
    }

    public static VirtualMachinesRouter create(final ServiceContext ctx) {
        return new VirtualMachinesRouter(ctx.auth(), ctx.sqlPool(), ctx.router(), ctx.vertx());
    }

    @Override
    public void mount() {
        LOGGER.info("Mounting new HTTP route: '%s'".formatted(HTTP_VMS_PATH));

        /*
         * GET /vms
         */
        getRouter().route(HttpMethod.GET, HTTP_VMS_PATH)
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ctx -> {

                new GetAllVmsTask(this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(vms -> {

                        final var allVms = new ArrayList<VmHttpResponse>();
                        for (final var vm : vms) {
                            final var user = vm.owner();
                            allVms.add(new VmHttpResponse(vm.id(), vm.providerName(), vm.statusName(), user.id()));
                        }
                        final var response = new VmHttpGetAllResponse(allVms.size(), allVms);

                        ResponseUtils.ok(response, ctx);
                    });
            });

        /*
         * GET /vms/:vmID
         */
        getRouter().route(HttpMethod.GET, HTTP_VMS_PATH + "/:vmID")
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .pathParameter(param("vmID", stringSchema()))
                .build()
            )
            .handler(ctx -> {
                final var vmId = ctx.pathParam("vmID");

                final var input = new GetVmTask.Input(vmId);
                new GetVmTask(input, this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(vm -> {

                        final var response = new VmHttpResponse(vm.id(), vm.providerName(), vm.statusName(), vm.owner().id());

                        ResponseUtils.ok(response, ctx);
                    });
            });

        /*
         * POST /vms
         */
        getRouter().route(HttpMethod.POST, HTTP_VMS_PATH)
            .handler(BodyHandler.create())
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .predicate(BODY_REQUIRED)
                .body(json(objectSchema()
                    .requiredProperty("provider", stringSchema())
                ))
                .build()
            )
            .handler(ctx -> {
                final var payload = ctx.body().asJsonObject().mapTo(VmHttpCreateRequest.class);

                final var input = new CreateVmTask.Input(
                    payload.provider()
                );

                new CreateVmTask(input, this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess
                        (vm -> {

                        final var user = vm.owner();
                        final var response = new VmHttpResponse(vm.id(), vm.providerName(), vm.statusName(), user.id());

                        ResponseUtils.created(response, ctx);
                    });
            });
    }
}
