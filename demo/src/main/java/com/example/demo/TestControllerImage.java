package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import java.io.FileInputStream;

@SpringBootTest
class ImageConversionServiceTest {

    @Autowired
    private ImageConvertingService conversionService;

    @Test
    void testConvertToBitmap() throws Exception {
        // Path to your test image file
        String imagePath = "C:\\Users\\pc\\Desktop\\Studia\\year3\\zespolowa\\background.jpg";
        MockMultipartFile file = new MockMultipartFile("image", "background.jpg",
                "image/jpeg", new FileInputStream(imagePath));

        // Use the service to convert the image
        byte[] result = conversionService.convertToPng(file);

        // Assertions or further processing here
    }
}


