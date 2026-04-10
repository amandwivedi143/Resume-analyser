# ============================================================
# 1. FRONTEND BUILD (Node)
# ============================================================
FROM node:18 AS frontend-builder

WORKDIR /app

COPY frontend ./frontend

WORKDIR /app/frontend
RUN npm install
RUN npm run build

# ============================================================
# 2. BACKEND BUILD (Java Maven)
# ============================================================
FROM maven:3.9.6-eclipse-temurin-17 AS backend-builder

WORKDIR /app

# Copy backend source
COPY pom.xml .
COPY src ./src

# Copy built frontend into Spring Boot static folder
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static

RUN mvn clean package -DskipTests

# ============================================================
# 3. FINAL IMAGE (Run JAR)
# ============================================================
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=backend-builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
