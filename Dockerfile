FROM jboss/keycloak

COPY target/*.jar /opt/jboss/keycloak/standalone/deployments/
