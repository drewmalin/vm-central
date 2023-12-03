package com.drewmalin.vm.central.security;

public enum Role {

    ADMIN("ROLE:ADMIN"),

    USER("ROLE:USER"),
    ;

    private final String id;

    Role(final String id) {
        this.id = id;
    }

    public static Role forId(final String id) {
        for (final var role : values()) {
            if (role.id.equals(id)) {
                return role;
            }
        }
        throw new IllegalArgumentException("No role exists for ID: %s".formatted(id));
    }

    public String getId() {
        return this.id;
    }
}
