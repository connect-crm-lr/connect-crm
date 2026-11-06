# Imagem do back-end do Connect CRM (API Spring Boot).
# Build em duas etapas: a primeira compila com o Maven Wrapper e a segunda só roda o .jar,
# então a imagem final não leva o JDK, o Maven nem o código-fonte.
#
# Construir (na pasta backend/):  docker build -t connect-crm-backend .
# As variáveis de ambiente (banco, CORS, porta) estão em docs/deploy.md.

# ---------- 1) Build ----------
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# Dependências primeiro: enquanto o pom.xml não muda, o Docker reaproveita esta camada.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

# Os testes rodam no CI (precisam do PostgreSQL), por isso aqui são pulados.
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

# ---------- 2) Execução ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Mesmo fuso do banco e do Hibernate (AGENTS 4).
ENV TZ=America/Sao_Paulo

# Roda sem ser root.
RUN useradd --system --uid 1001 connectcrm
COPY --from=build /app/target/*.jar app.jar
USER connectcrm

EXPOSE 8080

# JAVA_OPTS permite ajustar a memória no servidor, ex.: -XX:MaxRAMPercentage=75
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
