FROM jboss/keycloak:9.0.2

ARG VERSION

COPY target/crowd-user-storage-*$VERSION.jar /opt/jboss/keycloak/standalone/deployments/
