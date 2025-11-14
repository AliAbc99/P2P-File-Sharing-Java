# Stage 1: Base image with JDK 19 for running the JAR
FROM openjdk:19-jdk-slim AS runtime

# Set the working directory inside the container
WORKDIR /app

# Copy the pre-built JAR file from your local machine to the container
COPY 471Project.jar /app/
# Copy the shared_folders directory into the container
COPY shared_folders /root/shared_folders

# Install required libraries for GUI applications
RUN apt-get update && apt-get install -y \
    libxext6 \
    libx11-6 \
    libxtst6 \
    libpng16-16 \
    libjpeg62-turbo \
    libxrender1 \
    libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

# Set DISPLAY for GUI support
ENV DISPLAY=host.docker.internal:0.0

# Command to run the application using the JAR file
CMD ["java", "-jar", "/app/471Project.jar"]