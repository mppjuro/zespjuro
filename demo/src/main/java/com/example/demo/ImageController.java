package com.example.demo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.io.*;
import java.nio.file.Files;

/**
 * Controller for handling image upload and conversion operations.
 * <p>This controller provides endpoints to upload binary photos, convert local images, etc.</p>
 *
 * @version 0.0
 * @author tymonros
 */
@RestController
@RequestMapping("/api/image")
@Tag(name = "Image operations", description = "Uploading, converting images.")
public class ImageController {

    @Autowired
    private ImageConvertingService conversionService;

    /**
     * Endpoint for image format conversion.
     *
     * @param photoData binary photo to be uploaded.
     * @return ResponseEntity containing success or error message.
     */

    @Operation(summary = "Uploads binary photo", description = "Accepts image in binary format and saves it to server")
    @PostMapping("/upload-photo-binary")
    public ResponseEntity<String> uploadPhotoBinary(@RequestBody byte[] photoData) {
        try {
            String uploadDir = "uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            // Generate a unique filename or use a fixed one
            String fileName = "photo-" + System.currentTimeMillis() + ".jpg";
            File destination = new File(uploadDir + fileName);

            // Write the binary data directly
            Files.write(destination.toPath(), photoData);

            return ResponseEntity.ok("Photo uploaded and saved as " + fileName);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving photo: " + e.getMessage());
        }
    }

    /**
     * Endpoint for local image (currently hardcoded) format conversion from .JPG to .PNG format
     * @return ResponseEntity indicating success or failure.
     */
    @Operation(summary = "Converts local image", description = "Converts local (hardcoded) image")
    @GetMapping("/convertLocalImage")
    public ResponseEntity<String> convertLocalImage() {
        try {
            String filePath = "C:\\Users\\pc\\Desktop\\Studia\\year3\\zespolowa\\background.jpg";
            String outputPath = "C:\\Users\\pc\\Desktop\\Studia\\year3\\zespolowa\\image.png";

            // Verify file existence
            File inputFile = new File(filePath);
            if (!inputFile.exists()) {
                return ResponseEntity.badRequest().body("Input file does not exist");
            }

            byte[] pngData = conversionService.convertToPng(new FileInputStream(inputFile));

            try (FileOutputStream fileOutputStream = new FileOutputStream(outputPath)) {
                fileOutputStream.write(pngData);
            }

            return ResponseEntity.ok("Local image converted successfully");

        } catch (IOException e) {
            e.printStackTrace(); // Add logging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error converting image: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace(); // Add logging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Unexpected error: " + e.getMessage());
        }
    }
}
