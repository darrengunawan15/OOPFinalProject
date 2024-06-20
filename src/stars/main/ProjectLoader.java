package stars.main;

import java.awt.Color;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JProgressBar;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import stars.avatar.ImageAvatar;

public class ProjectLoader implements DateFormattable, isLoadable{

    private JPanel[] projectPanels;
    private JLabel[] projectTexts;
    private JLabel[] leaderContainers;
    private ImageAvatar[] imageAvatars;
    private JLabel[] deadlineTexts;
    private JLabel[] progressTexts;
    private JProgressBar[] progressBars;

    public ProjectLoader(JPanel[] projectPanels, JLabel[] projectTexts, JLabel[] leaderContainers, ImageAvatar[] imageAvatars,
                         JLabel[] deadlineTexts, JLabel[] progressTexts, JProgressBar[] progressBars) {
        this.projectPanels = projectPanels;
        this.projectTexts = projectTexts;
        this.leaderContainers = leaderContainers;
        this.imageAvatars = imageAvatars;
        this.deadlineTexts = deadlineTexts;
        this.progressTexts = progressTexts;
        this.progressBars = progressBars;
    }

    public void loadProjects() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/stars/data/database.json")) {
            JSONObject jsonData = (JSONObject) jsonParser.parse(reader);

            // Load project data
            JSONArray projects = (JSONArray) jsonData.get("projects");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // Sort projects by lastEdited date in descending order
            Collections.sort(projects, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    try {
                        Date d1 = sdf.parse((String) o1.get("lastEdited"));
                        Date d2 = sdf.parse((String) o2.get("lastEdited"));
                        return d2.compareTo(d1); // Descending order
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });

            // Display up to 5 projects
            for (int i = 0; i < projectPanels.length; i++) {
                if (i < projects.size()) {
                    JSONObject project = (JSONObject) projects.get(i);
                    projectTexts[i].setText((String) project.get("projectName"));
                    leaderContainers[i].setText("Leader: " + (String) project.get("projectLeader"));
                    deadlineTexts[i].setText("Deadline: " + formatDate((String) project.get("deadline")));
                    int progress = ((Long) project.get("progress")).intValue();
                    progressTexts[i].setText("Progress: " + progress + "%");
                    progressBars[i].setValue(progress);
                    projectPanels[i] = new RoundedPanel(50, new Color(63, 63, 63));
                    projectPanels[i].setVisible(true);
                } else {
                    projectPanels[i].setForeground(new Color(40, 40, 40));
                    projectPanels[i].setOpaque(true);
                    projectTexts[i].setText("");
                    leaderContainers[i].setText("");
                    imageAvatars[i].setVisible(false);
                    deadlineTexts[i].setText("");
                    progressTexts[i].setText("");
                    progressBars[i].setVisible(false);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String formatDate(String dateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM, yyyy");
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return dateStr;
        }
    }
}
