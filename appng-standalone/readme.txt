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

This is a standalone version of appNG - the [app]lication platform of the [N]ext [G]eneration!
==============================================================================================

Prerequisites:
=============
* JRE/JDK 1.8

Usage:
======
* unzip appng-standalone-x.y.z.zip
* on the FIRST start, run
	java -jar appng-standalone-x.y.z.jar -i -u
  (when running under Windows, you have to add the option `-Dfile.encoding=UTF-8` before the `-jar` option)
* go to http://localhost:8080/manager in your browser
  User: admin
  Password: s3cr3t
* on the following starts, run
	java -jar appng-standalone-x.y.z.jar

	
Options:
========
-i [<filename>]	executes an install-skript, if filename is omitted, a build-in one is used
-u				unzip the WAR-Archive, only needed on the first execution
-p <port>		the port used by Tomcat (default:8080)
