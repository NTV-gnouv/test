package it.unito.prog.controllers;

import it.unito.prog.Main;
import it.unito.prog.controllers.components.ClientStatusLabel;
import it.unito.prog.controllers.components.ServerStatusDot;
import it.unito.prog.lib.enums.Command;
import it.unito.prog.lib.enums.MailFolder;
import it.unito.prog.lib.enums.ServerResponse;
import it.unito.prog.lib.objects.Email;
import it.unito.prog.lib.utils.MailUtil;
import it.unito.prog.lib.utils.Serializer;
import it.unito.prog.model.modules.observables.Observables;
import it.unito.prog.scene.AppScene;
import it.unito.prog.utils.AlertUtil;
import it.unito.prog.utils.PlatformUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.shape.Circle;
import javafx.scene.web.HTMLEditor;
import javafx.stage.FileChooser;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

public final class ComposeController extends BaseController {
    String currentUser;

    @FXML
    private TextField senderField;

    @FXML
    private TextField recipientField;

    @FXML
    private TextField subjectField;

    @FXML
    private HTMLEditor htmlEditor;

    @FXML
    private Label clientStatusLabel;

    @FXML
    private Circle serverStatusCircle;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = Main.getCurrentUser().getString("email");
        // Set logged user as sender
        senderField.setText(currentUser);

        // Validate Email format
        recipientField.setOnKeyTyped(event -> validateRecipients(recipientField.getText(), true));

        // Init server status dot
        ServerStatusDot.bind(serverStatusCircle);

        // Init status label
        ClientStatusLabel.bind(clientStatusLabel);
    }

    public void loadEmailView(Email email, int mode) {
        PlatformUtil.safeRun(() -> {
            switch (mode) {
                case 0: // Forward
                    recipientField.setText("");
                    break;
                case 1: // ReplyTo
                    recipientField
                            .setText(email.getSender().equalsIgnoreCase(currentUser) ? email.getRecipients().get(0)
                                    : email.getSender());
                    subjectField.setText("RE: " + email.getSubject());
                    break;
                case 2: // ReplyToAll
                    List<String> recipients = email.getRecipients().stream()
                            .filter(s -> !s.equalsIgnoreCase(currentUser)).collect(Collectors.toList());
                    if (!email.getSender().equalsIgnoreCase(currentUser))
                        recipients.add(email.getSender());
                    recipientField.setText(String.join(", ", recipients));
                    subjectField.setText("RE: " + email.getSubject());
                    break;
            }

            String oldMessagePrefix = "</br>-----</br>> \"" + email.getSender() + "\" wrote:<br>";
            htmlEditor.setHtmlText(oldMessagePrefix + email.getBody());
        });
    }

    @FXML
    public void fileAction(ActionEvent actionEvent) throws IOException {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Attach file");

        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.gif"));

        File file = fileChooser.showOpenDialog(null);

        if (file != null) {
            String fileB64 = fileToB64(file);
            htmlEditor.setHtmlText(htmlEditor.getHtmlText() + "<img src=\"data:image/png;base64," + fileB64 + "\"/>");
        }
    }

    @FXML
    public void sendAction(ActionEvent actionEvent) {
        if (subjectField.getText().isBlank()) {
            AlertUtil.showError("Subject field is empty!");
            return;
        }

        if (validateRecipients(recipientField.getText(), false))
            sendEmail();
    }

    @FXML
    public void cancelAction(ActionEvent actionEvent) {
        clearAndSwitch();
    }

    private void sendEmail() {
        Email email = new Email(currentUser, getRecipients(), Instant.now().getEpochSecond(), subjectField.getText(),
                htmlEditor.getHtmlText());
        System.out.println(email.toString());
        String emailB64 = Serializer.writeToB64(email);

        clientMail().sendCmd(Command.SEND_EMAIL, (response, args) -> {
            if (response != ServerResponse.OK) {
                String responseStr = response.toString();
                setStatusLabel(responseStr);

                if (response == ServerResponse.USER_NOT_FOUND)
                    responseStr += " (" + args.get(0) + ")";
                AlertUtil.showError(responseStr);
                return;
            }

            if (getCurrentFolder() == MailFolder.OUTBOX) {
                email.setToRead(false);
                observablesManager().addListEntry(Observables.EMAIL_LIST, email.liteClone());
            }

            AlertUtil.showInfo("E-mail sent!");
            PlatformUtil.safeRun(this::clearAndSwitch);
        }, emailB64);
    }

    private void clearAndSwitch() {
        recipientField.setText("");
        subjectField.setText("");
        htmlEditor.setHtmlText("");

        sceneSwitcher().switchTo(AppScene.MAIN);
        setStatusLabel(BaseController.waitingForInput);
    }

    private boolean validateRecipients(String fieldText, boolean fromField) {
        if (fieldText.isBlank()) {
            String empty_recipient = "Recipient field is empty!";
            if (fromField)
                setStatusLabel(empty_recipient);
            else
                AlertUtil.showError(empty_recipient);
            return false;
        }

        List<String> recipients = getRecipients();
        for (String recipient : recipients) {
            if (recipient.isBlank())
                continue;

            if (recipient.equalsIgnoreCase(currentUser)) {
                String own_recipient = "You can't send email to yourself!";
                if (fromField)
                    setStatusLabel(own_recipient);
                else
                    AlertUtil.showError(own_recipient);
                return false;
            }
            if (!MailUtil.validate(recipient)) {
                String waf_err = recipient + " - " + ServerResponse.WRONG_ADDRESS_FORMAT.toString();
                if (fromField)
                    setStatusLabel(waf_err);
                else
                    AlertUtil.showError(waf_err);
                return false;
            }
        }

        if (fromField)
            setStatusLabel("OK");
        return true;
    }

    private List<String> getRecipients() {
        return Arrays.stream(recipientField.getText().split(","))
                .map(String::strip)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toList());
    }

    private String fileToB64(File file) throws IOException {
        byte[] fileContent = FileUtils.readFileToByteArray(file);
        String encodeString = Base64.getEncoder().encodeToString(fileContent);
        return encodeString;
    }
}
