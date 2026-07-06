package com.docportal.docportal.services;

import com.docportal.docportal.exceptions.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path root;
    private final List<String> allowedExtensions;

    public FileStorageService(
            @Value("${docportal.storage.location}") String location,
            @Value("${docportal.storage.allowed-extensions}") String allowedExtensions) {
        this.root = Paths.get(location).toAbsolutePath().normalize();
        this.allowedExtensions = Arrays.stream(allowedExtensions.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .toList();
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new IllegalStateException("Impossible de créer le dossier de stockage : " + this.root, e);
        }
    }

    /** Valide le fichier et le stocke sous un nom technique unique. Retourne le nom stocké. */
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Aucun fichier fourni");
        }

        String originalName = Paths.get(file.getOriginalFilename() == null ? "" : file.getOriginalFilename())
                .getFileName().toString();
        if (originalName.isBlank()) {
            throw ApiException.badRequest("Nom de fichier invalide");
        }
        if (originalName.matches(".*[<>:\"|?*\\\\/].*")) {
            throw ApiException.badRequest("Le nom du fichier contient des caractères interdits");
        }

        String extension = extensionOf(originalName);
        if (!allowedExtensions.contains(extension)) {
            throw ApiException.badRequest(
                    "Extension non autorisée : ." + extension + " (autorisées : " + String.join(", ", allowedExtensions) + ")");
        }

        String storedName = UUID.randomUUID() + "." + extension;
        try {
            Files.copy(file.getInputStream(), root.resolve(storedName), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Échec de l'enregistrement du fichier", e);
        }

        return new StoredFile(originalName, storedName, root.resolve(storedName).toString(), extension);
    }

    public Resource load(String storedFileName) {
        try {
            Path path = root.resolve(storedFileName).normalize();
            if (!path.startsWith(root)) {
                throw ApiException.forbidden("Chemin de fichier invalide");
            }
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw ApiException.notFound("Fichier physique introuvable");
            }
            return resource;
        } catch (IOException e) {
            throw new IllegalStateException("Échec de la lecture du fichier", e);
        }
    }

    public void deleteQuietly(String storedFileName) {
        try {
            Files.deleteIfExists(root.resolve(storedFileName).normalize());
        } catch (IOException ignored) {
            // suppression physique best-effort : la suppression logique en base fait foi
        }
    }

    private String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw ApiException.badRequest("Le fichier doit avoir une extension");
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    public record StoredFile(String originalName, String storedName, String path, String extension) {
    }
}
