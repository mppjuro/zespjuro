package com.example.demo;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Service
public class ImageConvertingService {

    /**
     * Converts an image file to a BufferedImage and then to PNG format.
     *
     * @param file The MultipartFile containing the image data
     * @return byte[] of the image in PNG format
     * @throws IOException if an error occurs during reading or writing the image
     */
    public byte[] convertToPng(MultipartFile file) throws IOException {
        BufferedImage image = ImageIO.read(file.getInputStream());
        if (image == null) {
            throw new IOException("File is not a valid image");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos); // Changed from "bmp" to "png"
        return baos.toByteArray();
    }

    public byte[] convertToPng(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);
        if (image == null) {
            throw new IOException("File is not a valid image");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos); // Changed from "bmp" to "png"
        return baos.toByteArray();
    }
}
