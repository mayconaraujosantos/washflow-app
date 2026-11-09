# Runtime-only image built by the CI workflow's "image" step (kaniko), from a
# context holding the output of `gradle installDist` as ./app.
# Keep in sync with the final stage of the root Dockerfile.
FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache curl tini \
    && addgroup -S -g 10001 app \
    && adduser -S -u 10001 -G app -h /app -s /bin/sh app

WORKDIR /app
COPY --chown=app:app app/ /app/

ENV PORT=7000
ENV APP_ENV=production
EXPOSE 7000

USER app
ENTRYPOINT ["/app/bin/washflow-api"]
