package com.example.demo.websocket;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.File;

public class PhotoWebSocketHandler extends BinaryWebSocketHandler {

    private final PhotoService photoService;
    private FileOutputStream outputStream;
    private String fileName;
    private PhotoForwardingWebSocketClient forwardingClient;

    public PhotoWebSocketHandler(PhotoService photoService) {
        this.photoService = photoService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("WebSocket connection established: " + session.getId());
        session.setBinaryMessageSizeLimit(30 * 1024 * 1024); // 30MB
        session.setTextMessageSizeLimit(30 * 1024 * 1024);
        super.afterConnectionEstablished(session);

        fileName = photoService.generateUniqueFileName(".png");
        File destination = new File("uploads/" + fileName);
        outputStream = new FileOutputStream(destination, true);
        forwardingClient = new PhotoForwardingWebSocketClient("ws://localhost:9998/ws");
        forwardingClient.connect();
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        ByteBuffer buffer = message.getPayload();
        photoService.generateUniqueFileName(".png");
        byte[] photoBytes = new byte[buffer.remaining()];
        buffer.get(photoBytes);

        //  Convert to InputStream
        InputStream inputStream = new ByteArrayInputStream(photoBytes);

        // Save the photo
        photoService.savePhoto(inputStream, fileName);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(photoBytes));
        if (image == null) {
            throw new Exception("Nie można odczytać obrazu");
        }

        ImageIO.write(image, "png", baos);
        byte[] pngBytes = baos.toByteArray();

        forwardingClient.sendChunk(pngBytes);
        forwardingClient.sendText("KONIEC");

        session.sendMessage(new TextMessage("Photo uploaded via WebSocket as " + fileName));
        System.out.println("Obraz przesłany pomyślnie");

        byte[] response = forwardingClient.waitForResponse();
        if (response == null) {
            System.err.println("Brak odpowiedzi od serwera!");
            return;
        }

        System.out.println("Otrzymano odpowiedź. Długość: " + response.length);
        System.out.println("Odpowiedź: " + Arrays.toString(response));
        /*
        *
        * KOMUNIKACJA Z KAMILEM
        *
        *
        * */
        // Send the processed photo back to the frontend
        session.sendMessage(new BinaryMessage(pngBytes));
        System.out.println("Processed photo sent back to frontend. Length: " + response.length);



    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        if (outputStream != null) outputStream.close();
        if (forwardingClient != null) forwardingClient.close();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        exception.printStackTrace();
        session.close(CloseStatus.SERVER_ERROR);
    }
}