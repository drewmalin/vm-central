package com.drewmalin.vm.central.data.repository;

import io.vertx.core.Future;

import java.util.List;

/**
 * A repository of {@link Identifiable}s.
 *
 * @param <T> the type of {@link Identifiable} contained in this repository.
 */
public interface Repository<T extends Identifiable> {

    /**
     * Gets all identifiables in this repository.
     *
     * @return a {@link Future<List<T>>} containing all identifiables
     */
    Future<List<T>> getAll();

    /**
     * Adds the identifiable to this repository. If the identifiable already exists in this repository, this operation
     * no-ops. If the identifiable already exists in the repository and 'ensureUnique' is set to true, an exception
     * is thrown. Implementers may alter the provided identifiable (e.g. to set other identifying information such
     * as database primary keys) and so the resultant identifiable is not guaranteed to be precisely equal to the input.
     *
     * @param identifiable the {@link Identifiable} to add to this repository
     * @param ensureUnique if true, the provided identifiable must not already exist in this repository
     *
     * @return a {@link Future<T>} containing the added identifiable
     */
    Future<T> add(T identifiable, boolean ensureUnique);

    /**
     * Puts the identifiable into this repository. If the identifiable already exists in this repository, this operation
     * updates the identifiable, essentially replacing the previous entry with this one. If the identifiable did not
     * already exist in this repository, this operation behaves the same as {@link #add(Identifiable, boolean)}.
     * Implementers may alter the provided identifiable (e.g. to set other identifying information such as database
     * primary keys) and so the resultant identifiable is not guaranteed to be precisely equal to the input.
     *
     * @param identifiable the {@link Identifiable} to put into this repository
     *
     * @return a {@link Future<T>} containing the identifiable
     */
    Future<T> put(T identifiable);

    /**
     * Puts the identifiables into this repository. If any identifiable already exists in this repository, this operation
     * updates the identifiable, essentially replacing the previous entry with this one. If the identifiable did not
     * already exist in this repository, this operation behaves the same as {@link #add(Identifiable, boolean)}.
     * Implementers may alter the provided identifiable (e.g. to set other identifying information such as database
     * primary keys) and so the resultant identifiable is not guaranteed to be precisely equal to the input.
     *
     * @param identifiables the {@link List<Identifiable>} to put into this repository
     *
     * @return a {@link Future<List<T>>} containing the identifiables
     */
    Future<List<T>> putAll(List<T> identifiables);

    /**
     * Gets the identifiable identified by the provided ID from this repository. If no identifiable exists, the
     * resultant future will be marked as having failed.
     *
     * @param id the {@link String} ID of the identifiable to return
     *
     * @return a {@link Future<T>} containing the identifiable
     */
    Future<T> get(String id);

    /**
     * Deletes the identifiable identified by the provided ID from this repository. If no identifiable exists, the
     * resultant future will be marked as having failed.
     *
     * @param id the {@link String} ID of the identifiable to delete
     *
     * @return a {@link Future<Void>}
     */
    Future<Void> delete(String id);
}
