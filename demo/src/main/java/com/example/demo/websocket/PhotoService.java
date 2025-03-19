package com.example.demo.websocket;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Service
public class PhotoService {

    public String savePhoto(InputStream inputStream, String fileName) throws Exception {
        String uploadDir = "uploads/";
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File destination = new File(uploadDir + fileName);
        try (FileOutputStream outputStream = new FileOutputStream(destination)) {
            byte[] buffer = new byte[4 * 1024]; // 4KB chunks
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        return fileName;
    }

    // Optional: Generate a unique filename if needed
    public String generateUniqueFileName(String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return "photo-" + System.currentTimeMillis() + extension;
    }
}
