package com.devstack.pos.service;

import com.devstack.pos.entity.SystemSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotifyLkSmsService {

    private final SystemSettingsService systemSettingsService;

    @Value("${notify.lk.user-id:}")
    private String defaultUserId;

    @Value("${notify.lk.api-key:}")
    private String defaultApiKey;

    @Value("${notify.lk.sender-id:NotifyDEMO}")
    private String defaultSenderId;

    @Value("${notify.lk.enabled:true}")
    private boolean defaultEnabled;

    private static final String NOTIFY_LK_ENDPOINT = "https://app.notify.lk/api/v1/send";
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Send order receipt SMS asynchronously via Notify.lk (non-blocking for checkout speed)
     */
    public void sendOrderReceiptSmsAsync(String rawContact, String customerName, Long billCode, double totalAmount, String businessName, Long businessId) {
        sendOrderReceiptSmsAsync(rawContact, customerName, billCode, totalAmount, null, null, businessName, businessId);
    }

    public void sendOrderReceiptSmsAsync(String rawContact, String customerName, Long billCode, double totalAmount, Double pointsEarned, Double totalPoints, String businessName, Long businessId) {
        CompletableFuture.runAsync(() -> {
            try {
                sendOrderReceiptSms(rawContact, customerName, billCode, totalAmount, pointsEarned, totalPoints, businessName, businessId);
            } catch (Exception e) {
                log.error("[Notify.lk SMS Error] Failed to dispatch SMS receipt: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * Synchronous send logic
     */
    public boolean sendOrderReceiptSms(String rawContact, String customerName, Long billCode, double totalAmount, String businessName, Long businessId) {
        return sendOrderReceiptSms(rawContact, customerName, billCode, totalAmount, null, null, businessName, businessId);
    }

    public boolean sendOrderReceiptSms(String rawContact, String customerName, Long billCode, double totalAmount, Double pointsEarned, Double totalPoints, String businessName, Long businessId) {
        String formattedTo = formatSriLankanPhoneNumber(rawContact);
        if (formattedTo == null) {
            log.info("[Notify.lk SMS] No valid Sri Lankan mobile number provided: '{}'. SMS receipt skipped.", rawContact);
            return false;
        }

        // 1. Resolve configuration (Tenant settings take precedence over application.properties)
        SystemSettings settings = (businessId != null) 
                ? systemSettingsService.getSystemSettingsForBusiness(businessId)
                : systemSettingsService.getSystemSettings();

        boolean enabled = defaultEnabled;
        String userId = defaultUserId;
        String apiKey = defaultApiKey;
        String senderId = defaultSenderId;

        if (settings != null) {
            if (settings.getSmsEnabled() != null) {
                enabled = settings.getSmsEnabled();
            }
            if (settings.getSmsUserId() != null && !settings.getSmsUserId().isBlank()) {
                userId = settings.getSmsUserId().trim();
            }
            if (settings.getSmsApiKey() != null && !settings.getSmsApiKey().isBlank()) {
                apiKey = settings.getSmsApiKey().trim();
            }
            if (settings.getSmsSenderId() != null && !settings.getSmsSenderId().isBlank()) {
                senderId = settings.getSmsSenderId().trim();
            }
        }

        if (!enabled) {
            log.info("[Notify.lk SMS] SMS notifications are disabled in settings. Skipping.");
            return false;
        }

        if (userId == null || userId.isBlank() || apiKey == null || apiKey.isBlank()) {
            log.warn("[Notify.lk SMS] User ID or API Key not configured. Please set in Web Admin -> Settings or application.properties.");
            return false;
        }

        // 2. Build message text with Order Points and Total Accumulated Points
        String store = (businessName != null && !businessName.isBlank()) 
                ? businessName 
                : (settings != null && settings.getBusinessName() != null ? settings.getBusinessName() : "Cloud POS");
        String name = (customerName != null && !customerName.isBlank()) ? customerName : "Valued Customer";

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Dear %s, thank you for shopping at %s! Bill #%s. Total: Rs. %,.2f.",
                name, store, billCode != null ? billCode : "-", totalAmount));

        if (pointsEarned != null && pointsEarned > 0) {
            sb.append(String.format(" Pts Earned: +%.1f.", pointsEarned));
        }
        if (totalPoints != null && totalPoints > 0) {
            sb.append(String.format(" Total Pts: %.1f.", totalPoints));
        }
        sb.append(" Thank you!");
        String message = sb.toString();

        // 3. Dispatch to Notify.lk
        return callNotifyLkApi(userId, apiKey, senderId, formattedTo, message);
    }

    private boolean callNotifyLkApi(String userId, String apiKey, String senderId, String to, String message) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("user_id", userId);
            params.put("api_key", apiKey);
            params.put("sender_id", senderId);
            params.put("to", to);
            params.put("message", message);

            String formBody = params.entrySet().stream()
                    .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "=" +
                              URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(NOTIFY_LK_ENDPOINT))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(formBody))
                    .timeout(Duration.ofSeconds(12))
                    .build();

            log.info("[Notify.lk SMS] Sending order receipt SMS to {} via Sender ID '{}'...", to, senderId);
            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                log.info("[Notify.lk SMS] Sent successfully! Response: {}", response.body());
                return true;
            } else {
                log.error("[Notify.lk SMS] Gateway returned HTTP {}: {}", response.statusCode(), response.body());
                return false;
            }
        } catch (Exception e) {
            log.error("[Notify.lk SMS] Network error calling Notify.lk API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Standardizes Sri Lankan mobile numbers into '947XXXXXXXX' format required by Notify.lk.
     */
    public static String formatSriLankanPhoneNumber(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;

        if (digits.startsWith("94") && digits.length() == 11) {
            return digits; // e.g. 94712345678
        }
        if (digits.startsWith("0") && digits.length() == 10) {
            return "94" + digits.substring(1); // 0712345678 -> 94712345678
        }
        if (digits.length() == 9 && (digits.startsWith("7") || digits.startsWith("1"))) {
            return "94" + digits; // 712345678 -> 94712345678
        }
        return digits.length() >= 9 ? digits : null;
    }
}
