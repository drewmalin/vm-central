package com.drewmalin.vm.central.http.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmHttpCreateRequest(
    String provider
) {

}
