1. On appng-application, run
	mvn dependency:list -Dsort -Dmdep.outputScope=false -DincludeScope=compile -DoutputFile=dependencies.xml
2. remove the first two lines from dependencies.xml
3. remove the leading spaces on each line
4. in an editor, search for
	(.+):(.+):(.+):(.+)
	and replace it with
	<dependency><groupId>$1</groupId><artifactId>$2</artifactId><version>$4</version><scope>provided</scope></dependency>
5. insert the content inside the <dependencies> section of <dependencyManagement>
