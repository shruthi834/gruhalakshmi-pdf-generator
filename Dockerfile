# ==========================================
# Stage 1: Build React Frontend
# ==========================================
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm install
COPY frontend/ ./
RUN npm run build

# ==========================================
# Stage 2: Build Spring Boot Application
# ==========================================
FROM maven:3.9-eclipse-temurin-17-alpine AS backend-builder
WORKDIR /app
COPY pom.xml ./
COPY src ./src
# Copy compiled frontend assets into Spring Boot static resources
COPY --from=frontend-builder /app/src/main/resources/static ./src/main/resources/static
RUN mvn clean package -DskipTests

# ==========================================
# Stage 3: Lightweight Production Runtime
# ==========================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Install font libraries required for headless Kannada text shaping and AWT PDF rendering
RUN apk add --no-cache fontconfig ttf-dejavu freetype

COPY --from=backend-builder /app/target/pdf-generator-app-1.0.0.jar app.jar

EXPOSE 8080
ENV PORT=8080
ENV JAVA_OPTS="-Djava.awt.headless=true -Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
