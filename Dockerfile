# syntax=docker/dockerfile:1.7

FROM debian:bookworm-slim AS builder

ARG DEBIAN_FRONTEND=noninteractive

# Install only what is needed to build with Gradle and Corretto JDK.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates curl gnupg binutils \
    && install -d -m 0755 /etc/apt/keyrings \
    && curl -fsSL https://apt.corretto.aws/corretto.key \
      | gpg --dearmor -o /etc/apt/keyrings/corretto-keyring.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/corretto-keyring.gpg] https://apt.corretto.aws stable main" \
      > /etc/apt/sources.list.d/corretto.list \
    && apt-get update \
    && apt-get install -y --no-install-recommends java-21-amazon-corretto-jdk \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Low-churn layers first for better cache reuse.
COPY gradlew gradlew.bat build.gradle settings.gradle gradle.properties ./
COPY gradle ./gradle
RUN chmod +x ./gradlew
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon dependencies

# High-churn source code last.
COPY src ./src
RUN --mount=type=cache,target=/root/.gradle ./gradlew --no-daemon bootJar

# Build a slim runtime from Corretto JDK to avoid arch-specific JRE package gaps.
RUN jlink \
    --add-modules java.se,jdk.crypto.ec,jdk.unsupported \
    --strip-debug \
    --no-man-pages \
    --no-header-files \
    --compress=1 \
    --output /opt/corretto-jre


FROM debian:bookworm-slim AS runtime

ARG DEBIAN_FRONTEND=noninteractive

# Runtime image: keep only base certs and the custom Corretto runtime.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Run as a non-root user in production.
RUN addgroup --system app \
    && adduser --system --ingroup app app \
    && mkdir -p /var/log/planit/was \
    && chown -R app:app /app /var/log/planit

COPY --from=builder /opt/corretto-jre /opt/corretto-jre
ENV JAVA_HOME=/opt/corretto-jre
ENV PATH="${JAVA_HOME}/bin:${PATH}"

COPY --from=builder /app/build/libs/*.jar /app/app.jar

VOLUME ["/var/log/planit/was"]

USER app
EXPOSE 8080

CMD ["sh", "-c", "java -jar /app/app.jar 2>&1 | tee -a /var/log/planit/was/app.log"]
