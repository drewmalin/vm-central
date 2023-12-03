package com.drewmalin.vm.central.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Primary configuration object for the system. Additional fields may be added to capture the general context
 * (including Java configurations or environment variables).
 *
 * By default, this will capture the typical vm-central system configuration file.
 *
 * @param vertx
 * @param vmCentral
 * @param cloudVmWorker
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Config(
    @JsonProperty(value = "vertx", required = true)
    Vertx vertx,
    @JsonProperty(value = "vm.iaas", required = true)
    CloudVmIaas cloudVmIaas,
    @JsonProperty(value = "vm.central", required = true)
    VmCentral vmCentral,
    @JsonProperty(value = "vm.cloud.worker", required = true)
    CloudVmWorker cloudVmWorker
) {

    public boolean isEmpty() {
        return this.vertx == null
            && this.vmCentral == null
            && this.cloudVmWorker == null;
    }

    public record Vertx(
        @JsonProperty("vertx.worker.pool.size")
        int poolSize,
        @JsonProperty("metrics.prometheus.enabled")
        boolean prometheusEnabled
    ) {

    }

    /*
     * "vm.aws": {
     *   "aws": { ... }
     * }
     */
    public record CloudVmIaas(
        @JsonProperty(value = "aws")
        Aws aws
    ) {

        /*
         * "aws": {
         *   "endpoint": <string>
         *   "profileCredentials": {
         *     "profile": <string>
         *   },
         *   "basicCredentials": {
         *     "accessKeyId": <string>
               "secretAccessKey": <string>
         *   }
         * }
         *
         * Note: "profileCredentials" and "basicCredentials" are mutually exclusive
         */
        public record Aws(
            @JsonProperty("endpoint")
            String endpoint,
            @JsonProperty("profileCredentials")
            ProfileCredentials profileCredentials,
            @JsonProperty("basicCredentials")
            BasicCredentials basicCredentials
        ) {

            public record ProfileCredentials(
                @JsonProperty("profile")
                String profile
            ) {

            }

            public record BasicCredentials(
                @JsonProperty("accessKeyId")
                String accessKeyId,
                @JsonProperty("secretAccessKey")
                String secretAccessKey
            ) {

            }
        }
    }

    public record VmCentral(
        @JsonProperty("auth.key.filename.public")
        String publicKeyFilename,
        @JsonProperty("auth.key.filename.private")
        String privateKeyFilename,
        @JsonProperty("http.port")
        int httpPort,
        @JsonProperty("datasource.engine")
        String datasourceEngine,
        @JsonProperty("datasource.host")
        ConfigValue datasourceHostConfig,
        @JsonProperty("datasource.port")
        ConfigValue datasourcePortConfig,
        @JsonProperty("datasource.database")
        ConfigValue datasourceDatabaseConfig,
        @JsonProperty("datasource.username")
        ConfigValue datasourceUsernameConfig,
        @JsonProperty("datasource.password")
        ConfigValue datasourcePasswordConfig,
        @JsonProperty("datasource.max.pool.size")
        int datasourceMaxPoolSize,
        @JsonProperty("job.vm.update_status")
        JobVmUpdateStatus jobVmUpdateStatus
    ) {

        public record JobVmUpdateStatus(
            @JsonProperty("disabled")
            boolean disabled,
            @JsonProperty("period.millis")
            int periodMillis
        ) {

        }

        public String datasourceHost() {
            return datasourceHostConfig().getValue();
        }

        public int datasourcePort() {
            return Integer.parseInt(datasourcePortConfig().getValue());
        }

        public String datasourceDatabase() {
            return datasourceDatabaseConfig().getValue();
        }

        public String datasourceUsername() {
            return datasourceUsernameConfig().getValue();
        }

        public String datasourcePassword() {
            return datasourcePasswordConfig().getValue();
        }

    }

    public record CloudVmWorker(
        @JsonProperty("disabled")
        boolean disabled,
        @JsonProperty("vertx.worker.pool.name")
        String vertxWorkerPoolName,
        @JsonProperty("vertx.worker.pool.size")
        int vertxWorkerPoolSize,
        @JsonProperty("vertx.instance.count")
        int vertxInstanceCount
    ) {

    }

    public record ConfigValue(
        @JsonProperty("type")
        String type,
        @JsonProperty("value")
        String value
    ) {

        private String getValue() {
            return switch (type()) {
                case "plaintext" -> value();
                case "aws_ssm" -> throw new NotImplementedException("AWS SSM configs not yet implemented!");
                default -> throw new IllegalArgumentException("Unknown config type: %s".formatted(type));
            };
        }
    }
}
