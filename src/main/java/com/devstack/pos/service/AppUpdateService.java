package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * OTA (Over-The-Air) Auto-Update Service for the Desktop POS application.
 *
 * HOW IT WORKS:
 * ─────────────────────────────────────────────────────────
 * 1. You fix a bug and bump the version in pom.xml + application.properties
 * 2. Run deploy-installer.ps1 → get KumaraPOS-x.x.x.exe
 * 3. Upload the .exe to your server (or GitHub Releases, Google Drive, etc.)
 * 4. Run this SQL:
 *      UPDATE system_settings
 *      SET latest_app_version = '1.0.1',
 *          update_release_notes = 'Fixed receipt printer bug',
 *          update_download_url = 'https://your-server.com/updates/KumaraPOS-1.0.1.exe',
 *          update_mandatory = false
 *      WHERE is_active = true;
 * 5. Next time the cashier opens the app → popup appears →
 *    clicks "Update Now" → .exe downloads automatically → installer runs → done!
 * ─────────────────────────────────────────────────────────
 */

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AppUpdateService {

    private final SystemSettingsService systemSettingsService;

    @Value("${app.version:1.0.0}")
    private String currentAppVersion;

    // ==================== VERSION CHECK ====================

    /**
     * Check if a newer version is available based on system_settings table
     */
    public boolean isUpdateAvailable() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            if (settings == null || settings.getLatestAppVersion() == null 
                    || settings.getLatestAppVersion().isBlank()) {
                return false;
            }

            String latestVersion = settings.getLatestAppVersion().trim();
            String current = currentAppVersion != null ? currentAppVersion.trim() : "1.0.0";
            
            boolean isNewer = compareVersions(latestVersion, current) > 0;

            if (isNewer) {
                log.info("Update available! Current: v{}, Latest: v{}", 
                        currentAppVersion, latestVersion);
            }

            return isNewer;
        } catch (Exception e) {
            log.warn("Failed to check for updates: {}", e.getMessage());
            return false;
        }
    }

    public boolean isUpdateMandatory() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            return settings != null && Boolean.TRUE.equals(settings.getUpdateMandatory());
        } catch (Exception e) {
            return false;
        }
    }

    public String getLatestVersion() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            return settings != null && settings.getLatestAppVersion() != null 
                    ? settings.getLatestAppVersion().trim() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public String getReleaseNotes() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            return settings != null && settings.getUpdateReleaseNotes() != null 
                    ? settings.getUpdateReleaseNotes() : "Bug fixes and improvements.";
        } catch (Exception e) {
            return "Bug fixes and improvements.";
        }
    }

    public String getDownloadUrl() {
        try {
            SystemSettings settings = systemSettingsService.getSystemSettings();
            return settings != null && settings.getUpdateDownloadUrl() != null 
                    ? settings.getUpdateDownloadUrl().trim() : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String getCurrentVersion() {
        return currentAppVersion;
    }

    /**
     * Check if there's a download URL configured for the update
     */
    public boolean hasDownloadUrl() {
        String url = getDownloadUrl();
        return url != null && !url.isBlank();
    }

    // ==================== DOWNLOAD & INSTALL ====================

    /**
     * Download the new installer (.exe) from the URL stored in the database.
     * Shows progress via the progressCallback (0.0 to 1.0).
     *
     * @param progressCallback called with download progress (0.0 to 1.0)
     * @return Path to the downloaded .exe file
     * @throws IOException if download fails
     */
    public Path downloadUpdate(Consumer<Double> progressCallback) throws IOException, InterruptedException {
        String downloadUrl = getDownloadUrl();
        if (downloadUrl == null || downloadUrl.isBlank()) {
            throw new IOException("No download URL configured for the update.");
        }

        log.info("Starting download from: {}", downloadUrl);

        // Create temp directory for the download
        Path tempDir = Files.createTempDirectory("kumarapos-update-");
        String fileName = extractFileName(downloadUrl);
        Path downloadPath = tempDir.resolve(fileName);

        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS) // Follow redirects (Google Drive, GitHub, etc.)
                .connectTimeout(Duration.ofSeconds(30))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(downloadUrl))
                .timeout(Duration.ofMinutes(10)) // Max 10 min for large files
                .GET()
                .build();

        // First, get the file size with a HEAD request (for progress bar)
        long totalSize = -1;
        try {
            HttpRequest headRequest = HttpRequest.newBuilder()
                    .uri(URI.create(downloadUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .build();
            HttpResponse<Void> headResponse = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
            totalSize = headResponse.headers().firstValueAsLong("content-length").orElse(-1);
            log.info("Update file size: {} MB", totalSize > 0 ? String.format("%.1f", totalSize / 1024.0 / 1024.0) : "unknown");
        } catch (Exception e) {
            log.warn("Could not determine file size (progress bar may not work): {}", e.getMessage());
        }

        // Download the file
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() != 200) {
            throw new IOException("Download failed with HTTP status: " + response.statusCode());
        }

        // Stream to file with progress tracking
        long finalTotalSize = totalSize > 0 ? totalSize : response.headers().firstValueAsLong("content-length").orElse(-1);
        
        try (InputStream inputStream = response.body()) {
            byte[] buffer = new byte[8192];
            long downloaded = 0;
            int bytesRead;

            var outputStream = Files.newOutputStream(downloadPath);
            try {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;

                    // Report progress
                    if (progressCallback != null) {
                        if (finalTotalSize > 0) {
                            double progress = (double) downloaded / finalTotalSize;
                            progressCallback.accept(Math.min(progress, 1.0));
                        } else {
                            // If size unknown, send a special value or just alternate tiny progress
                            // to keep UI moving. -1.0 can signal indeterminate mode in some UIs.
                            progressCallback.accept(-1.0); 
                        }
                    }
                }
            } finally {
                outputStream.close();
            }
        }

        log.info("Download complete: {} ({} bytes)", downloadPath.getFileName(), Files.size(downloadPath));

        // Final progress
        if (progressCallback != null) {
            progressCallback.accept(1.0);
        }

        return downloadPath;
    }

    /**
     * Launch the downloaded installer and exit the current application.
     * The Windows installer will handle uninstalling the old version
     * and installing the new one automatically.
     *
     * @param installerPath Path to the downloaded .exe installer
     */
    public void launchInstallerAndExit(Path installerPath) throws IOException {
        log.info("Launching installer: {}", installerPath);

        if (!Files.exists(installerPath)) {
            throw new IOException("Installer file not found: " + installerPath);
        }

        // On Windows, use 'cmd /c start' to launch the installer 
        // as a completely separate process so we can exit safely.
        String[] cmd = {"cmd", "/c", "start", "/wait", installerPath.toString()};
        
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.directory(installerPath.getParent().toFile());
        pb.start();

        // Exit the app so the installer can replace files
        javafx.application.Platform.exit();
        System.exit(0);
    }

    // ==================== HELPERS ====================

    /**
     * Extract filename from a URL
     */
    private String extractFileName(String url) {
        String path = URI.create(url).getPath();
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        
        // Default name if URL doesn't have a clear filename
        if (fileName.isBlank() || !fileName.contains(".")) {
            fileName = "KumaraPOS-" + getLatestVersion() + ".exe";
        }
        
        return fileName;
    }

    /**
     * Compare two semantic version strings (e.g., "1.0.0" vs "1.0.1")
     * @return positive if v1 > v2, negative if v1 < v2, 0 if equal
     */
    private int compareVersions(String v1, String v2) {
        String[] parts1 = v1.split("\\.");
        String[] parts2 = v2.split("\\.");
        int maxLength = Math.max(parts1.length, parts2.length);
        for (int i = 0; i < maxLength; i++) {
            int num1 = i < parts1.length ? parseVersionPart(parts1[i]) : 0;
            int num2 = i < parts2.length ? parseVersionPart(parts2[i]) : 0;
            if (num1 != num2) {
                return num1 - num2;
            }
        }
        return 0;
    }

    private int parseVersionPart(String part) {
        try {
            String numericPart = part.replaceAll("[^0-9].*", "");
            return numericPart.isEmpty() ? 0 : Integer.parseInt(numericPart);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
