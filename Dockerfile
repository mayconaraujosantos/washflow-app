FROM gradle:9.8-jdk21-alpine AS build

USER root
RUN apk add --no-cache curl bash unzip \
    && curl -fsSL https://bun.sh/install | bash \
    && ln -s /root/.bun/bin/bun /usr/local/bin/bun

WORKDIR /workspace
COPY . .
RUN gradle installDist --no-daemon

FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tini \
    && addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app -h /app -s /bin/sh app

WORKDIR /app
COPY --from=build /workspace/build/install/washflow-api/ /app/
RUN chown -R app:app /app

ENV PORT=7000
ENV APP_ENV=production
EXPOSE 7000

USER app
ENTRYPOINT ["/app/bin/washflow-api"]
