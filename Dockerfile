# Etapa de construcción
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src

# Otorgar permisos de ejecución al wrapper y compilar el proyecto sin correr tests
RUN chmod +x ./gradlew
RUN ./gradlew build -x test

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar

# Puerto por defecto (Asegúrate que coincida con tu application.properties)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
