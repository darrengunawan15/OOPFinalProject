package stars.main;

public class User {
    private String username;
    private String password;
    private String projectTime;
    private long finishedProjects;

    public User(String username, String password, String projectTime, long finishedProjects) {
        this.username = username;
        this.password = password;
        this.projectTime = projectTime;
        this.finishedProjects = (long) finishedProjects;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getProjectTime() {
        return projectTime;
    }

    public long getFinishedProjects() {
        return finishedProjects;
    }

    public boolean validate(String username, String password) {
        return this.username.equals(username) && this.password.equals(password);
    }
}