FROM eclipse-temurin:21-jre

# Set the working directory inside the container
WORKDIR /app

# Copy the jar file into the container
COPY build/libs/server-0.0.1-SNAPSHOT.jar /app/server-0.0.1-SNAPSHOT.jar
COPY config/application-secret.yml /app/config/application.yml
COPY config/ApplicationSettings.json /app/config/ApplicationSettings.json
COPY config/AssistantSettings.json /app/config/AssistantSettings.json

# Expose the port that the app runs on
EXPOSE 28080

# Define the command to run the app
ENTRYPOINT ["java", "-jar", "server-0.0.1-SNAPSHOT.jar"]