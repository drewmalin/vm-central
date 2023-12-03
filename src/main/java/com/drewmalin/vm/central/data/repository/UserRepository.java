package com.drewmalin.vm.central.data.repository;

import com.drewmalin.vm.central.data.model.UserDTO;
import io.vertx.core.Future;

/**
 * A repository of {@link UserDTO}s.
 */
public interface UserRepository
    extends Repository<UserDTO> {

    /**
     * Gets the {@link UserDTO} identified by the provided credentials. If no user exists, the resultant future will be marked as having failed.
     *
     * @param username the {@link String} username
     * @param password the {@link String} plaintext password
     *
     * @return a {@link Future<UserDTO>} containing the user
     */
    Future<UserDTO> get(String username, String password);


}
