package com.drewmalin.vm.central.http.model;

public record UserHttpCreateRequest(
    String username,
    String password,
    String firstName,
    String lastName
) {

}
