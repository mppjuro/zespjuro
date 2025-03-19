# Pobranie obrazu JDK
FROM openjdk:23-jdk-slim

# Instalacja zależności systemowych (w tym OpenCV)
RUN apt update && apt install -y \
    libopencv-dev \
    && rm -rf /var/lib/apt/lists/*

# Ustawienie katalogu roboczego
WORKDIR /app

# Kopiowanie pliku JAR do obrazu
COPY target/Binaryzacja-1.0-SNAPSHOT.jar binaryzacja.jar

# Komenda uruchamiająca aplikację
CMD ["java", "-jar", "binaryzacja.jar"]
