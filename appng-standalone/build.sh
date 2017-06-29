export VERSION=0.13.0-SNAPSHOT
rm -rf appng-standalone-$VERSION/
mvn clean package
cd target
unzip appng-standalone-$VERSION.zip
cd appng-standalone-$VERSION
java -Dappng.node.id=appNGizer -jar appng-standalone-$VERSION.jar -u -i -p 8080
