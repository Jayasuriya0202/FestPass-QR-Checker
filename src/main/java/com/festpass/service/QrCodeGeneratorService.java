package com.festpass.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.Base64;

@Service
public class QrCodeGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(QrCodeGeneratorService.class);
    private static final int DEFAULT_WIDTH = 250;
    private static final int DEFAULT_HEIGHT = 250;

    /**
     * Generates a Base64-encoded Data URL representing a PNG QR code image.
     * E.g. "data:image/png;base64,iVBORw0KGgoAAAANS..."
     */
    public String generateQrCodeDataUrl(String text) {
        byte[] imageBytes = generateQrCodeImageBytes(text, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        String base64 = Base64.getEncoder().encodeToString(imageBytes);
        return "data:image/png;base64," + base64;
    }

    /**
     * Generates raw PNG byte array of the QR code.
     */
    public byte[] generateQrCodeImageBytes(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            logger.error("Failed to generate QR code for text: {}", text, e);
            throw new RuntimeException("Error generating QR code image", e);
        }
    }
}
