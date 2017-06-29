#!/bin/bash
# Author: Claus Stuemke, aiticon GmbH 2015
# 
# This is a small start script for appng standalone it helps setting
# some start parameter of the JVM and the application 


function error_exit {
	echo "${2}"
	exit ${3}
}


echo "################################################################"
echo "####                APPNG STANDALONE STARTER                ####"
echo "################################################################"

PS3="your selection: "
APPNG_START_OPTIONS=""

# get the alphanumeric highest appng-standalone jar file. 
JAR_FILE="$(ls | grep -e "appng-standalone-.*.jar" | sort -r | head -n 1 )"
if [[ "${JAR_FILE}" = "" ]]
then
  error_exit [ "ERROR: No appng-standalone jar file found in current directory" 1 ]
fi


echo "Do you like to: "
select OPTION in "install & start" "start" "abort"
do
  case $OPTION in 
    "start")
      break
      ;;
	"install & start")
	  APPNG_START_OPTIONS="-i -u"
	  break
	  ;;
	*)
	error_exit [ "abort" 1 ]
	exit 1
	break
	;;
  esac
done

echo ""
printf "Port (8080): "
read PORT
if [[ "${PORT}" = "" ]]
then
  PORT="8080"
fi

printf "Node Name ($HOSTNAME): "
read NODE_NAME
if [[ "${NODE_NAME}" = "" ]]
then
  NODE_NAME="${HOSTNAME}"
fi


printf "With debug port? (y/N): "
read ANSWER
if [[ "${ANSWER}" = "Y" || "${ANSWER}" = "y" ]]  
then
  printf "Debug Port (8000): "
  read DEBUG_PORT
  if [[ "${DEBUG_PORT}" = "" ]]
  then
    DEBUG_PORT="8000"
  fi
  DEBUG_OPTION="-Xdebug -Xrunjdwp:transport=dt_socket,address=${DEBUG_PORT},server=y,suspend=y"
else
  DEBUG_OPTION=""
fi

COMMAND="java ${DEBUG_OPTION} -Dappng.node.id=${NODE_NAME} -jar ${JAR_FILE} ${APPNG_START_OPTIONS} -p ${PORT}"
RED='\033[0;31m'
NC='\033[0m' # No Color
printf "\ncommand: ${RED}${COMMAND}${NC}\n"
printf " execute? (Y/n): "
read ANSWER
if [[ "${ANSWER}" = "n" || "${ANSWER}" = "N" ]]  
then
  error_exit [ "abort" 1 ]
else
  ${COMMAND}
fi
exit 0

