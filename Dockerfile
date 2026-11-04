# ---- Build ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Maven version comes from the wrapper (.mvn/wrapper/maven-wrapper.properties)
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline

COPY src ./src
RUN ./mvnw -B -q package -DskipTests

# ---- Runtime ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system spring
USER spring

COPY --from=build /app/target/*.jar app.jar

# Heap at 75% of the container memory limit (JVM default: 25%)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
