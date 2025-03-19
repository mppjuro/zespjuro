# Pobranie obrazu JDK
FROM openjdk:23-jdk-slim

# Instalacja zależności systemowych (w tym OpenCV)
RUN apt update && apt install -y \
    libopencv-core-dev libopencv-imgproc-dev libopencv-highgui-dev \
    && rm -rf /var/lib/apt/lists/*

# Ustawienie katalogu roboczego
WORKDIR /app

# Tworzenie katalogu `uploads/` na zdjęcia
RUN mkdir -p /app/uploads

# Kopiowanie pliku JAR do obrazu
COPY target/demo-0.0.1-SNAPSHOT.jar demo.jar

# Komenda uruchamiająca aplikację
CMD ["java", "-jar", "demo.jar"]