FROM ces-docker.dkrreg.mmih.biz/wildfly11:latest

MAINTAINER CES Money Management <btsjava@momentum.co.za>

USER root

# Copy artifacts
COPY impl/target/multiply-money-management-impl.war /opt/wildfly/standalone/deployments/multiply-money-services.war

# Copy Wildfly configuration files
COPY docker/files/wildfly/configuration/* /opt/wildfly/standalone/configuration/

# Copy Wildfly standalone conf files
COPY docker/files/wildfly/bin/standalone.conf /opt/wildfly/bin/

# Copy Wildfly modules
COPY docker/files/wildfly/modules/ /opt/wildfly/modules/

# Copy properties files to bin directory
COPY docker/files/profiles/dev/environment.properties /opt/wildfly/bin/environment-dev.properties
COPY docker/files/profiles/pre/environment.properties /opt/wildfly/bin/environment-preprod.properties
COPY docker/files/profiles/prod/environment.properties /opt/wildfly/bin/environment-prod.properties
COPY docker/files/profiles/local/environment.properties /opt/wildfly/bin/environment-staging.properties

# Copy WSDL files to bin directory
COPY bpm-auto-account-stub/src/main/resources/META-INF/wsdl/* /opt/wildfly/bin/
COPY docker/files/wildfly/standalone.xml /opt/wildfly/standalone/configuration/standalone.xml

# Re-apply file permissions
RUN chown -R wildfly:wildfly /opt/wildfly*
RUN chmod +x /opt/wildfly/bin/startWildfly.sh

USER wildfly

RUN echo "Artifacts to be deployed:" && ls /opt/wildfly/standalone/deployments/
