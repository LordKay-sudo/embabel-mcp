FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven && mvn -q -DskipTests package && mv target/*.jar app.jar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/app.jar .
EXPOSE 1337
ENV MCP_SERVER_PORT=1337
ENV BIOINSIGHT_API_BASE_URL=http://host.docker.internal:8000/api/v1
ENTRYPOINT ["java", "-jar", "app.jar"]
