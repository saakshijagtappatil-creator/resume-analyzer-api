package com.resumeanalyzer.api.util;

import com.resumeanalyzer.api.exception.InvalidFileException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Component
public class PdfTextExtractor {

    public String extractText(String filePath) {
        log.info("Extracting text from PDF: {}", filePath);

        try (PDDocument document = PDDocument.load(
                Files.newInputStream(Paths.get(filePath)))) {

            return extractTextFromDocument(document, filePath);

        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", filePath, e);
            throw new InvalidFileException(
                    "Failed to read PDF file: " + e.getMessage());
        }
    }

    public String extractTextFromBytes(byte[] pdfBytes) {
        log.info("Extracting text from PDF bytes ({} bytes)", pdfBytes.length);

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            return extractTextFromDocument(document, "byte-array");
        } catch (InvalidFileException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF bytes", e);
            throw new InvalidFileException(
                    "Failed to read PDF file: " + e.getMessage());
        }
    }

    private String extractTextFromDocument(PDDocument document, String source) throws IOException {
        if (document.isEncrypted()) {
            throw new InvalidFileException("PDF is encrypted and cannot be processed");
        }

        PDFTextStripper stripper = new PDFTextStripper();
        stripper.setSortByPosition(true);

        String text = stripper.getText(document);

        if (text == null || text.isBlank()) {
            throw new InvalidFileException(
                    "Could not extract text from PDF. " +
                    "The file may be scanned or image-based");
        }

        log.info("Successfully extracted {} characters from PDF ({})", text.length(), source);
        return text.trim();
    }

    public String generateFileHash(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(file.getBytes());
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new InvalidFileException(
                    "Could not read file for hashing: " + e.getMessage());
        }
    }

    public String generateFileHashFromPath(String filePath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(Paths.get(filePath));
            byte[] hashBytes = digest.digest(fileBytes);
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        } catch (IOException e) {
            throw new InvalidFileException(
                    "Could not read file for hashing: " + e.getMessage());
        }
    }
}