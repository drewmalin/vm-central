package com.drewmalin.vm.central;

import com.drewmalin.vm.central.http.model.TokenHttpCreateResponse;
import com.drewmalin.vm.central.http.model.UserHttpResponse;
import io.restassured.http.ContentType;
import io.vertx.core.json.JsonObject;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static io.restassured.RestAssured.given;
import static io.restassured.RestAssured.when;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

@Tag("system")
public class SimpleSystemTest {

    private static final String ENDPOINT = "http://localhost:9876";

    private static final Random RANDOM = new Random();

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "vmcentral";

    @Test
    void loginFlow() {
        /*
         * Initially, no token means we should get a 401
         */
        when()
            .get(ENDPOINT + "/users")
            .then()
            .statusCode(401);

        /*
         * Request a token
         */
        final var tokenPayload = new JsonObject();
        tokenPayload.put("username", ADMIN_USERNAME);
        tokenPayload.put("password", ADMIN_PASSWORD);

        final var tokenResponse = given()
            .contentType(ContentType.JSON)
            .body(tokenPayload.toString())
            .when()
            .post(ENDPOINT + "/token");

        assertThat(tokenResponse.statusCode(), is(200));

        final var adminToken = tokenResponse.body()
            .as(TokenHttpCreateResponse.class)
            .token();

        /*
         * Create a user
         */
        final var userPassword = "password";
        final var randomNumber = RANDOM.nextInt();
        final var createUserPayload = new JsonObject();
        createUserPayload.put("username", "test-%d".formatted(randomNumber));
        createUserPayload.put("password", userPassword);
        createUserPayload.put("firstName", "test");
        createUserPayload.put("lastName", "test");

        final var createUserResponse = given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer %s".formatted(adminToken))
            .body(createUserPayload.toString())
            .when()
            .post(ENDPOINT + "/users");

        assertThat(createUserResponse.statusCode(), is(201));

        final var user = createUserResponse.body()
            .as(UserHttpResponse.class);

        assertThat(user.username(), is(createUserPayload.getString("username")));

        /*
         * Log in with the user, attempt 1 (bad password)
         */
        final var userTokenPayload = new JsonObject();
        userTokenPayload.put("username", user.username());
        userTokenPayload.put("password", "this is not the password!!!");

        given()
            .contentType(ContentType.JSON)
            .body(userTokenPayload.toString())
            .when()
            .post(ENDPOINT + "/token")
            .then()
            .statusCode(401);

        /*
         * Log in with the user, attempt 2 (bad password)
         */
        userTokenPayload.put("username", user.username());
        userTokenPayload.put("password", userPassword);

        final var userTokenResponse = given()
            .contentType(ContentType.JSON)
            .body(userTokenPayload.toString())
            .when()
            .post(ENDPOINT + "/token");

        assertThat(userTokenResponse.statusCode(), is(200));

        final var userToken = userTokenResponse.body()
            .as(TokenHttpCreateResponse.class)
            .token();

        /*
         * Use the token
         */
        given()
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer %s".formatted(userToken))
            .when()
            .get(ENDPOINT + "/users/%s".formatted(user.id()))
            .then()
            .statusCode(200);
    }
}
