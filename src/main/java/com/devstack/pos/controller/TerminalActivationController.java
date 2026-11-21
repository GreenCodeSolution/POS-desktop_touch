package com.devstack.pos.controller;

import com.devstack.pos.Initialize;
import com.devstack.pos.entity.PosTerminal;
import com.devstack.pos.service.TerminalService;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXTextField;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class TerminalActivationController implements Initializable {

    @FXML public JFXTextField txtBusinessCode;
    @FXML public JFXTextField txtPairingKey;
    @FXML public Label lblError;
    @FXML public Label lblStatus;
    @FXML public JFXButton btnConnect;
    @FXML public ProgressIndicator progressIndicator;

    private final TerminalService terminalService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        lblError.setVisible(false);
        lblStatus.setText("Ready to pair with Cloud Database");
        progressIndicator.setVisible(false);

        txtPairingKey.setOnAction(this::btnConnectOnAction);
    }

    @FXML
    public void btnConnectOnAction(ActionEvent event) {
        String code = txtBusinessCode.getText();
        String key = txtPairingKey.getText();

        if (code == null || code.trim().isEmpty()) {
            showError("Please enter your Business Code (e.g. KUMARA-01).");
            return;
        }

        if (key == null || key.trim().isEmpty()) {
            showError("Please enter your Terminal Pairing Key (e.g. 111-222).");
            return;
        }

        lblError.setVisible(false);
        btnConnect.setDisable(true);
        progressIndicator.setVisible(true);
        lblStatus.setText("Authenticating with Cloud Server...");

        new Thread(() -> {
            try {
                PosTerminal pairedTerminal = terminalService.pairTerminal(code, key);

                Platform.runLater(() -> {
                    lblStatus.setText("Connected! Register: " + pairedTerminal.getTerminalName());
                    // Transition to Login Form
                    try {
                        loadLoginScreen();
                    } catch (Exception e) {
                        showError("Failed to open Login screen: " + e.getMessage());
                        btnConnect.setDisable(false);
                        progressIndicator.setVisible(false);
                    }
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError(e.getMessage());
                    btnConnect.setDisable(false);
                    progressIndicator.setVisible(false);
                    lblStatus.setText("Pairing failed. Please re-check credentials.");
                });
            }
        }).start();
    }

    private void showError(String message) {
        lblError.setText(message);
        lblError.setVisible(true);
    }

    private void loadLoginScreen() throws IOException {
        Stage stage = Initialize.getPrimaryStage();
        FXMLLoader loader = new FXMLLoader();
        var fxmlUrl = getClass().getResource("/com/devstack/pos/view/LoginForm.fxml");
        loader.setLocation(fxmlUrl);
        loader.setControllerFactory(Initialize.getSpringContext()::getBean);

        Scene scene = new Scene(loader.load());
        var cssUrl = getClass().getResource("/com/devstack/pos/view/styles/pos-styles.css");
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }

        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();
    }
}
