# appNG maven plugin

* replaces maven exec plugin to generate constant classes for properties and messages
* support for eclipse m2e lifecylce

## Example usage

```xml
<properties>
	<appNG.version>1.16.2</appNG.version>
	<application.homeFolder>${project.basedir}/application-home</application.homeFolder>
	<application.constants.generatedOutputFolder>target/generated-sources/constants</application.constants.generatedOutputFolder>
</properties>

...

<plugins>
	<plugin>
		<groupId>de.mainova.appng</groupId>
		<artifactId>appng-maven-plugin</artifactId>
		<version>${appng-maven-plugin.version}</version>
		<executions>
          <execution>
            <id>applicationConstants</id>
            <goals>
              <goal>generateApplicationConstants</goal>
            </goals>
            <configuration>
            	  <filePath>${application.homeFolder}/application.xml</filePath>
              <targetClass>com.example.appng.ApplicationConstant</targetClass>
              <outfolder>${application.constants.generatedOutputFolder}</outfolder>
            </configuration>
          </execution>
          <execution>
            <id>messageConstants</id>
            <goals>
              <goal>generateMessageConstants</goal>
            </goals>
            <configuration>
              <filePath>${application.homeFolder}/dictionary/mymessages.properties</filePath>
              <targetClass>com.example.appng.MessageConstant</targetClass>
              <outfolder>${application.constants.generatedOutputFolder}</outfolder>
            </configuration>
          </execution>
        </executions>
		<dependencies>
			<!-- application xsd may change. use project version -->
			<dependency>
				<groupId>org.appng</groupId>
				<artifactId>appng-xmlapi</artifactId>
				<version>${appNG.version}</version>
			</dependency>
			<dependency>
				<groupId>commons-io</groupId>
				<artifactId>commons-io</artifactId>
				<version>2.6</version>
			</dependency>
		</dependencies>
	</plugin>
	
	
	<plugin>
		<groupId>org.codehaus.mojo</groupId>
		<artifactId>build-helper-maven-plugin</artifactId>
		<version>3.0.0</version>
		<executions>
			<execution>
				<phase>generate-sources</phase>
				<goals>
					<goal>add-source</goal>
				</goals>
				<configuration>
					<sources>
						<source>${application.constants.generatedOutputFolder}</source>
					</sources>
				</configuration>
			</execution>
		</executions>
	</plugin>
</plugins>
```