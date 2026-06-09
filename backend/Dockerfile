ARG DOCKER_REGISTRY

FROM ${DOCKER_REGISTRY}maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

COPY settings.xml /root/.m2/settings.xml

COPY pom.xml .

COPY src ./src
RUN mvn clean package -DskipTests -B

FROM ${DOCKER_REGISTRY}eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/target/baking-backend.jar app.jar

RUN mkdir -p /app/uploads

EXPOSE 9031

ENTRYPOINT ["java", "-jar", "app.jar"]
