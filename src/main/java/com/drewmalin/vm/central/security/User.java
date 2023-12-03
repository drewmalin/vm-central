package com.drewmalin.vm.central.security;

public class User
    implements Principal {

    private final String id;
    private final Role role;

    public User(final String id, final Role role) {
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
