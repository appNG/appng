call mvn install -Dmaven.test.skip=true
cp target\*.jar %CATALINA_HOME%\webapps\ROOT\WEB-INF\lib