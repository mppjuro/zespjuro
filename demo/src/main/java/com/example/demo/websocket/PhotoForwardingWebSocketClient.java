package com.example.demo.websocket;

import jakarta.websocket.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import jakarta.websocket.Session;

@ClientEndpoint
public class PhotoForwardingWebSocketClient {

    private Session session;
    private final String targetUri;
    private final CountDownLatch latch = new CountDownLatch(1);
    private byte[] response;
    // Bufor do akumulacji części wiadomości
    private ByteArrayOutputStream responseBuffer = new ByteArrayOutputStream();

    public PhotoForwardingWebSocketClient(String targetUri) throws URISyntaxException, DeploymentException, IOException {
        this.targetUri = targetUri;
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxBinaryMessageBufferSize(30 * 1024 * 1024);
        container.setDefaultMaxTextMessageBufferSize(30 * 1024 * 1024);
        container.connectToServer(this, new URI(targetUri));
    }

    public void connect() throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        this.session = container.connectToServer(this, new URI(targetUri));
    }

    public void sendChunk(byte[] data) throws IOException {
        if (session != null && session.isOpen()) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            session.getBasicRemote().sendBinary(buffer);
        }
    }

    public void sendText(String text) throws IOException {
        if (session != null && session.isOpen()) {
            session.getBasicRemote().sendText(text);
        }
    }

    public void close() throws IOException {
        if (session != null && session.isOpen()) {
            session.close();
        }
    }

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("Connected to target microservice WebSocket: " + session.getId());
    }

    // Nowa metoda do odbierania fragmentów wiadomości binarnych
    @OnMessage
    public void onMessage(ByteBuffer message, boolean last) {
        try {
            byte[] bytes = new byte[message.remaining()];
            message.get(bytes);
            responseBuffer.write(bytes);
            if (last) {
                response = responseBuffer.toByteArray();
                latch.countDown();
            }
        } catch (IOException e) {
            System.err.println("Błąd podczas odbierania wiadomości: " + e.getMessage());
        }
    }

    // Usuwamy lub komentujemy starą metodę odbioru pełnych wiadomości
    /*
    @OnMessage
    public void onMessage(ByteBuffer message) {
        System.out.println("Otrzymano odpowiedź od serwera!");
        response = new byte[message.remaining()];
        message.get(response);
        latch.countDown();
    }
    */

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Disconnected from target microservice: " + reason);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("Error in target microservice connection: " + throwable.getMessage());
    }

    public byte[] waitForResponse() throws InterruptedException {
        latch.await();
        System.out.println("Doczekaliśmy się odpowiedzi !");
        ByteBuffer buffer = ByteBuffer.wrap(response);
        processResponse(buffer);
        System.out.println("Przetworzono odpowiedź serwera Binaryzacji !");
        return response;
    }

    private void processResponse(ByteBuffer buffer) {
        File folder = new File("ekg");
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // Ustawienie pozycji bufora na początek
        buffer.rewind();
        // Pierwszy int - liczba obrazów
        int numImages = buffer.getInt();
        System.out.println("Liczba obrazów: " + numImages);

        // Lista nazw plików odpowiadających wykresom
        String[] fileNames = {
                "I.png", "II.png", "III.png",
                "aVR.png", "aVL.png", "aVF.png",
                "V1.png", "V2.png", "V3.png",
                "V4.png", "V5.png", "V6.png"
        };

        for (int i = 0; i < numImages; i++) {
            int smallPx = buffer.getInt();
            System.out.println("Px na kratkę: " + (double) smallPx / 1000000.0);
            int width = buffer.getInt();
            int height = buffer.getInt();
            int n = buffer.getInt(); // liczba intów ze skompresowanymi danymi bitmapy

            int[] imageData = new int[n];
            for (int j = 0; j < n; j++) {
                imageData[j] = buffer.getInt();
            }

            // Utwórz obraz czarno-biały, 1 int to 32 px (32 bity)
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY);
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int bitIndex = y * width + x;
                    int intIndex = bitIndex / 32;
                    int bitOffset = bitIndex % 32;
                    int bit = (imageData[intIndex] >> bitOffset) & 1;
                    int rgb = (bit == 1) ? 0x000000 : 0xFFFFFF;
                    image.setRGB(x, y, rgb);
                }
            }

            String fileName = (i < fileNames.length) ? fileNames[i] : ("chart_" + i + ".png");
            File outputFile = new File(folder, fileName);
            try {
                ImageIO.write(image, "png", outputFile);
                System.out.println("Zapisano obraz: " + outputFile.getAbsolutePath());
            } catch (IOException e) {
                System.err.println("Błąd zapisu obrazu " + fileName + ": " + e.getMessage());
            }
        }
    }
}
