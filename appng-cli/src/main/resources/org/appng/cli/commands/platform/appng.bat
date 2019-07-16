@echo off

SET APPNG_HOME=${appng.home}
SET APPNG_DATA=${appng.data}

java -cp $APPNG_HOME\WEB-INF\lib\*;$APPNG_HOME\..\..\lib\* -Dlog4j.configuration=file:../conf/log4j-cli.properties -Dappng.home=$APPNG_HOME -DappngData=$APPNG_DATA org.appng.cli.CliBootstrap %*
