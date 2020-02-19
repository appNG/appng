appNG appNGizer Maven Plugin
============================
Provides the ability to upload and install locally created application-/template-archives to a local repository of an appNG instance where appNGizer is running.
Optionally, the installed archive can be activated for a site and a site reload can be performed.

Goals
=======
upload:		uploads a local archive to a local repository
install: 	uploads and installs a local archive to/from a local repository

Configuration 

General (all mandatory)
- endpoint		- the endpoint URL of appNGizer
- sharedSecret	- the platform's shared secret used for authentication
- repository	- the name of the local repository

install goal only
- activate 		- if the installed archive should be activated for the site (default: false)
- site  		- the name of the site to reload after installing the archive (no default)
- privileged	- should the application be installed as privileged application? (default: false)
- hidden		- should the application be installed as hidden application? (default: false)

Usage
=======
<plugin>
	<groupId>org.appng.maven</groupId>
	<artifactId>appng-appngizer-maven-plugin</artifactId>
	<version>${appNG.version}</version>
	<configuration>
		<endpoint>http://localhost:8080/appNGizer/</endpoint>
		<sharedSecret>TheSecret</sharedSecret>
		<repository>Local</repository>
		<site>manager</site>
		<activate>true</activate>
		<privileged>false</privileged>
		<hidden>false</hidden>
	</configuration>
	<executions>
		<execution>
			<goals>
				<goal>install</goal>
			</goals>
		</execution>
	</executions>
</plugin>