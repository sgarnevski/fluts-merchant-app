# All Java services of the full app and the pipeline: one Maven build (on top of the core, cloned
# and installed by install-core.sh), one small runtime image per
# service. docker compose picks the image with `target:`; the build stage is shared (and cached).
# Build context: the repository root. Tests are not run here: some need Docker themselves
# (Testcontainers) and all of them run with ./mvnw verify.
FROM eclipse-temurin:21-jdk AS build
RUN apt-get update -qq && apt-get install -y -qq --no-install-recommends git > /dev/null && rm -rf /var/lib/apt/lists/*
WORKDIR /build
# The core (github.com/sgarnevski/fluts-merchant) first, so its layer is reused while only the platform changes
ARG FLUTS_MERCHANT_REF=main
COPY install-core.sh .
RUN --mount=type=cache,target=/root/.m2 FLUTS_MERCHANT_REF=$FLUTS_MERCHANT_REF ./install-core.sh
COPY . .
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -B -pl flut-app,flut-consumer,flut-producer,flut-trader -am package \
        -DskipTests -Djacoco.skip=true -Dpmd.skip=true

FROM eclipse-temurin:21-jre AS runtime
RUN useradd --system --uid 10001 flut
USER flut
ENTRYPOINT ["java", "-jar", "/app/app.jar"]

FROM runtime AS flut-app
COPY --from=build /build/flut-app/target/flut-app.jar /app/app.jar
EXPOSE 8081

FROM runtime AS flut-consumer
COPY --from=build /build/flut-consumer/target/flut-consumer.jar /app/app.jar

FROM runtime AS flut-producer
COPY --from=build /build/flut-producer/target/flut-producer.jar /app/app.jar

FROM runtime AS flut-trader
COPY --from=build /build/flut-trader/target/flut-trader.jar /app/app.jar
