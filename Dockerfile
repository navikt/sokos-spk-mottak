FROM bellsoft/liberica-openjdk-alpine:21.0.8@sha256:c4052811bba52c7a06ebde235c839108bf723dfab3c65066f61145a252480b16

RUN apk update && apk add --no-cache \
  dumb-init \
  && rm -rf /var/lib/apt/lists/*

COPY build/install/*/lib /lib
COPY java-opts.sh /

RUN chmod +x /java-opts.sh

ENV TZ="Europe/Oslo"
ENV JDK_JAVA_OPTIONS="-XX:MaxRAMPercentage=75"

ENTRYPOINT ["dumb-init", "--"]
CMD ["sh", "-c", "exec java -cp /lib/*:/var/run/secrets/db2license/db2jcc_license_cisuz.jar no.nav.sokos.spk.mottak.ApplicationKt"]
