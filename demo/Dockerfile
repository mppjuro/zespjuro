# Pobranie obrazu JDK
FROM openjdk:23-jdk-slim

# Ustawienie katalogu roboczego
WORKDIR /app

# Kopiowanie pliku JAR do obrazu
COPY target/demo-0.0.1-SNAPSHOT.jar demo.jar

# Komenda uruchamiająca aplikację
CMD ["java", "-jar", "demo.jar"]