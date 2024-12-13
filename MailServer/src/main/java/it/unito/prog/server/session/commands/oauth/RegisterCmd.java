package it.unito.prog.server.session.commands.oauth;

import java.util.List;

import org.json.JSONObject;

import it.unito.prog.lib.enums.ServerResponse;
import it.unito.prog.server.session.Session;
import it.unito.prog.server.session.commands.CommandExecutor;

public class RegisterCmd implements CommandExecutor{

    @Override
    public void handle(Session session, List<String> params) {
        if(session.isLoggedIn()) {
            session.sendResponse(ServerResponse.UNKNOWN);
            return;
        }

        String param = params.get(0);

        JSONObject json = new JSONObject(param);

        String email = json.getString("email");
        String password = json.getString("password");

        if(email == null || password == null) {
            session.sendResponse(ServerResponse.UNKNOWN);
            return;
        }

        if(email.isEmpty() || password.isEmpty()) {
            session.sendResponse(ServerResponse.UNKNOWN);
            return;
        }

        JSONObject user = new JSONObject()
            .put("email", email)
            .put("password", password);

        if(session.register(user)) {
            session.sendResponse(ServerResponse.OK);
        } else {
            session.sendResponse(ServerResponse.UNKNOWN);
        }
    }
    
}
