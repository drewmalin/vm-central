package com.drewmalin.vm.central.http.router;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.http.auth.AuthProvider;
import com.drewmalin.vm.central.http.model.UserHttpCreateRequest;
import com.drewmalin.vm.central.http.model.UserHttpGetAllResponse;
import com.drewmalin.vm.central.http.model.UserHttpModifyRequest;
import com.drewmalin.vm.central.http.model.UserHttpResponse;
import com.drewmalin.vm.central.http.utils.ResponseUtils;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.task.CreateUserTask;
import com.drewmalin.vm.central.task.DeleteUserTask;
import com.drewmalin.vm.central.task.GetAllUsersTask;
import com.drewmalin.vm.central.task.GetUserTask;
import com.drewmalin.vm.central.task.UpdateUserTask;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
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

public class UsersRouter
    extends RequestRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HealthCheckRouter.class);

    private static final String HTTP_USERS_PATH = "/users";

    private final AuthProvider authProvider;
    private final Pool sqlPool;

    private UsersRouter(final AuthProvider authProvider, final Pool sqlPool, final Router router, final Vertx vertx) {
        super(router, vertx);

        this.authProvider = authProvider;
        this.sqlPool = sqlPool;
    }

    public static UsersRouter create(final ServiceContext ctx) {
        return new UsersRouter(ctx.auth(), ctx.sqlPool(), ctx.router(), ctx.vertx());
    }

    @Override
    public void mount() {
        LOGGER.info("Mounting new HTTP route: '%s'".formatted(HTTP_USERS_PATH));

        /*
         * GET /users
         */
        getRouter().route(HttpMethod.GET, HTTP_USERS_PATH)
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ctx -> {

                new GetAllUsersTask(this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(users -> {

                        final var allUsers = new ArrayList<UserHttpResponse>();
                        for (final var user : users) {
                            allUsers.add(new UserHttpResponse(user.id(), user.username()));
                        }
                        final var response = new UserHttpGetAllResponse(allUsers.size(), allUsers);

                        ResponseUtils.ok(response, ctx);
                    });
            });

        /*
         * POST /users
         */
        getRouter().route(HttpMethod.POST, HTTP_USERS_PATH)
            .handler(BodyHandler.create())
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .predicate(BODY_REQUIRED)
                .body(json(objectSchema()
                    .requiredProperty("username", stringSchema())
                    .requiredProperty("password", stringSchema())
                    .requiredProperty("firstName", stringSchema())
                    .requiredProperty("lastName", stringSchema())
                ))
                .build()
            )
            .handler(ctx -> {
                final var payload = ctx.body().asJsonObject().mapTo(UserHttpCreateRequest.class);

                final var input = new CreateUserTask.Input(
                    payload.username(),
                    payload.password(),
                    payload.firstName(),
                    payload.lastName(),
                    Role.USER
                );

                new CreateUserTask(input, this.sqlPool, getVertx())
                    .submit(ctx)
                    .onSuccess(user -> {
                        final var response = new UserHttpResponse(user.id(), user.username());
                        ResponseUtils.created(response, ctx);
                    });
            });

        /*
         * GET /users/:userID
         */
        getRouter().route(HttpMethod.GET, HTTP_USERS_PATH + "/:userID")
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .pathParameter(param("userID", stringSchema()))
                .build()
            )
            .handler(ctx -> {
                final var userId = ctx.pathParam("userID");

                final var input = new GetUserTask.Input(userId);
                new GetUserTask(input, this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(user -> {
                        final var response = new UserHttpResponse(user.id(), user.username());
                        ResponseUtils.ok(response, ctx);
                    });
            });

        /*
         * PUT /users/:userID
         */
        getRouter().route(HttpMethod.PUT, HTTP_USERS_PATH + "/:userID")
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .predicate(BODY_REQUIRED)
                .pathParameter(param("userID", stringSchema()))
                .body(json(objectSchema()
                    .requiredProperty("firstName", stringSchema())
                    .requiredProperty("lastName", stringSchema())
                ))
                .build()
            )
            .handler(ctx -> {
                final var userID = ctx.pathParam("userID");
                final var payload = ctx.body().asJsonObject().mapTo(UserHttpModifyRequest.class);

                final var input = new UpdateUserTask.Input(
                    userID,
                    payload.firstName(),
                    payload.lastName());

                new UpdateUserTask(input, this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(dto -> {
                        final var response = new UserHttpResponse(dto.id(), dto.username());
                        ResponseUtils.ok(response, ctx);
                    });
            });

        /*
         * DELETE /users/:userID
         */
        getRouter().route(HttpMethod.DELETE, HTTP_USERS_PATH + "/:userID")
            .handler(JWTAuthHandler.create(this.authProvider.getJwtAuth()))
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .pathParameter(param("userID", stringSchema()))
                .build()
            )
            .handler(ctx -> {
                final var userID = ctx.pathParam("userID");

                final var input = new DeleteUserTask.Input(userID);

                new DeleteUserTask(input, this.sqlPool, getVertx()).submit(ctx)
                    .onSuccess(ignored -> {
                        ResponseUtils.ok(new JsonObject(), ctx);
                    });
            });

    }
}
