package com.devstack.pos.controller;

import com.devstack.pos.entity.AppUser;
import com.devstack.pos.service.SessionManager;
import com.devstack.pos.service.TrialService;
import com.devstack.pos.service.UserService;
import com.devstack.pos.util.JwtUtil;
import com.devstack.pos.util.StageManager;
import com.devstack.pos.util.UserSessionData;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

@Component
@RequiredArgsConstructor
public class LoginFormController implements Initializable {
    public HBox context;
    public TextField txtEmail;
    public PasswordField txtPassword;

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final SessionManager sessionManager;
    private final TrialService trialService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Auto-fill username since field is hidden per user request
        txtEmail.setText("admin@gmail.com");

        // Initialize PIN to 1234 per user request
        txtPassword.setText("");

        // Ensure focus is on the password/PIN field
        javafx.application.Platform.runLater(() -> txtPassword.requestFocus());

        // Set up Enter key handler for password field - trigger login
        txtPassword.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                btnSignInOnAction(new ActionEvent());
            }
        });
    }

    // Signup is managed via the web portal — no create account from desktop

    @javafx.fxml.FXML
    public void btnKeypadPressed(ActionEvent event) {
        Object source = event.getSource();
        if (source instanceof javafx.scene.control.Button) {
            String text = ((javafx.scene.control.Button) source).getText();
            if (txtEmail.isFocused()) {
                txtEmail.appendText(text);
            } else {
                txtPassword.appendText(text);
                txtPassword.requestFocus();
            }
        }
    }

    @javafx.fxml.FXML
    public void btnKeypadClear(ActionEvent event) {
        if (txtEmail.isFocused()) {
            txtEmail.clear();
        } else {
            txtPassword.clear();
            txtPassword.requestFocus();
        }
    }

    @javafx.fxml.FXML
    public void btnKeypadBackspace(ActionEvent event) {
        if (txtEmail.isFocused()) {
            String current = txtEmail.getText();
            if (!current.isEmpty()) {
                txtEmail.setText(current.substring(0, current.length() - 1));
            }
        } else {
            String current = txtPassword.getText();
            if (!current.isEmpty()) {
                txtPassword.setText(current.substring(0, current.length() - 1));
            }
            txtPassword.requestFocus();
        }
    }

    public void btnSignInOnAction(ActionEvent actionEvent) {
        try {
            // Check trial version status before allowing login
            if (trialService.isTrialEnabled() && trialService.isTrialExpired()) {
                Alert trialAlert = new Alert(Alert.AlertType.ERROR);
                trialAlert.setTitle("Trial Period Expired");
                trialAlert.setHeaderText("Access Denied");
                trialAlert.setContentText(
                        "Your trial period has expired. Please contact the administrator to extend or activate the full version.\n\n"
                                +
                                "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n" +
                                "Status: " + trialService.getTrialStatusMessage());
                trialAlert.showAndWait();
                return;
            }

            AppUser appUser = userService.findUser(txtEmail.getText());
            if (appUser != null) {
                String jwtToken = userService.checkPassword(appUser.getEmail(), txtPassword.getText());
                if (!ObjectUtils.isEmpty(jwtToken)) {
                    // Clear previous session data safely
                    UserSessionData.clear();

                    // Store JWT token
                    UserSessionData.jwtToken = jwtToken;
                    UserSessionData.email = txtEmail.getText();

                    // Extract and store user roles from JWT token
                    java.util.List<String> roles = jwtUtil.getRolesFromToken(jwtToken);
                    UserSessionData.userRoles = new java.util.HashSet<>(roles);

                    // Restrict access to CASHIER role ONLY for this desktop application
                    if (!UserSessionData.isCashier()) {
                        UserSessionData.clear();
                        Alert accessAlert = new Alert(Alert.AlertType.ERROR);
                        accessAlert.setTitle("Access Denied");
                        accessAlert.setHeaderText("Insufficient Permissions");
                        accessAlert.setContentText("This desktop application is reserved for POS Cashiers only.\n\n" +
                                "Please log in via the Web Portal for management and administrative tasks.");
                        accessAlert.showAndWait();
                        return;
                    }

                    // Extract and store granular permissions from JWT token
                    java.util.List<String> permissions = jwtUtil.getPermissionsFromToken(jwtToken);
                    UserSessionData.permissions = new java.util.HashSet<>(permissions);

                    // Initialize last activity time
                    UserSessionData.updateLastActivity();

                    // Start session monitoring for inactivity and token expiration
                    sessionManager.startSessionMonitoring();

                    System.out.println("=== LOGIN SUCCESSFUL ===");
                    System.out.println("User: " + txtEmail.getText());
                    System.out.println("Roles from JWT: " + UserSessionData.userRoles);
                    System.out.println("Is Cashier: " + UserSessionData.isCashier());
                    System.out.println("Session monitoring started");
                    System.out.println("========================");

                    // Show trial warning ONLY if 7 days or less remaining
                    if (trialService.isTrialActive() && trialService.isTrialWarningPeriod()) {
                        long daysRemaining = trialService.getDaysRemaining();
                        Alert trialWarning = new Alert(Alert.AlertType.WARNING);
                        trialWarning.setTitle("Trial Version Warning");
                        trialWarning.setHeaderText("⚠️Trial Period Ending Soon");

                        String message;
                        if (daysRemaining == 0) {
                            message = "⚠️WARNING: Your trial period expires TODAY!\n\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version to avoid service interruption.";
                        } else if (daysRemaining == 1) {
                            message = "⚠️WARNING: Your trial period expires TOMORROW!\n\n" +
                                    "Days remaining: " + daysRemaining + " day\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version.";
                        } else {
                            message = "⚠️WARNING: Your trial period is ending soon!\n\n" +
                                    "Days remaining: " + daysRemaining + " days\n" +
                                    "Trial End Date: " + trialService.getTrialEndDateFormatted() + "\n\n" +
                                    "Please contact the administrator to extend or activate the full version before the trial expires.";
                        }

                        trialWarning.setContentText(message);
                        trialWarning.show();
                    }
                    // No popup if more than 7 days remaining

                    setUi("MainForm");
                } else {
                    new Alert(Alert.AlertType.WARNING, "check your password and try again!").show();
                }
            } else {
                new Alert(Alert.AlertType.WARNING, "User email not found!").show();
            }
        } catch (IOException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR, e.getMessage()).show();
        }
    }

    private void setUi(String url) throws IOException {
        Stage stage = (Stage) context.getScene().getWindow();
        FXMLLoader loader = new FXMLLoader();
        loader.setLocation(getClass().getResource("/com/devstack/pos/view/" + url + ".fxml"));
        loader.setControllerFactory(com.devstack.pos.PosApplication.getApplicationContext()::getBean);

        Parent root = loader.load();
        StageManager.loadFullScreenScene(stage, root);
    }
}
