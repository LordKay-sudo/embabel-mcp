FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw mvnw.cmd ./
COPY .mvn .mvn
COPY src ./src
RUN chmod +x mvnw && ./mvnw -q -DskipTests package && cp target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /app/app.jar .
EXPOSE 1337
ENV MCP_SERVER_PORT=1337
ENV BIOINSIGHT_API_BASE_URL=http://api:8000/api/v1
HEALTHCHECK --interval=15s --timeout=5s --retries=8 --start-period=45s \
  CMD curl -fsS http://127.0.0.1:1337/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
