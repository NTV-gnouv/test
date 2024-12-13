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

public class HomeController extends BaseController {
    @FXML
    private TextField emailField;

    @FXML
    private TextField passwordField;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // TODO Auto-generated method stub
    }

    @FXML
    public void loginAction(ActionEvent event) {
        if(emailField.getText().isEmpty() || passwordField.getText().isEmpty()) {
            AlertUtil.showError("Please fill in all fields.");
        }

        JSONObject user = new JSONObject()
            .put("email", emailField.getText())
            .put("password", passwordField.getText());

        clientMail().sendCmd(Command.LOGIN, (response, args) -> {
            if(response != ServerResponse.OK) {
                AlertUtil.showError(response.toString());
                return;
            }
            Main.setCurrentUser(user.getString("email"), user.getString("password"));
            sceneSwitcher().switchTo(AppScene.MAIN);
        }, user.toString());
    }

    @FXML
    public void loginToReg(ActionEvent event) {
        sceneSwitcher().switchTo(AppScene.REGISTER);
    }
}
