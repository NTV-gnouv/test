package it.unito.prog.controllers;

import java.net.URL;
import java.util.ResourceBundle;

import org.json.JSONObject;

import it.unito.prog.Main;
import it.unito.prog.lib.enums.Command;
import it.unito.prog.lib.enums.ServerResponse;
import it.unito.prog.scene.AppScene;
import it.unito.prog.utils.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;

public class RegisterController extends BaseController {

    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField1;

    @FXML
    private TextField passwordField2;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO Auto-generated method stub
    }

    @FXML void registerAction(ActionEvent event) {
        if(emailField.getText().isEmpty() || passwordField1.getText().isEmpty() || passwordField2.getText().isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
            return;
        }

        if(!passwordField1.getText().equals(passwordField2.getText())) {
            AlertUtil.showError("Passwords do not match.");
            return;
        }

        JSONObject user = new JSONObject()
            .put("email", emailField.getText())
            .put("password", passwordField1.getText());

        clientMail().sendCmd(Command.REGISTER, (response, args) -> {
            if(response != ServerResponse.OK) {
                AlertUtil.showError(response.toString());
                return;
            }
            Main.setCurrentUser(user.getString("email"), user.getString("password"));
            // PlatformUtil.safeRun(() -> {
            //     try {
            //         Main.AppFx.initMainScene();
            //     } catch (IOException e) {
            //         e.printStackTrace();
            //     }
            // });
            sceneSwitcher().switchTo(AppScene.MAIN);
        }, user.toString());
    }
    
    @FXML
    public void regToLogin(ActionEvent event) {
        sceneSwitcher().switchTo(AppScene.HOME);
    }
}
