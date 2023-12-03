package com.drewmalin.vm.central.data.model;

import com.drewmalin.vm.central.data.repository.Identifiable;
import com.drewmalin.vm.central.security.Principal;
import com.drewmalin.vm.central.security.Role;
import com.drewmalin.vm.central.security.User;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.Validate;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserDTO(
    @JsonProperty("user_pk")
    int pk,
    @JsonProperty("user_id")
    String id,
    @JsonProperty("username")
    String username,
    @JsonProperty("hashed_password")
    byte[] hashedPassword,
    @JsonProperty("salt")
    byte[] salt,
    @JsonProperty("first_name")
    String firstName,
    @JsonProperty("last_name")
    String lastName,
    @JsonProperty("role_id")
    String roleId
)
    implements Identifiable {

    private UserDTO(final Builder builder) {
        this(
            builder.pk,
            builder.id,
            builder.username,
            builder.hashedPassword,
            builder.salt,
            builder.firstName,
            builder.lastName,
            builder.roleId
        );
    }

    @Override
    public String getId() {
        return this.id;
    }

    /**
     * Converts this user to a security Principal.
     *
     * @return the {@link Principal} representation of this user.
     */
    public Principal toPrincipal() {
        final var role = Role.forId(this.roleId);
        return new User(this.id, role);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(final UserDTO user) {
        return new Builder()
            .pk(user.pk())
            .id(user.id())
            .username(user.username())
            .hashedPassword(user.hashedPassword())
            .salt(user.salt())
            .firstName(user.firstName())
            .lastName(user.lastName())
            .roleId(user.roleId());
    }

    /**
     * A builder of {@link UserDTO}s.
     */
    public static class Builder {

        private int pk;
        private String id;
        private String username;
        private byte[] hashedPassword;
        private byte[] salt;
        private String firstName;
        private String lastName;
        private String roleId;

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

        public Builder username(final String username) {
            this.username = username;
            return this;
        }

        public Builder hashedPassword(final byte[] hashedPassword) {
            this.hashedPassword = hashedPassword;
            return this;
        }

        public Builder salt(final byte[] salt) {
            this.salt = salt;
            return this;
        }

        public Builder firstName(final String firstName) {
            this.firstName = firstName;
            return this;
        }

        public Builder lastName(final String lastName) {
            this.lastName = lastName;
            return this;
        }

        public Builder roleId(final String roleId) {
            this.roleId = roleId;
            return this;
        }

        public UserDTO build() {
            Validate.notBlank(this.id);
            Validate.notBlank(this.username);
            Validate.notBlank(this.firstName);
            Validate.notBlank(this.lastName);
            Validate.notNull(this.hashedPassword);
            Validate.notNull(this.salt);
            Validate.notBlank(this.roleId);

            return new UserDTO(this);
        }
    }

}
