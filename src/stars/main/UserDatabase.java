package stars.main;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class UserDatabase {
    private final List<User> users;

    public UserDatabase() {
        this.users = new ArrayList<>();
    }

    public void addUser(User user) {
        users.add(user);
    }

    public boolean validate(String username, String password) {
        for (User user : users) {
            if (user.validate(username, password)) {
                return true;
            }
        }
        return false;
    }

    public static UserDatabase loadFromJSON(String filePath) throws IOException, ParseException {
        UserDatabase userDatabase = new UserDatabase();
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(filePath)) {
            JSONObject jsonData = (JSONObject) jsonParser.parse(reader);
            JSONArray usersArray = (JSONArray) jsonData.get("users");
            for (Object obj : usersArray) {
                JSONObject userObj = (JSONObject) obj;
                String username = (String) userObj.get("username");
                String password = (String) userObj.get("password");
                String projectTime = (String) userObj.get("projectTime");
                long finishedProjects = (long) userObj.get("finishedProjects");
                User user = new User(username, password, projectTime, finishedProjects);
                userDatabase.addUser(user);
            }
        }
        return userDatabase;
    }

    public void saveToJSON(String filePath) throws IOException {
        JSONArray usersArray = new JSONArray();
        for (User user : users) {
            JSONObject userObj = new JSONObject();
            userObj.put("username", user.getUsername());
            userObj.put("password", user.getPassword());
            userObj.put("projectTime", user.getProjectTime());
            userObj.put("finishedProjects", user.getFinishedProjects());
            usersArray.add(userObj);
        }
        JSONObject jsonData = new JSONObject();
        jsonData.put("users", usersArray);

        try (FileWriter file = new FileWriter(filePath)) {
            file.write(jsonData.toJSONString());
        }
    }

    public static UserDatabase createSampleUsers() {
        UserDatabase userDatabase = new UserDatabase();
        userDatabase.addUser(new User("admin", "admin", "40 hours", 5));
        return userDatabase;
    }
}
