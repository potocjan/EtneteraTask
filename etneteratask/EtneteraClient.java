package etneteratask;

import java.util.List;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import org.json.*;

public class EtneteraClient {

    private final WebTarget webTarget;
    private final Client client;
    private static final String BASE_URI = "https://ikariera.etnetera.cz/veletrhy/";
    private static final String NICKNAME = "Molekulovej mozek";
    private static final String EMAIL = "janpotociar@email.cz";
    private static int SLOTS;
    private static String GAME_ID;
    private static int ranking;

    public EtneteraClient() {
        client = javax.ws.rs.client.ClientBuilder.newClient();
        webTarget = client.target(BASE_URI);
        ranking = getMyRanking();
    }

    public void registerUser(int slots) {
        SLOTS = slots;
        
        JSONObject dataToSendAsJSON = new JSONObject().put("nickname", NICKNAME);
        dataToSendAsJSON.put("email", EMAIL);
        dataToSendAsJSON.put("slots", SLOTS);

        String responseAsString = webTarget.path("/start").request(javax.ws.rs.core.MediaType.APPLICATION_JSON).post(Entity.json(dataToSendAsJSON.toString()), String.class);
        JSONObject responseAsJson = new JSONObject(responseAsString);
        GAME_ID = responseAsJson.optString("gameId");
    }

    public Evaluation guess(List<Integer> tip) {
        JSONObject dataToSendAsJson = new JSONObject();
        dataToSendAsJson.put("gameId", GAME_ID);
        dataToSendAsJson.put("guess", tip);

        String response = webTarget.path("/guess").request(javax.ws.rs.core.MediaType.APPLICATION_JSON).post(Entity.json(dataToSendAsJson.toString()), String.class);
        JSONObject responseAsJson = new JSONObject(response);
        JSONObject evaluation = responseAsJson.getJSONObject("evaluation");
        int black = Integer.parseInt(evaluation.optString("black"));
        int white = Integer.parseInt(evaluation.optString("white"));

        if (black == SLOTS) {
            throw new TaskCompleteException();
        }

        return new Evaluation(black, white);
    }

    public void close() {
        client.close();
    }

    public final int getMyRanking() {
        String responseAsString = webTarget.path("/ranking").request(javax.ws.rs.core.MediaType.APPLICATION_JSON).get(String.class);
        JSONObject responseAsJson = new JSONObject(responseAsString);
        JSONArray rankingAsArray = responseAsJson.getJSONArray("ranking");
        int myScore = 0;
        for (int i = 0; i < rankingAsArray.length(); i++) {
            JSONObject ranking = rankingAsArray.getJSONObject(i);
            if ("Molekulovej mozek".equals(ranking.optString("nickname"))) {
                myScore = Integer.parseInt(ranking.optString("score"));
                break;
            }
        }
        return myScore;
    }
}
