package com.devstack.pos.service;

import com.devstack.pos.util.FileStorageUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class BackupService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Value("${spring.datasource.username}")
    private String dbUser;

    @Value("${spring.datasource.password}")
    private String dbPassword;

    public void createBackupAndSendEmail(String recipientEmail) throws Exception {
        String backupDir = FileStorageUtil.getBackupDirectory();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "backup_" + timestamp + ".sql";
        String filePath = backupDir + File.separator + fileName;

        // Create database dump
        createDbDump(filePath);

        // Send email with attachment
        sendEmailWithAttachment(recipientEmail, filePath, fileName);
    }

    private void createDbDump(String filePath) throws IOException, InterruptedException {
        // Extract database name from URL (e.g.,
        // jdbc:postgresql://localhost:5432/robotikka)
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        ProcessBuilder processBuilder = new ProcessBuilder(
                "pg_dump",
                "-U", dbUser,
                "-h", "localhost",
                "--clean", // Include commands to DROP database objects
                "--if-exists",
                "-f", filePath,
                dbName);

        // Set password using environment variable to avoid interactive prompt
        processBuilder.environment().put("PGPASSWORD", dbPassword);

        Process process = processBuilder.start();
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("pg_dump failed with exit code " + exitCode);
        }
    }

    private void sendEmailWithAttachment(String to, String filePath, String fileName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setFrom(senderEmail);
        helper.setTo(to);
        helper.setSubject(
                "Database Backup - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        helper.setText("Please find the attached database backup file for the POS system.");

        FileSystemResource file = new FileSystemResource(new File(filePath));
        helper.addAttachment(fileName, file);

        mailSender.send(message);
    }

    public void restoreDatabase(File backupFile) throws IOException, InterruptedException {
        // Extract database name from URL
        String dbName = dbUrl.substring(dbUrl.lastIndexOf("/") + 1);
        if (dbName.contains("?")) {
            dbName = dbName.substring(0, dbName.indexOf("?"));
        }

        ProcessBuilder processBuilder;

        if (backupFile.getName().endsWith(".sql")) {
            // Standard SQL format restore (most compatible)
            processBuilder = new ProcessBuilder(
                    "psql",
                    "-U", dbUser,
                    "-h", "localhost",
                    "-d", dbName,
                    "-f", backupFile.getAbsolutePath());
        } else {
            // Custom dump format restore
            processBuilder = new ProcessBuilder(
                    "pg_restore",
                    "-U", dbUser,
                    "-h", "localhost",
                    "-d", dbName,
                    "--clean",
                    "--if-exists",
                    backupFile.getAbsolutePath());
        }

        // Set password using environment variable
        processBuilder.environment().put("PGPASSWORD", dbPassword);

        // Redirect error stream so we can see what happened if it fails
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Read output for debugging
        java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = process.waitFor();

        if (exitCode != 0) {
            System.err.println("Database process failed:\n" + output.toString());
            throw new IOException("Database operation failed (Code " + exitCode + "). Check logs for details.");
        }
    }
}
