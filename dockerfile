FROM eclipse-temurin:17-jdk AS build

WORKDIR /app

COPY pom.xml mvnw ./
COPY .mvn .mvn

RUN chmod +x mvnw && ./mvnw dependency:go-offline -q

COPY src src

RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:17-jre

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]