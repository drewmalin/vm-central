package com.drewmalin.vm.central.data.model;

import com.drewmalin.vm.central.data.repository.Identifiable;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VmDTO(
    @JsonProperty("virtual_machine_pk")
    int pk,
    @JsonProperty("virtual_machine_id")
    String id,
    @JsonProperty("provider")
    String providerName,
    @JsonIgnore
    Provider vmProvider,
    @JsonProperty("status")
    String statusName,
    @JsonIgnore
    Status vmStatus,
    @JsonIgnore
    UserDTO owner
)
    implements Identifiable {

    private VmDTO(final Builder builder) {
        this(
            builder.pk,
            builder.id,
            builder.provider.name,
            builder.provider,
            builder.status.name,
            builder.status,
            builder.owner
        );
    }

    @Override
    public String getId() {
        return this.id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(final VmDTO vm) {
        return new Builder()
            .pk(vm.pk())
            .id(vm.id())
            .provider(vm.vmProvider())
            .status(vm.vmStatus())
            .owner(vm.owner());
    }

    /**
     * The status of {@link VmDTO}s.
     */
    public enum Status {
        INITIALIZING("INITIALIZING"),
        UP("UP"),
        DOWN("DOWN"),
        ;

        private final String name;

        Status(final String name) {
            this.name = name;
        }

        public static Status fromName(final String name) {
            for (final var status : Status.values()) {
                if (status.name.equalsIgnoreCase(name)) {
                    return status;
                }
            }

            throw new IllegalArgumentException("Unknown status name: %s".formatted(name));
        }
    }

    /**
     * The host/provider of {@link VmDTO}s.
     */
    public enum Provider {
        AWS("AWS"),
        GCP("GCP"),
        ;

        private final String name;

        Provider(final String name) {
            this.name = name;
        }

        public static Provider fromName(final String name) {
            for (final var provider : Provider.values()) {
                if (provider.name.equalsIgnoreCase(name)) {
                    return provider;
                }
            }

            throw new IllegalArgumentException("Unknown provider name: %s".formatted(name));
        }
    }

    /**
     * A builder of {@link VmDTO}s.
     */
    public static class Builder {

        private int pk;
        private String id;
        private Provider provider;
        private Status status;
        private UserDTO owner;

        private Builder() {

        }

        public Builder pk(final int pk) {
            this.pk = pk;
            return this;
        }

        public Builder id(final String id) {
            this.id = id;
            return this;
        }

        public Builder provider(final Provider provider) {
            this.provider = provider;
            return this;

        }

        public Builder provider(final String provider) {
            this.provider = Provider.fromName(provider);
            return this;

        }

        public Builder status(final Status status) {
            this.status = status;
            return this;
        }

        public Builder status(final String status) {
            this.status = Status.fromName(status);
            return this;
        }

        public Builder owner(final UserDTO owner) {
            this.owner = owner;
            return this;
        }

        public VmDTO build() {
            Validate.notBlank(this.id);
            Validate.notNull(this.provider);
            Validate.notNull(this.status);
            Validate.notNull(this.owner);

            return new VmDTO(this);
        }
    }
}
