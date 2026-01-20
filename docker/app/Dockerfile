# Stage 1: Build
FROM public.ecr.aws/docker/library/maven:3.9.6-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Pure Java, no Chrome needed)
FROM public.ecr.aws/docker/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

COPY --from=build /app/target/TearsDeepMind-1.0-SNAPSHOT.jar app.jar
RUN mkdir -p TearsMind

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]