package com.drewmalin.vm.central.http.model;

import java.util.List;

public record UserHttpGetAllResponse(int total, List<UserHttpResponse> users) {

}
