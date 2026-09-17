package com.shoerp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.shoerp.common.BusinessException;
import com.shoerp.config.AppProperties;

@Service
public class FileStorageService {

    private static final DateTimeFormatter FOLDER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final Path root;

    public FileStorageService(AppProperties appProperties) {
        this.root = Paths.get(appProperties.getUploadDir()).toAbsolutePath().normalize();
    }

    public Path getRoot() {
        return root;
    }

    public String store(MultipartFile file, String category) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file is empty");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extensionOf(original);
        if (!isAllowedImage(extension)) {
            throw new BusinessException("Only image files (jpg, jpeg, png, gif, webp, bmp) are allowed");
        }
        String folder = sanitizeCategory(category) + "/" + LocalDate.now().format(FOLDER);
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            Path targetDir = root.resolve(folder).normalize();
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(filename).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("Invalid upload path");
            }
            file.transferTo(target);
        } catch (IOException ex) {
            throw new BusinessException(500, "Failed to store file: " + ex.getMessage());
        }
        return "/uploads/" + folder + "/" + filename;
    }

    private String sanitizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return "misc";
        }
        String cleaned = category.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "");
        return cleaned.isBlank() ? "misc" : cleaned;
    }

    private String extensionOf(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            return "";
        }
        return filename.substring(index + 1).toLowerCase(Locale.ROOT);
    }

    private boolean isAllowedImage(String extension) {
        return switch (extension) {
            case "jpg", "jpeg", "png", "gif", "webp", "bmp" -> true;
            default -> false;
        };
    }
}
