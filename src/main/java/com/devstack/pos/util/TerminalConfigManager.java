package com.devstack.pos.util;

import com.devstack.pos.entity.Business;
import com.devstack.pos.entity.PosTerminal;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

public class TerminalConfigManager {

    private static final String DIR_NAME = ".kumarapos";
    private static final String FILE_NAME = "terminal.properties";

    private static Properties properties = new Properties();
    private static boolean isLoaded = false;

    private static File getConfigFile() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, DIR_NAME);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return new File(dir, FILE_NAME);
    }

    public static synchronized void load() {
        File file = getConfigFile();
        if (file.exists()) {
            try (FileInputStream in = new FileInputStream(file)) {
                properties.load(in);
                isLoaded = true;
            } catch (IOException e) {
                System.err.println("Failed to read terminal configuration: " + e.getMessage());
            }
        } else {
            isLoaded = true;
        }
    }

    public static boolean isPaired() {
        if (!isLoaded) load();
        return Boolean.parseBoolean(properties.getProperty("terminal.is_paired", "false"));
    }

    public static Long getBusinessId() {
        if (!isLoaded) load();
        String val = properties.getProperty("terminal.business_id");
        return val != null ? Long.parseLong(val) : null;
    }

    public static String getBusinessCode() {
        if (!isLoaded) load();
        return properties.getProperty("terminal.business_code", "N/A");
    }

    public static String getBusinessName() {
        if (!isLoaded) load();
        return properties.getProperty("terminal.business_name", "Kumara POS");
    }

    public static Long getTerminalId() {
        if (!isLoaded) load();
        String val = properties.getProperty("terminal.terminal_id");
        return val != null ? Long.parseLong(val) : null;
    }

    public static String getTerminalCode() {
        if (!isLoaded) load();
        return properties.getProperty("terminal.terminal_code", "TERM-01");
    }

    public static String getTerminalName() {
        if (!isLoaded) load();
        return properties.getProperty("terminal.terminal_name", "Main Register");
    }

    public static String getPairingKey() {
        if (!isLoaded) load();
        return properties.getProperty("terminal.pairing_key", "");
    }

    public static String getDeviceUuid() {
        if (!isLoaded) load();
        String uuid = properties.getProperty("terminal.device_uuid");
        if (uuid == null || uuid.isBlank()) {
            uuid = UUID.randomUUID().toString();
            properties.setProperty("terminal.device_uuid", uuid);
        }
        return uuid;
    }

    public static synchronized void saveConfig(Business business, PosTerminal terminal) {
        properties.setProperty("terminal.is_paired", "true");
        properties.setProperty("terminal.business_id", String.valueOf(business.getId()));
        properties.setProperty("terminal.business_code", business.getBusinessCode());
        properties.setProperty("terminal.business_name", business.getName());
        properties.setProperty("terminal.terminal_id", String.valueOf(terminal.getId()));
        properties.setProperty("terminal.terminal_code", terminal.getTerminalCode());
        properties.setProperty("terminal.terminal_name", terminal.getTerminalName());
        properties.setProperty("terminal.pairing_key", terminal.getPairingKey());
        
        if (!properties.containsKey("terminal.device_uuid")) {
            properties.setProperty("terminal.device_uuid", UUID.randomUUID().toString());
        }

        File file = getConfigFile();
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, "KumaraPOS Terminal Pairing Hardware Configuration");
            isLoaded = true;
            System.out.println(">>> Terminal paired successfully to " + business.getName() + " (" + terminal.getTerminalName() + ")");
        } catch (IOException e) {
            System.err.println("Failed to save terminal configuration: " + e.getMessage());
        }
    }

    public static synchronized void unpair() {
        properties.clear();
        properties.setProperty("terminal.is_paired", "false");
        File file = getConfigFile();
        if (file.exists()) {
            file.delete();
        }
        isLoaded = false;
        System.out.println(">>> Terminal unpaired successfully.");
    }
}
