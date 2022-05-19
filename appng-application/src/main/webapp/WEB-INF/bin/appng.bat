@echo off

REM Directory where the Tomcat binary distribution resides
SET CATALINA_HOME=D:\servers\apache-tomcat-9.0.63

REM Root directory of the appNG web application
SET APPNG_HOME=%~dp0\..\..

java -cp "%APPNG_HOME%"/WEB-INF/lib/*;"%CATALINA_HOME%"/lib/* -Dlog4j2.configuration=file:"%APPNG_HOME%"/WEB-INF/conf/log4j2-cli.xml org.appng.cli.CliBootstrap %*