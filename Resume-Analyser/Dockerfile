# ============================================================
# 1) Build FRONTEND (React / Vite / npm)
# ============================================================
FROM node:22-alpine AS frontend-builder
WORKDIR /app/frontend

COPY frontend/package*.json ./
RUN npm install

COPY frontend ./
RUN npm run build

# ============================================================
# 2) Build BACKEND (Spring Boot + Java 21)
# ============================================================
FROM maven:3.9.6-eclipse-temurin-21 AS backend-builder
WORKDIR /app

COPY pom.xml ./
RUN mvn -q dependency:go-offline

COPY . .

# Copy frontend build into Spring Boot static folder
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests

# ============================================================
# 3) Runtime Image (Java 21)
# ============================================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=backend-builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
