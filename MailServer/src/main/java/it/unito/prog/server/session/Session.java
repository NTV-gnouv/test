package it.unito.prog.server.session;

import it.unito.prog.lib.enums.ServerResponse;
import it.unito.prog.lib.objects.User;
import org.json.JSONObject;

public interface Session extends Runnable {
    boolean login(JSONObject auth);
    
    boolean register(JSONObject auth);

    boolean isLoggedIn();

    boolean setUser(JSONObject auth);

    User getUser();

    void sendResponse(ServerResponse serverResponse, String... params);

    void close();
}
