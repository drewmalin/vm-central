package com.drewmalin.vm.central.data.repository;

public interface Identifiable {

    /**
     * Returns the unique {@link String} ID of this {@link Identifiable}. This is considered to be unique across the
     * system.
     *
     * @return the unique {@link String} ID of this {@link Identifiable}
     */
    String getId();
}
