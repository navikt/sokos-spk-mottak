FROM ghcr.io/navikt/baseimages/temurin:21
LABEL org.opencontainers.image.source=https://github.com/navikt/sokos-spk-mottak

COPY build/libs/*.jar app.jar
CMD ["dumb-init", "--"]

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:ActiveProcessorCount=2 -XX:+UseParallelGC"

# Export vault properties to env
COPY .nais/export-vault.sh /init-scripts/export-vault.sh

ENTRYPOINT ["java","-jar", "app.jar"]
