# syntax=docker/dockerfile:1

################################################################################
# Stage 1: Build the application
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
# RUN mvn dependency:go-offline -DskipTests

# Copy source code and build
COPY src ./src
RUN mvn package -DskipTests


################################################################################
# Stage 2: Run with Jetty
FROM jetty:11-jre17

# Copy WAR file to Jetty webapps
COPY --from=build /app/target/*.war /var/lib/jetty/webapps/ROOT.war

EXPOSE 8080




################################################################################
# Stage 2: Run the application if jar built
# Use a lightweight JRE image (Don't need full JDK + Maven)
# FROM eclipse-temurin:17-jre-jammy AS final

# # Create non-privileged user
# # No password login, No user info required, No home directory, No shell access, Don't create home directory
# ARG UID=10001
# RUN adduser \
#     --disabled-password \
#     --gecos "" \
#     --home "/nonexistent" \
#     --shell "/sbin/nologin" \
#     --no-create-home \
#     --uid "${UID}" \
#     appuser
# # Switch to non-privileged user
# USER appuser

# # Copy the built JAR and rename to simple name
# COPY --from=build /app/target/*.jar app.jar

# # Servlet usually runs on 8080
# EXPOSE 8080

# ENTRYPOINT ["java", "-jar", "app.jar"]