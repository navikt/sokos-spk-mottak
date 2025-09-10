FROM bellsoft/liberica-openjdk-alpine:21.0.8@sha256:c4052811bba52c7a06ebde235c839108bf723dfab3c65066f61145a252480b16

RUN apk add --no-cache dumb-init

COPY build/install/*/lib /lib


ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["dumb-init", "--"]
CMD ["java", "-cp", "/lib/*:/var/run/secrets/db2license/db2jcc_license_cisuz.jar", "no.nav.sokos.spk.mottak.ApplicationKt"]