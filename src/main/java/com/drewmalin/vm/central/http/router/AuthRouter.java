package com.drewmalin.vm.central.http.router;

import com.drewmalin.vm.central.context.ServiceContext;
import com.drewmalin.vm.central.http.utils.ResponseUtils;
import com.drewmalin.vm.central.http.auth.AuthProvider;
import com.drewmalin.vm.central.http.model.TokenHttpCreateResponse;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.authentication.UsernamePasswordCredentials;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.validation.builder.ValidationHandlerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vertx.ext.web.validation.RequestPredicate.BODY_REQUIRED;
import static io.vertx.ext.web.validation.builder.Bodies.json;
import static io.vertx.json.schema.common.dsl.Schemas.objectSchema;
import static io.vertx.json.schema.common.dsl.Schemas.stringSchema;

public class AuthRouter
    extends RequestRouter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthRouter.class);

    private static final String HTTP_TOKEN_PATH = "/token";

    private final AuthProvider authProvider;

    private AuthRouter(final AuthProvider authProvider, final Router router, final Vertx vertx) {
        super(router, vertx);

        this.authProvider = authProvider;
    }

    public static AuthRouter create(ServiceContext ctx) {
        return new AuthRouter(ctx.auth(), ctx.router(), ctx.vertx());
    }

    @Override
    public void mount() {
        LOGGER.info("Mounting new HTTP route: '%s'".formatted(HTTP_TOKEN_PATH));

        getRouter().route(HttpMethod.POST, HTTP_TOKEN_PATH)
            .handler(BodyHandler.create())
            .handler(ValidationHandlerBuilder.create(getSchemaParser())
                .predicate(BODY_REQUIRED)
                .body(json(objectSchema()
                    .requiredProperty("username", stringSchema())
                    .requiredProperty("password", stringSchema())
                ))
                .build()
            )
            .handler(ctx -> {
                final var credentials = ctx.body().asJsonObject().mapTo(UsernamePasswordCredentials.class);

                this.authProvider.createToken(credentials, ctx)
                    .onSuccess(token -> {

                        final var response = new TokenHttpCreateResponse(token);

                        ResponseUtils.ok(response, ctx);
                    });
            });
    }
}
