# Stage 1: Build
FROM public.ecr.aws/docker/library/maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime (Pure Java, no Chrome needed)
FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Python 3 and Pip
RUN apk add --no-cache python3 py3-pip

# Install Python dependencies
COPY src/main/resources/scripts/requirements.txt ./requirements.txt
RUN pip install --no-cache-dir -r requirements.txt --break-system-packages

# Copy scripts to a separate directory
COPY src/main/resources/scripts/spx_scanner.py ./scripts/spx_scanner.py

COPY --from=build /app/target/TearsDeepMind-1.0-SNAPSHOT.jar app.jar
RUN mkdir -p TearsMind

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]