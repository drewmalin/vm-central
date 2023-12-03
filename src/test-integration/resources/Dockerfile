FROM azul/zulu-openjdk-alpine:21

COPY target/dependencies                                /run/dependencies
ADD  target/application.jar                             /run/application.jar
ADD  src/test-integration/resources/docker.config.json  /run/config.json
ADD  src/test-integration/resources/id_rsa              /run/id_rsa
ADD  src/test-integration/resources/id_rsa.pub          /run/id_rsa.pub

ENV VERTX_CONFIG_PATH=/run/config.json

ENTRYPOINT java -jar run/application.jar