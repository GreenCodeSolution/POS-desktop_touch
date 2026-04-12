package com.devstack.pos;

import javafx.application.Application;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Main Entry Point for KumaraPOS.
 * This class now handles the ultra-fast startup by showing a 
 * splash screen immediately while Spring Boot loads in the background.
 */
@SpringBootApplication
public class PosApplication {

    public static void main(String[] args) {
        // Capture ALL exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("CRITICAL ERROR in thread: " + thread.getName());
            throwable.printStackTrace(System.err);
        });

        // 🚀 FAST STARTUP: Launch JavaFX first. 
        System.out.println(">>> LAUNCHING POS ENGINE <<<");
        Application.launch(Initialize.class, args);
    }

    /**
     * Helper method to allow other classes to access the Spring context.
     */
    public static ConfigurableApplicationContext getApplicationContext() {
        return Initialize.getSpringContext();
    }
}
