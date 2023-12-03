package com.drewmalin.vm.central.http.model;

import java.util.List;

public record VmHttpGetAllResponse(
    int size,
    List<VmHttpResponse> vms
) {

}
