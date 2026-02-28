package com.devstack.pos;

import javafx.application.Application;
import lombok.Getter;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class PosApplication {

    @Getter
    private static ConfigurableApplicationContext applicationContext;

    public static void main(String[] args) {
        // Capture ALL exceptions
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("═══════════════════════════════════════════");
            System.err.println("UNCAUGHT EXCEPTION in thread: " + thread.getName());
            System.err.println("═══════════════════════════════════════════");
            throwable.printStackTrace(System.err);
            System.err.println("═══════════════════════════════════════════");
        });

        try {
            System.out.println(">>> STARTING POS DESKTOP APPLICATION <<<");

            // Start Spring Boot context with NO web server (desktop only)
            applicationContext = new SpringApplicationBuilder(PosApplication.class)
                    .headless(false)
                    .web(org.springframework.boot.WebApplicationType.NONE)
                    .run(args);

            // Set Spring context in Initialize class
            Initialize.setSpringContext(applicationContext);

            System.out.println(">>> Starting Cashier On-Premise UI (JavaFX)...");

            // Launch JavaFX application
            Application.launch(Initialize.class, args);

        } catch (Exception e) {
            System.err.println("═══════════════════════════════════════════");
            System.err.println("EXCEPTION IN MAIN:");
            System.err.println("═══════════════════════════════════════════");
            e.printStackTrace(System.err);
            System.err.println("═══════════════════════════════════════════");
        }
    }
}
