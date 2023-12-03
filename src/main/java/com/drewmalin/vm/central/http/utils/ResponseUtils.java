package com.drewmalin.vm.central.http.utils;

import com.drewmalin.vm.central.task.UnauthorizedException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.HttpException;
import io.vertx.ext.web.validation.BadRequestException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.NoSuchElementException;

public class ResponseUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseUtils.class);

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    private ResponseUtils() {

    }

    public static void ok(final Object response, final RoutingContext routingContext) {
        response(200, Json.encodePrettily(response), routingContext);
    }

    public static void created(final Object response, final RoutingContext routingContext) {
        response(201, Json.encodePrettily(response), routingContext);
    }

    public static void accepted(final Object response, final RoutingContext routingContext) {
        response(202, Json.encodePrettily(response), routingContext);
    }

    public static void error(final Throwable t, final RoutingContext routingContext) {
        final int status;
        final String message;
        final String detail = t.getMessage();

        switch (t) {
            case BadRequestException e -> {
                status = 400;
                message = "Bad request";
            }
            case IllegalArgumentException e -> {
                status = 400;
                message = "Bad request";
            }
            case UnauthorizedException e -> {
                status = 401;
                message = "Unauthorized";
            }
            case NoSuchElementException e -> {
                status = 404;
                message = "Not found";
            }
            case HttpException e -> {
                status = e.getStatusCode();
                message = e.getMessage();
            }
            default -> {
                status = 500;
                message = "Internal Server Error";

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Unhandled error", t);
                }
            }
        }

        var error = new JsonObject()
            .put("error", message);

        if (StringUtils.isNotBlank(detail)) {
            error = error.put("message", detail);
        }

        final var errorMessage = error.encodePrettily();

        response(status, errorMessage, routingContext);
    }

    private static void response(final int status, final String response, final RoutingContext routingContext) {
        routingContext.response()
            .setStatusCode(status)
            .putHeader(HEADER_CONTENT_TYPE, APPLICATION_JSON)
            .end(response);
    }

}
