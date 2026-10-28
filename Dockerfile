# 1) Monta o site React
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# 2) Compila o Spring Boot com o site dentro
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/pom.xml ./
RUN mvn -q dependency:go-offline
COPY backend/src ./src
COPY --from=frontend /app/backend/src/main/resources/static ./src/main/resources/static
RUN mvn -q -DskipTests package

# 3) Imagem final, só com o Java
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 1001 atelie && mkdir -p /data/uploads /data/privado && chown -R atelie /data
WORKDIR /app
COPY --from=backend /app/backend/target/jo-monteiro-atelie.jar app.jar
USER atelie
ENV UPLOADS_DIR=/data/uploads \
    UPLOADS_PRIVADOS_DIR=/data/privado \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -Duser.timezone=America/Fortaleza"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
