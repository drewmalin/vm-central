package com.drewmalin.vm.central.task;

import com.drewmalin.vm.central.data.model.UserDTO;
import com.drewmalin.vm.central.integrationtest.AbstractVertxTest;
import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.security.System;
import io.vertx.core.Vertx;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.typeCompatibleWith;
import static org.hamcrest.core.Is.is;

@Tag("integration")
public class UserTaskIntegrationTest
    extends AbstractVertxTest {

    private static final Principal PROCESSING_USER = System.TEST_ADMIN;

    @Test
    void test(final Vertx vertx, final VertxTestContext tc) {
        final var newUserCheckpoint = tc.checkpoint(1);
        final var getUserCheckpoint = tc.checkpoint(1);
        final var allUsersCheckpoint = tc.checkpoint(1);
        final var deleteUserCheckpoint = tc.checkpoint(1);
        final var getDeletedUserCheckpoint = tc.checkpoint(1);

        withServiceContext(vertx, tc, (ctx) -> {

            /*
             * Step 1: create a new user
             */

            final var createUserInput = new CreateUserTask.Input(
                "username",
                "password",
                "firstname",
                "lastname",
                Role.USER
            );

            Tasks.createUser(createUserInput, ctx).submit(PROCESSING_USER).onComplete(tc.succeeding(result -> {
                tc.verify(() -> {
                    assertThat(result.username(), is("username"));
                    assertThat(result.firstName(), is("firstname"));
                    assertThat(result.lastName(), is("lastname"));

                    newUserCheckpoint.flag();
                });
            })).flatMap(user -> {

                /*
                 * Step 2: retrieve the user by ID
                 */

                final var getUserInput = new GetUserTask.Input(
                    user.id()
                );

                return Tasks.getUser(getUserInput, ctx).submit(PROCESSING_USER).onComplete(tc.succeeding(result -> {
                    tc.verify(() -> {
                        assertThat(result.id(), is(user.id()));
                        assertThat(result.username(), is("username"));
                        assertThat(result.firstName(), is("firstname"));
                        assertThat(result.lastName(), is("lastname"));

                        getUserCheckpoint.flag();
                    });
                }));
            }).flatMap(user -> {

                /*
                 * Step 3: search for the user within the 'get all' response
                 */

                return Tasks.getAllUsers(ctx).submit(PROCESSING_USER).onComplete(tc.succeeding(result -> {
                    tc.verify(() -> {

                        assertThat(result.size(), is(greaterThanOrEqualTo(1)));

                        UserDTO foundUser = null;
                        for (final var userResult : result) {
                            if (userResult.id().equals(user.id())) {
                                foundUser = user;
                            }
                        }

                        if (foundUser != null) {
                            assertThat(foundUser.username(), is("username"));
                            assertThat(foundUser.firstName(), is("firstname"));
                            assertThat(foundUser.lastName(), is("lastname"));

                            allUsersCheckpoint.flag();
                        }
                        else {
                            tc.failNow("Failed to find user with id: %s".formatted(user.id()));
                        }
                    });
                })).map(ignored -> {
                    // We don't need the full list, so just pass the original user to the next Future
                    return user;
                });
            }).flatMap(user -> {

                /*
                 * Step 4: delete the user
                 */

                final var deleteUserInput = new DeleteUserTask.Input(
                    user.id()
                );

                return Tasks.deleteUser(deleteUserInput, ctx).submit(PROCESSING_USER).onComplete(tc.succeeding(result -> {
                    tc.verify(deleteUserCheckpoint::flag);
                })).map(ignored -> {
                    // Pass the original user to the next Future
                    return user;
                });
            }).flatMap(user -> {

                /*
                 * Step 5: attempt to retrieve the user, assert a failure
                 */
                final var getUserInput = new GetUserTask.Input(
                    user.id()
                );

                return Tasks.getUser(getUserInput, ctx).submit(PROCESSING_USER).onComplete(tc.failing(result -> {
                    tc.verify(() -> {
                        assertThat(result.getClass(), typeCompatibleWith(NoSuchElementException.class));
                        getDeletedUserCheckpoint.flag();
                    });
                }));
            });
        });
    }
}