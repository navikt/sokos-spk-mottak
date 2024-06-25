#!/bin/env sh

# Henter ut env variabler fra vault
if test -d /var/run/secrets/nais.io/vault;
then
    for FILE in $(find /var/run/secrets/nais.io/vault -maxdepth 1 -name "*.env")
    do
        _oldIFS=$IFS
        IFS='
'
        for line in $(cat "$FILE"); do
            _key=${line%%=*}
            _val=${line#*=}

            if test "$_key" != "$line"
            then
                echo "- exporting $_key"
            else
                echo "- (warn) exporting contents of $FILE which is not formatted as KEY=VALUE"
            fi

            export "$_key"="$(echo "$_val"|sed -e "s/^['\"]//" -e "s/['\"]$//")"
        done
        IFS=$_oldIFS
    done
fi

# Henter ut env variabler fra azuread
DIR=/var/run/secrets/nais.io/azuread
echo "Attempting to export Azure AD from $DIR if it exists"

if test -d $DIR;
then
    for FILE in `ls $DIR`
    do
       KEY="AZURE_`echo $FILE | tr '[:lower:]' '[:upper:]'`"
       echo "- exporting $KEY"
       export $KEY=`cat $DIR/$FILE`
    done
fi

# For å kunne kommunisere med .local tjenester
if test -r "${NAV_TRUSTSTORE_PATH}";
then
    if ! keytool -list -keystore ${NAV_TRUSTSTORE_PATH} -storepass "${NAV_TRUSTSTORE_PASSWORD}" > /dev/null;
    then
        echo Truststore is corrupt, or bad password
        exit 1
    fi

    JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStore=${NAV_TRUSTSTORE_PATH}"
    JAVA_OPTS="${JAVA_OPTS} -Djavax.net.ssl.trustStorePassword=${NAV_TRUSTSTORE_PASSWORD}"
    export JAVA_OPTS
fi

# Injecter proxy settings satt av nais platform
export JAVA_OPTS="${JAVA_OPTS} ${JAVA_PROXY_OPTIONS}"

# Opentelemetry
if [ ! -z "${OTEL_EXPORTER_OTLP_ENDPOINT}" ]; then
    JAVA_OPTS="${JAVA_OPTS} -javaagent:/opentelemetry-javaagent.jar"
fi
