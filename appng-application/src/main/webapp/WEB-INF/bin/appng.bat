@echo off

REM Directory where the Tomcat binary distribution resides
SET CATALINA_HOME=D:\servers\apache-tomcat-7.0.67

REM Root directory of the appNG web application
SET APPNG_HOME=%~dp0\..\..

java -cp "%APPNG_HOME%"/WEB-INF/lib/*;"%CATALINA_HOME%"/lib/* -Dlog4j.configuration=file:"%APPNG_HOME%"/WEB-INF/conf/log4j-cli.properties -Dappng.home="%APPNG_HOME%" org.appng.cli.CliBootstrap %*