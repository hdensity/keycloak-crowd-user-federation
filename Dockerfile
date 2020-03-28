FROM jboss/keycloak

COPY target/*[0-9].jar /opt/jboss/keycloak/standalone/deployments/
