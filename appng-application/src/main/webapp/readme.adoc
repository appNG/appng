   -  - ---- ----------------------------------------- -------- -  -  
               ____      _______         _____       .---,,--,-^²²^-,.
      .,--^^²´´    `^^²´´       `²^-.-²´´     ``²---´ dSb. db ,dS$P² <
   ,-´_.dSS$§§§§$Sb. .dS$§§§§§$Sb. : .dS$§§§§$SSb. :  $§4b.$$ l$´ ss ;
 ,/ .dS$SP²^^^²4$§§$ $§§$P²^^^²4S$$b.`4$$P²^^^²4S§$b. 4$`4b$P `4S$SP |
 ! .$§SP°       $§§$ $§§$´      `4S§$.`4$´      `4S§$. .-----^²------´ 
 ; [$§$l        $§§$ $§§$        l$§$] l$        l$§$] !               
 l.`S$Sb.       $§§$ $§§$       ,dS$S´ d$       ,dS$S´ ;               
  \ `4S$Sbsaaasd$§§$ $§§$ssaaasdS$SP´,d$$ssaaasdS$SP´ l                
   \_ ``4SS$§§§§§§§$ $§§§§§§§$SSP´´  $§§§§§§§$SSP´´_.^                 
     `----,_,.____,. $$$$  ___.___,^ $$$$  ___.,--^                    
  _  _ ______ ____ ! $§§$ ! ____ _ ! $$$$ | ________ _______ ____ _  _ 
                   | $$$$ ;        l $$$$ ;                            
                   `--,.-^´        `--,.-^´

Welcome to appNG, the [app]lication platform of the [N]ext [G]eneration!
========================================================================

1. Requirements
----------------------
-	JRE >= 1.8.x
		http://www.oracle.com/technetwork/java/javase/downloads/index.html
-	Apache Tomcat 8.5.x
		- Tomcat 7 is also supported, but only with versions >= 7.0.55
-	for persistence, MySql >= 5.6.x is used by default
		http://www.mysql.com/
		
2. Configuration & Installation
----------------------
-	delete all folders from $CATALINA_HOME/webapps
-	expand the appng-application-X.Y.Z.war to $CATALINA_HOME/webapps/ROOT
-	create a new database with the DB management tool of your choice and set your database connection properties in the file
		WEB-INF/conf/appNG.properties
-	Download the Connector/J here
		http://dev.mysql.com/downloads/connector/j/
	and save mysql-connector-java-5.1.42.jar it to $CATALINA_HOME/lib
-	verify that CATALINA_HOME and APPNG_HOME in
		WEB-INF/bin/appng		(for unix/linux based systems)
		WEB-INF/bin/appng.bat	(for windows based systems)
		match your local machine settings (change/uncomment the respective lines if necessary)
-	in WEB-INF/conf/install.list
	-	set the variable ADMIN_SUBJECT (#15) to your desired username
			def ADMIN_SUBJECT=johndoe
	-	in the line starting with "create-subject" (#31), set your desired password (-p), full name (-n) and e-mail address (-e)
			create-subject -u $ADMIN_SUBJECT -p secret -n "John Doe" -l en -e jd@example.org
-	on a command prompt, change to
		WEB-INF/bin
		and run
			chmod +x appng
			./appng batch -f ../conf/install.list
		(for unix/linux based systems)
		
			appng batch -f ..\conf\install.list
		(for windows based systems)
-	start Tomcat
			$CATALINA_HOME/bin/catalina.sh run
		(for unix/linux based systems)	
		
			%CATALINA_HOME%/bin/catalina.bat run
		(for windows based systems)
-	check WEB-INF/log/appNG.log
	if you see the line
		 appNG X.Y.Z started in xxx ms.
	at the end, startup was successful	 
-	browse to
			http://localhost:8080/manager
	to login with your username/password
-	enjoy!
-	start developing your own applications!
	