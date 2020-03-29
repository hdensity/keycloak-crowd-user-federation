FROM jboss/keycloak

ARG VERSION

COPY target/*$VERSION.jar /opt/jboss/keycloak/standalone/deployments/
