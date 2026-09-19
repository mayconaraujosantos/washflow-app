FROM gradle:9.7.1-jdk21 AS build

USER root
RUN apt-get update \
    && apt-get install --no-install-recommends -y curl unzip \
    && curl -fsSL https://bun.sh/install | bash \
    && ln -s /root/.bun/bin/bun /usr/local/bin/bun \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre

RUN apt-get update \
    && apt-get install --no-install-recommends -y curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --no-create-home app

WORKDIR /app
COPY --from=build /workspace/build/install/washflow-api/ /app/
RUN chown -R app:app /app

ENV PORT=8080
EXPOSE 8080

USER app
ENTRYPOINT ["/app/bin/washflow-api"]
