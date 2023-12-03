package com.drewmalin.vm.central.http.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmHttpResponse(
    String id,
    String provider,
    String status,
    String ownerId
) {

}
