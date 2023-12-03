package com.drewmalin.vm.central.data.repository;

public class NoSuchRecordException
    extends SqlOperationException {

    public NoSuchRecordException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
