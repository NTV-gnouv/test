package it.unito.prog.server.session;

import it.unito.prog.Main;
import it.unito.prog.lib.enums.ServerResponse;
import it.unito.prog.lib.objects.User;
import it.unito.prog.lib.utils.MailUtil;
import it.unito.prog.server.session.commands.CommandManager;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

class SessionImpl implements Session {

    private final Socket connection;
    private final String clientAddr;

    private BufferedReader reader;
    private PrintWriter writer;

    private User user;

    public SessionImpl(Socket connection) {
        this.connection = connection;
        this.clientAddr = connection.getRemoteSocketAddress().toString().substring(1);
        this.user = null;
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            writer = new PrintWriter(connection.getOutputStream(), true);

            String request = reader.readLine();
            if (request.isBlank()) return;
            try {
                JSONObject jsonRequest = new JSONObject(request);
                log(request, true);
                CommandManager.getInstance().handle(this, jsonRequest);
            } catch (JSONException e) {
                sendResponse(ServerResponse.SEND_CMD_FAILED);
                log("WRONG FORMAT REQUEST: " + request, true);
            }
        } catch (IOException ignored) {
            /* Connection closed by the user or timeout */
        } finally {
            close();
        }
    }

    @Override
    public boolean login(JSONObject auth) {
        if (!(auth.has("email") && auth.has("password"))) {
            sendResponse(ServerResponse.ANONYMOUS_SESSION);
            return false;
        }

        String email = auth.getString("email");
        if (!MailUtil.validate(email)) {
            sendResponse(ServerResponse.WRONG_ADDRESS_FORMAT);
            return false;
        }

        User user;
        String password = auth.getString("password");
        boolean userExist = Main.appModel().databaseManager().checkUser(email);

        if(!userExist) {
            sendResponse(ServerResponse.USER_NOT_FOUND);
            return false;
        }

        user = Main.appModel().databaseManager().readUser(email);
        if(!user.getPassword().equals(password)) {
            sendResponse(ServerResponse.WRONG_PASSWORD);
            return false;
        }

        Main.appModel().synchronizeManager().createLock(user.getEmail());
        this.user = user;

        return true;
    }

    @Override
    public boolean register(JSONObject auth) {
        if (!(auth.has("email") && auth.has("password"))) {
            sendResponse(ServerResponse.ANONYMOUS_SESSION);
            return false;
        }

        String email = auth.getString("email");
        if (!MailUtil.validate(email)) {
            sendResponse(ServerResponse.WRONG_ADDRESS_FORMAT);
            return false;
        }

        User user;
        String password = auth.getString("password");
        boolean userExist = Main.appModel().databaseManager().checkUser(email);

        if(userExist) {
            sendResponse(ServerResponse.ALREADY_REGISTERED_USER);
            return false;
        }

        user = new User(email, password);
        if (!Main.appModel().databaseManager().saveUser(user)) {
            sendResponse(ServerResponse.IO_ERROR);
            return false;
        }

        Main.appModel().synchronizeManager().createLock(user.getEmail());
        this.user = user;

        return true;
    }

    @Override
    public boolean isLoggedIn() {
        return user != null;
    }

    @Override
    public boolean setUser(JSONObject auth) {

        if (!(auth.has("email") && auth.has("password"))) {
            sendResponse(ServerResponse.ANONYMOUS_SESSION);
            return false;
        }

        String email = auth.getString("email");
        if (!MailUtil.validate(email)) {
            sendResponse(ServerResponse.WRONG_ADDRESS_FORMAT);
            return false;
        }

        User user;
        String password = auth.getString("password");
        boolean userExist = Main.appModel().databaseManager().checkUser(email);

        if(!userExist) {
            sendResponse(ServerResponse.USER_NOT_FOUND);
            return false;
        }

        user = Main.appModel().databaseManager().readUser(email);
        if(!user.getPassword().equals(password)) {
            sendResponse(ServerResponse.WRONG_PASSWORD);
            return false;
        }

        Main.appModel().synchronizeManager().createLock(user.getEmail());
        this.user = user;

        return true;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void sendResponse(ServerResponse serverResponse, String... args) {
        JSONObject jsonObject = new JSONObject()
                .put("status", serverResponse.name())
                .put("args", args);
        String jsonString = jsonObject.toString();

        writer.println(jsonString);
        log(jsonString, false);
    }

    @Override
    public void close() {
        if (isLoggedIn()) {
            Main.appModel().synchronizeManager().deleteLock(user.getEmail());
        }

        try {
            reader.close();
            writer.close();
            connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String s, boolean client) {
        Main.printLn(clientAddr + (client ? " >> " : " << ") + s);
    }
}
