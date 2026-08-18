# 1. Estágio de Compilação (Build)
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /app

# Instala o Maven que é necessário para compilar dentro do Alpine
RUN apk add --no-cache maven

# Copia os arquivos de configuração do Maven e o código fonte
COPY pom.xml .
COPY src ./src

# Compila o projeto gerando o JAR e pulando os testes para agilizar o deploy
RUN mvn clean package -DskipTests

# 2. Estágio de Execução (Runtime)
FROM eclipse-temurin:25-jdk-alpine
WORKDIR /app

# Cria um usuário de sistema comum por segurança
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copia o JAR gerado no estágio anterior
COPY --from=build /app/target/pets-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-Dserver.port=${PORT:8080}", "-jar", "app.jar"]