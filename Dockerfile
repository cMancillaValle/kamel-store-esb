FROM maven:3.9.9-eclipse-temurin-21 AS build

ARG SERVICE
WORKDIR /workspace

COPY pom.xml ./
COPY services/ventas-service/pom.xml services/ventas-service/pom.xml
COPY services/esb-service/pom.xml services/esb-service/pom.xml
COPY services/inventario-service/pom.xml services/inventario-service/pom.xml
COPY services/facturacion-service/pom.xml services/facturacion-service/pom.xml
RUN mvn -B -pl services/${SERVICE} -am dependency:go-offline

COPY services services
RUN mvn -B -pl services/${SERVICE} -am package -DskipTests

FROM eclipse-temurin:21-jre-jammy

ARG SERVICE
WORKDIR /app
COPY --from=build /workspace/services/${SERVICE}/target/${SERVICE}-1.0.0.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
