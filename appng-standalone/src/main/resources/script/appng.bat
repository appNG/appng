@echo off
SET APPNG_HOME=%~dp0\..\..
java -cp "%APPNG_HOME%"\WEB-INF\lib\*;"%APPNG_HOME%"\..\lib\* -Dlog4j.configuration=file:"%APPNG_HOME%"\WEB-INF\conf\log4j-cli.properties -Dappng.home="%APPNG_HOME%" org.appng.cli.CliBootstrap %*