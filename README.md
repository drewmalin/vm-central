# VM Central

A cloud VM (virtual machine) central system, using the following technologies:

* [Vert.x](https://vertx.io) web server, event bus, and asynchronous job processing
* [PostgreSQL](https://www.postgresql.org) database
* [Flyway](https://flywaydb.org) database migrations
* [Prometheus](https://prometheus.io) monitoring, alerting
* [Grafana](https://grafana.com) observability
* [Testcontainers](https://testcontainers.com) integration testing
* [REST-assured](https://rest-assured.io) system testing and benchmarking

### Build

Build the project into a .jar located at `/target`:

```
> mvn clean package
```

### Test

Run unit tests:

```
> mvn clean test
```

Run integration tests:

```
> mvn clean integration-test
```

### Deploy (Docker-Compose)

Build, then:
```
> docker compose up --build
```

Alternatively, deploy specific containers to allow for local debugging:

```
> docker compose up db
```

Set the following environment variable prior to local deployment (this can also be done permanently from an IntelliJ Run
Configuration):

```
VERTX_CONFIG_PATH=src/test-integration/resources/test.config.json
```

### Metrics (Docker-Compose)

```
> docker compose up db prometheus grafana
```

View service metrics endpoint: 
- http://localhost:9876/metrics

View Prometheus:
- http://localhost:9090

View Grafana:
- http://localhost:3000 (username: `admin`, password: `grafana`)
