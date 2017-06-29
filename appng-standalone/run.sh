export version=`mvn org.apache.maven.plugins:maven-help-plugin:2.1.1:evaluate -Dexpression=project.version|grep -Ev '(^\[|Download\w+:)'`
mvn package -Dmaven.test.skip
rm -rf target/appng-standalone-$version
unzip target/appng-standalone-$version.zip -d target
cd target/appng-standalone-$version
java -jar appng-standalone-$version.jar -i -u
