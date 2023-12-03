package com.drewmalin.vm.central.security;

/**
 * A person or application that uses a security entity to perform an operation on a resource.
 */
public interface Principal {

    /**
     * The ID of this principal.
     *
     * @return the {@link String} ID of this principal
     */
    String id();

    /**
     * The role of this principal.
     *
     * @return the {@link Role} of this principal
     */
    Role role();
}
