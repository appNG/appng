FROM maven:3-jdk-8 as build

RUN mkdir /srv/appng.build
WORKDIR /srv/appng.build
COPY ./ ./

ARG APPNG_MANAGER_VERSION \
    APPNG_AUTHENTICATION_VERSION \
    APPNG_SCHEDULER_VERSION

RUN --mount=type=cache,target=/root/.m2:z \
    mvn install -pl appng-application -am -B \
		-Dmaven.test.skip -Dmaven.source.skip -Dmaven.javadoc.skip -Dasciidoctor.skip -Dmaven.war.skip \
        -Djavax.xml.accessExternalSchema=all \
        -DappNG.manager.version=${APPNG_MANAGER_VERSION:-$(mvn help:evaluate -Dexpression=appNG.manager.version -q -DforceStdout)} \
        -DappNG.authentication.version=${APPNG_AUTHENTICATION_VERSION:-$(mvn help:evaluate -Dexpression=appNG.authentication.version -q -DforceStdout)} \
        -DappNG.scheduler.version=${APPNG_SCHEDULER_VERSION:-$(mvn help:evaluate -Dexpression=appNG.scheduler.version -q -DforceStdout)} \
	&& mvn -pl appng-application war:exploded



FROM tomcat:9-jdk8

ENV APPNG_HOME=${CATALINA_HOME}/webapps/ROOT
# The URL pointing to the mysql container (name) we are setting up in docker-compose.yml. 
ENV HIBERNATE_CONNECTION_URL=jdbc:mysql://appng_dev_mariadb:3306/appng

RUN rm -rf ${APPNG_HOME}

ARG JDBC_DRIVER_DOWNLOAD_URL=https://repo1.maven.org/maven2/mysql/mysql-connector-java/8.0.30/mysql-connector-java-8.0.30.jar
RUN curl -s -O --output-dir ${CATALINA_HOME}/lib ${JDBC_DRIVER_DOWNLOAD_URL}

COPY --from=build /srv/appng.build/appng-application/target/appng-application-*/ ${APPNG_HOME}
RUN sed -i "s#^hibernate\.connection\.url *=.*#hibernate.connection.url = ${HIBERNATE_CONNECTION_URL}#" ${APPNG_HOME}/WEB-INF/conf/appNG.properties

RUN chmod +x ${APPNG_HOME}/WEB-INF/bin/appng
