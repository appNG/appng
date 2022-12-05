#
# Verify or guess CATALINA_HOME and CATALINA_BASE.
#

TOMCAT_JAR_RELPATH="lib/catalina.jar"

APPNG_HOME_RELPATH="webapps/ROOT"
APPNG_COREJAR_GLOB="appng-core-*.jar"

VERIFIED_CATALINA_HOME=
VERIFIED_CATALINA_BASE=

# -----------------------------------------------------------------------------
# Find CATALINA_HOME
# -----------------------------------------------------------------------------

# CATALINA_HOME is set and points to a Tomcat installation.
if [ -n "${CATALINA_HOME}" ]; then
  [ -e "${CATALINA_HOME}/${TOMCAT_JAR_RELPATH}" ] && VERIFIED_CATALINA_HOME="${CATALINA_HOME}"
fi

# CATALINA_HOME is not set or invalid, but it can be found relative to this script.
if [ -z "${VERIFIED_CATALINA_HOME}" ]; then
  guessed_catalina_home="$(readlink -f ../../../..)"
  [ -e "${guessed_catalina_home}/${TOMCAT_JAR_RELPATH}" ] && VERIFIED_CATALINA_HOME="${guessed_catalina_home}"
fi

if [ -n "${VERIFIED_CATALINA_HOME}" ]; then
  CATALINA_HOME="${VERIFIED_CATALINA_HOME}"
else
  echo "Cannot find Tomcat installation (CATALINA_HOME)!" >&2
  if [ -n "${CATALINA_HOME}" ]; then
    echo "'\$CATALINA_HOME' points to '${CATALINA_HOME}', which does not contain '${TOMCAT_JAR_RELPATH}'." >&2
  else
    echo "'\$CATALINA_HOME' is not set and guessed location '${guessed_catalina_home}' does not contain '${TOMCAT_JAR_RELPATH}'." >&2
  fi
  exit 1
fi

# -----------------------------------------------------------------------------
# Find CATALINA_BASE
# -----------------------------------------------------------------------------

# CATALINA_BASE is set and appNG is installed in it.
if [ -n "${CATALINA_BASE}" ]; then
  glob="${CATALINA_BASE}/${APPNG_HOME_RELPATH}/WEB-INF/lib/${APPNG_COREJAR_GLOB}"
  resolved_glob="$(echo ${glob})"
  [ "${resolved_glob}" != "${glob}" -a -e "${resolved_glob}" ] && VERIFIED_CATALINA_BASE="${CATALINA_BASE}"
fi

# CATALINA_HOME (and not _BASE) contains appNG (e.g. single instance Tomcat installation with CATALINA_BASE==CATALINA_HOME).
if [ -z "${VERIFIED_CATALINA_BASE}" ]; then
  glob="${CATALINA_HOME}/${APPNG_HOME_RELPATH}/WEB-INF/lib/${APPNG_COREJAR_GLOB}"
  resolved_glob="$(echo ${glob})"
  [ "${resolved_glob}" != "${glob}" -a -e "${resolved_glob}" ] && VERIFIED_CATALINA_BASE="${CATALINA_HOME}"
fi

if [ -n "${VERIFIED_CATALINA_BASE}" ]; then
  CATALINA_BASE="${VERIFIED_CATALINA_BASE}"
  APPNG_HOME="${CATALINA_BASE}/${APPNG_HOME_RELPATH}"
else
  echo "Cannot find Tomcat instance (CATALINA_BASE)!" >&2
  if [ -n "${CATALINA_BASE}" ]; then
    echo "'\$CATALINA_BASE' points to '${CATALINA_BASE}', which does not contain '${CATALINA_BASE}/${APPNG_HOME_RELPATH}/WEB-INF/lib/${APPNG_COREJAR_GLOB}'." >&2
  else
    echo "'\$CATALINA_BASE' is not set and guessed location '${CATALINA_HOME}' does not contain '${APPNG_HOME_RELPATH}/WEB-INF/lib/${APPNG_COREJAR_GLOB}'." >&2
  fi
  exit 2
fi

# -----------------------------------------------------------------------------

export CATALINA_HOME
export CATALINA_BASE
export APPNG_HOME
