package com.drewmalin.vm.central.security;

public enum System
    implements Principal {

    /**
     * The system root. Identifies operations performed during system startup and shutdown.
     */
    ROOT(
        "PRINCIPAL:SYSTEM:ROOT",
        Role.ADMIN
    ),

    /**
     * The test admin system.
     */
    TEST_ADMIN(
        "PRINCIPAL:SYSTEM:TEST",
        Role.ADMIN
    ),

    /**
     * The test user system.
     */
    TEST_USER(
        "PRINCIPAL:SYSTEM:TEST",
        Role.USER
    ),

    /**
     * The Cloud Worker system.
     */
    CLOUD_WORKER(
        "PRINCIPAL:SYSTEM:CLOUD_WORKER",
        Role.ADMIN
    ),

    /**
     * Background Jobs.
     */
    BACKGROUND_JOB(
        "PRINCIPAL:SYSTEM:BACKGROUND_JOB",
        Role.ADMIN
    ),
    ;

    private final String id;
    private final Role role;

    System(final String id, final Role role) {
        this.id = id;
        this.role = role;
    }

    @Override
    public String id() {
        return this.id;
    }

    @Override
    public Role role() {
        return this.role;
    }
}
