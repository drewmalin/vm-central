package com.drewmalin.vm.central.data.repository;

public class SqlOperationException
    extends RuntimeException {

    public SqlOperationException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public SqlOperationException(final String message) {
        super(message);
    }
}
