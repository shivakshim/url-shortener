# Step 1: Use base image with Java
FROM eclipse-temurin:21-jdk

# Step 2: Set working directory inside container
WORKDIR /app

# Step 3: Copy jar file into container
COPY target/urlshortner-0.0.1-SNAPSHOT.jar app.jar

# Step 4: Expose port (Spring Boot default)
EXPOSE 8080

# Step 5: Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]