package stars.main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import raven.drawer.Drawer;
import raven.popup.GlassPanePopup;
import stars.drawer.MyDrawerBuilder;

public class DashboardForm extends javax.swing.JFrame implements DateFormattable{
    
    private DefaultTableModel model;
    private List<JSONObject> nearestDeadlineProjects;
    private int currentProjectIndex;
    
    public DashboardForm() {
        GlassPanePopup.install(this);
        MyDrawerBuilder myDrawerBuilder = new MyDrawerBuilder(this, 0);
        Drawer.getInstance().setDrawerBuilder(myDrawerBuilder);
        initComponents();
        model = (DefaultTableModel) DailyTables.getModel();
        loadData();
        tableUpdate();
        handleRowClick();
    }
    
    private void loadData() {
        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader("src/stars/data/database.json")) {
            // Load project data from JSON file
            JSONObject jsonData = (JSONObject) jsonParser.parse(reader);
            JSONArray projects = (JSONArray) jsonData.get("projects");
            List<Date> deadlines = new ArrayList<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            // Sort projects by lastEdited for displaying in Panel 1 and Panel 2
            Collections.sort(projects, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    try {
                        Date d1 = sdf.parse((String) o1.get("lastEdited"));
                        Date d2 = sdf.parse((String) o2.get("lastEdited"));
                        return d2.compareTo(d1);  
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });

            // Display recent projects in Panel 1 and Panel 2
            if (projects.size() >= 1) {
                JSONObject project1 = (JSONObject) projects.get(0);
                project_1_text.setText((String) project1.get("projectName"));
                leader_1_container.setText("Leader: " + (String) project1.get("projectLeader"));
                deadline_1_text.setText("Deadline: " + formatDate((String) project1.get("deadline")));
                int progress1 = ((Long) project1.get("progress")).intValue();
                progress_1_text.setText("Progress: " + progress1 + "%");
                progress_1_bar.setValue(progress1);
                jPanel1.setVisible(true);
            } else {
                jPanel1.setVisible(false);
            }

            if (projects.size() >= 2) {
                JSONObject project2 = (JSONObject) projects.get(1);
                project_2_text.setText((String) project2.get("projectName"));
                leader_2_container.setText("Leader: " + (String) project2.get("projectLeader"));
                deadline_2_text.setText("Deadline: " + formatDate((String) project2.get("deadline")));
                int progress2 = ((Long) project2.get("progress")).intValue();
                progress_2_text.setText("Progress: " + progress2 + "%");
                progress_2_bar.setValue(progress2);
                jPanel2.setVisible(true);
            } else {
                jPanel2.setVisible(false);
            }

            // Sort projects by deadline for the nearest deadline section
            Collections.sort(projects, new Comparator<JSONObject>() {
                @Override
                public int compare(JSONObject o1, JSONObject o2) {
                    try {
                        Date d1 = sdf.parse((String) o1.get("deadline"));
                        Date d2 = sdf.parse((String) o2.get("deadline"));
                        return d1.compareTo(d2);
                    } catch (Exception e) {
                        return 0;
                    }
                }
            });

            // Calculate and display deadline info
            Date nearestDeadline = null;
            nearestDeadlineProjects = new ArrayList<>();
            for (Object projectObj : projects) {
                JSONObject project = (JSONObject) projectObj;
                Date projectDeadline = sdf.parse((String) project.get("deadline"));
                if (nearestDeadline == null || projectDeadline.equals(nearestDeadline)) {
                    nearestDeadline = projectDeadline;
                    nearestDeadlineProjects.add(project);
                } else if (projectDeadline.before(nearestDeadline)) {
                    nearestDeadline = projectDeadline;
                    nearestDeadlineProjects.clear();
                    nearestDeadlineProjects.add(project);
                } else {
                    break;
                }
            }

            if (!nearestDeadlineProjects.isEmpty()) {
                LocalDate currentDate = LocalDate.now();
                LocalDate deadlineDate = new java.sql.Date(nearestDeadline.getTime()).toLocalDate();
                long daysBetween = (long) ChronoUnit.DAYS.between(currentDate, deadlineDate);

                if (daysBetween == 1) {
                    deadlines_days_container.setText("Tomorrow");
                } else {
                    deadlines_days_container.setText("in " + daysBetween + " days");
                }

                int sameDayCount = nearestDeadlineProjects.size();
                index_deadlines_container.setText("1/" + sameDayCount);

                currentProjectIndex = 0;
                displayCurrentProject();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try (FileReader reader = new FileReader("src/stars/data/users.json")) {
            JSONObject jsonData = (JSONObject) jsonParser.parse(reader);

            JSONArray userDataArray = (JSONArray) jsonData.get("users");
            if (!userDataArray.isEmpty()) {
                JSONObject userData = (JSONObject) userDataArray.get(0);
                ptime_container.setText((String) userData.get("projectTime"));
                tiprog_container.setText(String.valueOf((long) userData.get("finishedProjects")));
                
            } else {
                ptime_container.setText("N/A");
                tiprog_container.setText("0");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        try (FileReader reader = new FileReader("src/stars/data/dailytasks.json")) {
            JSONObject jsonData = (JSONObject) jsonParser.parse(reader);

            // Load team members data
            JSONArray userDataArray = (JSONArray) jsonData.get("users");
            if (!userDataArray.isEmpty()) {
                JSONObject userData = (JSONObject) userDataArray.get(0);
                JSONArray dailyTasks = (JSONArray) userData.get("dailyTasks");
                model.setRowCount(0);

                if (dailyTasks != null && !dailyTasks.isEmpty()) {
                    for (Object taskObj : dailyTasks) {
                        if (taskObj instanceof String) {
                            String taskName = (String) taskObj;
                            model.addRow(new Object[]{false, taskName});
                        }
                    }
                }
                daily_task_container.setText("Daily Tasks (" + model.getRowCount() + ")");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void displayCurrentProject() {
        if (!nearestDeadlineProjects.isEmpty()) {
            JSONObject project = nearestDeadlineProjects.get(currentProjectIndex);
            deadlines_details_containers.setText(
                (String) project.get("projectName") + " : " + (String) project.get("projectDetails")
            );
        }
    }
    
    public String formatDate(String dateStr) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd");
            Date date = inputFormat.parse(dateStr);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd MMMM yyyy");
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Invalid Date";
        }
    }
    
    
    public void tableUpdate() {
        try {
            // Read daily tasks from the JSON file
            JSONArray dailyTasksArray = readUsersFromJSON();

            // Clear the existing table data
            model.setRowCount(0);

            // Populate the table with the daily tasks
            for (Object taskObj : dailyTasksArray) {
                JSONObject task = (JSONObject) taskObj;
                String taskName = (String) task.get("taskName");
                boolean selected = (boolean) task.get("selected");
                model.addRow(new Object[]{selected, taskName});
            }
            daily_task_container.setText("Daily Tasks (" + model.getRowCount() + ")");
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private void saveTasksToJSON() {
        try {
            JSONArray dailyTasksArray = new JSONArray();

            for (int i = 0; i < model.getRowCount(); i++) {
                boolean selected = (boolean) model.getValueAt(i, 0);
                String taskName = (String) model.getValueAt(i, 1);

                JSONObject task = new JSONObject();
                task.put("taskName", taskName);
                task.put("selected", selected);

                dailyTasksArray.add(task);
            }

            JSONObject userData = new JSONObject();
            userData.put("dailyTasks", dailyTasksArray);
            JSONArray usersArray = new JSONArray();
            usersArray.add(userData);
            JSONObject jsonData = new JSONObject();
            jsonData.put("users", usersArray);

            writeUsersToJSON(jsonData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteSelectedTasks() {
        for (int i = model.getRowCount() - 1; i >= 0; i--) {
            boolean selected = (boolean) model.getValueAt(i, 0);
            if (selected) {
                model.removeRow(i);
            }
        }
        saveTasksToJSON();  // Save the updated tasks to JSON
    }

    public void handleRowClick() {
        DailyTables.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    int selectedRow = DailyTables.getSelectedRow();
                    if (selectedRow != -1 && selectedRow < DailyTables.getRowCount()) {
                        boolean currentValue = (boolean) model.getValueAt(selectedRow, 0);
                        model.setValueAt(!currentValue, selectedRow, 0);
                        saveTasksToJSON();  // Save the updated tasks to JSON
                    }
                }
            }
        });
    }

    private JSONArray readUsersFromJSON() throws ParseException {
        try (FileReader reader = new FileReader("src/stars/data/dailytasks.json")) {
            JSONParser jsonParser = new JSONParser();
            JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

            JSONArray userDataArray = (JSONArray) jsonObject.get("users");
            if (!userDataArray.isEmpty()) {
                JSONObject userData = (JSONObject) userDataArray.get(0);
                return (JSONArray) userData.get("dailyTasks");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new JSONArray();
    }

    private void writeUsersToJSON(JSONObject userData) {
        try (FileWriter file = new FileWriter("src/stars/data/dailytasks.json")) {
            file.write(userData.toJSONString());
            file.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel5 = new javax.swing.JPanel();
        header = new javax.swing.JPanel();
        menuButton = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        Content = new javax.swing.JPanel();
        jPanel4 = new RoundedPanel(50, new Color(40, 40, 40));
        jLabel2 = new javax.swing.JLabel();
        jPanel3 = new RoundedPanel(50, new Color(63, 63, 63));
        project_1_text = new javax.swing.JLabel();
        deadline_1_text = new javax.swing.JLabel();
        progress_1_bar = new javax.swing.JProgressBar();
        progress_1_text = new javax.swing.JLabel();
        leader_1_container = new javax.swing.JLabel();
        imageAvatar1 = new stars.avatar.ImageAvatar();
        jPanel8 = new RoundedPanel(50, new Color(63, 63, 63));
        project_2_text = new javax.swing.JLabel();
        deadline_2_text = new javax.swing.JLabel();
        leader_2_container = new javax.swing.JLabel();
        progress_2_text = new javax.swing.JLabel();
        progress_2_bar = new javax.swing.JProgressBar();
        imageAvatar2 = new stars.avatar.ImageAvatar();
        jLabel3 = new javax.swing.JLabel();
        jPanel1 = new RoundedPanel(50, new Color(40, 40, 40));
        jLabel5 = new javax.swing.JLabel();
        jPanel9 = new RoundedPanel(20, new Color(63, 63, 63));
        jLabel4 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        ptime_container = new javax.swing.JLabel();
        tiprog_container = new javax.swing.JLabel();
        imageAvatar3 = new stars.avatar.ImageAvatar();
        imageAvatar4 = new stars.avatar.ImageAvatar();
        imageAvatar5 = new stars.avatar.ImageAvatar();
        jPanel2 = new RoundedPanel(50, new Color(40, 40, 40));
        daily_task_container = new javax.swing.JLabel();
        add_task = new javax.swing.JButton();
        delete_task = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        DailyTables = new javax.swing.JTable();
        jPanel6 =  new RoundedPanel(50, new Color(40, 40, 40));
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jPanel7 = new RoundedPanel(20, new Color(63, 63, 63));
        deadlines_days_container = new javax.swing.JLabel();
        index_deadlines_container = new javax.swing.JLabel();
        deadlines_details_containers = new javax.swing.JLabel();
        Restricted = new javax.swing.JPanel();

        jPanel5.setBackground(new java.awt.Color(153, 255, 102));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 100, Short.MAX_VALUE)
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1024, 756));
        setResizable(false);

        header.setPreferredSize(new java.awt.Dimension(1024, 80));

        menuButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/drawer/icon/bars-light.png"))); // NOI18N
        menuButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuButtonActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("SansSerif", 1, 18)); // NOI18N
        jLabel1.setText("Welcome, Admin");

        javax.swing.GroupLayout headerLayout = new javax.swing.GroupLayout(header);
        header.setLayout(headerLayout);
        headerLayout.setHorizontalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(menuButton, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 368, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        headerLayout.setVerticalGroup(
            headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(headerLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(headerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 66, Short.MAX_VALUE)
                    .addComponent(menuButton, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8))
        );

        Content.setPreferredSize(new java.awt.Dimension(100, 653));

        jLabel2.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        jLabel2.setText("Recent Projects");

        jPanel3.setBackground(new java.awt.Color(40, 40, 40));
        jPanel3.setPreferredSize(new java.awt.Dimension(300, 284));

        project_1_text.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        project_1_text.setText("project_1_text");

        deadline_1_text.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        deadline_1_text.setText("Deadline: deadline_container");

        progress_1_bar.setValue(67);
        progress_1_bar.setPreferredSize(new java.awt.Dimension(157, 6));

        progress_1_text.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        progress_1_text.setText("Progress: progress_container");

        leader_1_container.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        leader_1_container.setText("Leader: leader_container");

        imageAvatar1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/image/profilepicture.jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(project_1_text, javax.swing.GroupLayout.PREFERRED_SIZE, 216, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progress_1_text, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deadline_1_text, javax.swing.GroupLayout.PREFERRED_SIZE, 219, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(leader_1_container, javax.swing.GroupLayout.PREFERRED_SIZE, 234, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(imageAvatar1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progress_1_bar, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(19, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(project_1_text, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(leader_1_container)
                .addGap(18, 18, 18)
                .addComponent(imageAvatar1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(deadline_1_text)
                .addGap(18, 18, 18)
                .addComponent(progress_1_text)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress_1_bar, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(32, Short.MAX_VALUE))
        );

        jPanel8.setBackground(new java.awt.Color(40, 40, 40));
        jPanel8.setPreferredSize(new java.awt.Dimension(300, 272));

        project_2_text.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        project_2_text.setText("project_2_text");

        deadline_2_text.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        deadline_2_text.setText("Deadline: deadline_container");

        leader_2_container.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        leader_2_container.setText("Leader: leader_container");

        progress_2_text.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        progress_2_text.setText("Progress: progress_container");

        progress_2_bar.setValue(67);
        progress_2_bar.setPreferredSize(new java.awt.Dimension(157, 6));

        imageAvatar2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/image/profilepicture.jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageAvatar2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progress_2_text, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(progress_2_bar, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(deadline_2_text, javax.swing.GroupLayout.PREFERRED_SIZE, 236, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(leader_2_container, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(project_2_text, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(30, Short.MAX_VALUE))
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addComponent(project_2_text, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(leader_2_container)
                .addGap(18, 18, 18)
                .addComponent(imageAvatar2, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(deadline_2_text)
                .addGap(18, 18, 18)
                .addComponent(progress_2_text)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progress_2_bar, javax.swing.GroupLayout.PREFERRED_SIZE, 18, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel3.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(141, 141, 243));
        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel3.setText("View All");
        jLabel3.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel3MouseClicked(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel8, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 246, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(28, 28, 28))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, 284, Short.MAX_VALUE))
                .addContainerGap(30, Short.MAX_VALUE))
        );

        jLabel5.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel5.setText("Team Members");

        jPanel9.setBackground(new java.awt.Color(40, 40, 40));

        jLabel4.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel4.setText("Project time");

        jLabel8.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel8.setText("Finished Projects");

        ptime_container.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        ptime_container.setText("ptime_container");

        tiprog_container.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        tiprog_container.setText("finished_container");

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ptime_container, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 106, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tiprog_container, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 10, Short.MAX_VALUE)
                .addGroup(jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ptime_container)
                    .addComponent(tiprog_container))
                .addContainerGap())
        );

        imageAvatar3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/image/profilepicture.jpg"))); // NOI18N

        imageAvatar4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/image/profilepicture.jpg"))); // NOI18N

        imageAvatar5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/stars/image/profilepicture.jpg"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(imageAvatar3, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageAvatar4, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(imageAvatar5, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 149, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel5)
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(imageAvatar3, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(imageAvatar4, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(imageAvatar5, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanel9, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        daily_task_container.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        daily_task_container.setText("Daily Tasks (number)");

        add_task.setText("+");
        add_task.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                add_taskActionPerformed(evt);
            }
        });

        delete_task.setText("-");
        delete_task.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                delete_taskActionPerformed(evt);
            }
        });

        DailyTables.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "", "Notes"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Boolean.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane2.setViewportView(DailyTables);
        if (DailyTables.getColumnModel().getColumnCount() > 0) {
            DailyTables.getColumnModel().getColumn(0).setMinWidth(40);
            DailyTables.getColumnModel().getColumn(0).setPreferredWidth(20);
            DailyTables.getColumnModel().getColumn(0).setMaxWidth(40);
            DailyTables.getColumnModel().getColumn(1).setResizable(false);
        }

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(daily_task_container, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(delete_task, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(add_task, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(18, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(daily_task_container)
                    .addComponent(add_task, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(delete_task, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 274, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(36, Short.MAX_VALUE))
        );

        jLabel7.setFont(new java.awt.Font("SansSerif", 1, 12)); // NOI18N
        jLabel7.setText("Reminders");

        jLabel9.setFont(new java.awt.Font("SansSerif", 0, 12)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(141, 141, 243));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        jLabel9.setText("View All");
        jLabel9.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        jLabel9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabel9MouseClicked(evt);
            }
        });

        jPanel7.setBackground(new java.awt.Color(40, 40, 40));
        jPanel7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jPanel7MouseClicked(evt);
            }
        });

        deadlines_days_container.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        deadlines_days_container.setText("in x days");

        index_deadlines_container.setFont(new java.awt.Font("SansSerif", 1, 14)); // NOI18N
        index_deadlines_container.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
        index_deadlines_container.setText("x / x");

        deadlines_details_containers.setFont(new java.awt.Font("SansSerif", 0, 18)); // NOI18N
        deadlines_details_containers.setText("project_name: project_details");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel7Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(deadlines_details_containers, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel7Layout.createSequentialGroup()
                        .addComponent(deadlines_days_container, javax.swing.GroupLayout.PREFERRED_SIZE, 98, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(index_deadlines_container, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(24, 24, 24))
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(deadlines_days_container)
                    .addComponent(index_deadlines_container))
                .addGap(18, 18, 18)
                .addComponent(deadlines_details_containers)
                .addContainerGap(46, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jPanel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(25, 25, 25))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(24, 24, 24)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel7, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(21, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout ContentLayout = new javax.swing.GroupLayout(Content);
        Content.setLayout(ContentLayout);
        ContentLayout.setHorizontalGroup(
            ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContentLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18)
                .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16))
        );
        ContentLayout.setVerticalGroup(
            ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ContentLayout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(ContentLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(ContentLayout.createSequentialGroup()
                        .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(ContentLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(14, Short.MAX_VALUE))
        );

        Restricted.setBackground(new java.awt.Color(51, 51, 51));

        javax.swing.GroupLayout RestrictedLayout = new javax.swing.GroupLayout(Restricted);
        Restricted.setLayout(RestrictedLayout);
        RestrictedLayout.setHorizontalGroup(
            RestrictedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1024, Short.MAX_VALUE)
        );
        RestrictedLayout.setVerticalGroup(
            RestrictedLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 20, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(Restricted, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                .addComponent(header, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 997, Short.MAX_VALUE)
                .addComponent(Content, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 997, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(header, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(Content, javax.swing.GroupLayout.PREFERRED_SIZE, 645, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(Restricted, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void menuButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuButtonActionPerformed
        // TODO add your handling code here:
        Drawer.getInstance().showDrawer();
    }//GEN-LAST:event_menuButtonActionPerformed

    private void add_taskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_add_taskActionPerformed
        // TODO add your handling code here:
        
        // Create and configure the popup form
        JPanel panel = new JPanel(new GridLayout(1, 1));
        panel.add(new JLabel("Notes:"));
        JTextField notesField = new JTextField();
        panel.add(notesField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Add Note", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String notes = notesField.getText().trim();
            if (!notes.isEmpty()) {
                try {
                    // Read existing users data from the JSON file
                    JSONArray dailyTasksArray = readUsersFromJSON();

                    // Add the new note
                    JSONObject newNote = new JSONObject();
                    newNote.put("taskName", notes);
                    newNote.put("selected", false);
                    dailyTasksArray.add(newNote);

                    // Write the updated data back to the JSON file
                    JSONObject userData = new JSONObject();
                    userData.put("dailyTasks", dailyTasksArray);
                    JSONArray usersArray = new JSONArray();
                    usersArray.add(userData);
                    JSONObject jsonData = new JSONObject();
                    jsonData.put("users", usersArray);
                    writeUsersToJSON(jsonData);

                    // Update the table
                    tableUpdate();

                    // Inform the user that the note has been added
                    JOptionPane.showMessageDialog(this, "Note added successfully.");
                } catch (org.json.simple.parser.ParseException e) {
                    e.printStackTrace();
                }
            } else {
                JOptionPane.showMessageDialog(this, "Notes cannot be empty.", "Validation Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }//GEN-LAST:event_add_taskActionPerformed

    private void delete_taskActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_delete_taskActionPerformed
        // TODO add your handling code here:
        
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete selected tasks?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            deleteSelectedTasks();
        }

        daily_task_container.setText("Daily Tasks ("+ model.getRowCount() +")");
    }//GEN-LAST:event_delete_taskActionPerformed

    private void jLabel3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel3MouseClicked
        // TODO add your handling code here:
        
        this.dispose();
        new ProjectForm().setVisible(true);
    }//GEN-LAST:event_jLabel3MouseClicked

    private void jLabel9MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabel9MouseClicked
        try {
            // TODO add your handling code here:

            showDeadlinesPopup();
        } catch (IOException ex) {
            Logger.getLogger(DashboardForm.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ParseException ex) {
            Logger.getLogger(DashboardForm.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jLabel9MouseClicked

    private void jPanel7MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jPanel7MouseClicked
        // TODO add your handling code here:
        
        if (nearestDeadlineProjects != null && nearestDeadlineProjects.size() > 1) {
            currentProjectIndex = (currentProjectIndex + 1) % nearestDeadlineProjects.size();
            index_deadlines_container.setText((currentProjectIndex + 1) + "/" + nearestDeadlineProjects.size());
            displayCurrentProject();
        }
    }//GEN-LAST:event_jPanel7MouseClicked

    private void showDeadlinesPopup() throws IOException, ParseException {
        // Create the popup dialog
        FileReader reader = new FileReader("src/stars/data/database.json");
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = (JSONObject) jsonParser.parse(reader);

        JSONArray projectsArray = (JSONArray) jsonObject.get("projects");

        JPanel panel = new JPanel(new GridLayout(projectsArray.size(), 1));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();

        for (Object obj : projectsArray) {
            JSONObject project = (JSONObject) obj;
            String projectName = (String) project.get("projectName");
            String deadlineStr = (String) project.get("deadline");
            LocalDate deadline = LocalDate.parse(deadlineStr, formatter);

            int daysUntilDeadline = (int) ChronoUnit.DAYS.between(today, deadline);
            String message = projectName + ": \t in " + daysUntilDeadline + " days";

            panel.add(new JLabel(message));
        }

        JOptionPane.showMessageDialog(null, panel, "Project Deadlines", JOptionPane.INFORMATION_MESSAGE);
    }
    
    public static void main(String args[]) {
        
        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new DashboardForm().setVisible(true);
            }
        });
    }
    
    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel Content;
    private javax.swing.JTable DailyTables;
    private javax.swing.JPanel Restricted;
    private javax.swing.JButton add_task;
    private javax.swing.JLabel daily_task_container;
    private javax.swing.JLabel deadline_1_text;
    private javax.swing.JLabel deadline_2_text;
    private javax.swing.JLabel deadlines_days_container;
    private javax.swing.JLabel deadlines_details_containers;
    private javax.swing.JButton delete_task;
    private javax.swing.JPanel header;
    private stars.avatar.ImageAvatar imageAvatar1;
    private stars.avatar.ImageAvatar imageAvatar2;
    private stars.avatar.ImageAvatar imageAvatar3;
    private stars.avatar.ImageAvatar imageAvatar4;
    private stars.avatar.ImageAvatar imageAvatar5;
    private javax.swing.JLabel index_deadlines_container;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel leader_1_container;
    private javax.swing.JLabel leader_2_container;
    private javax.swing.JButton menuButton;
    private javax.swing.JProgressBar progress_1_bar;
    private javax.swing.JLabel progress_1_text;
    private javax.swing.JProgressBar progress_2_bar;
    private javax.swing.JLabel progress_2_text;
    private javax.swing.JLabel project_1_text;
    private javax.swing.JLabel project_2_text;
    private javax.swing.JLabel ptime_container;
    private javax.swing.JLabel tiprog_container;
    // End of variables declaration//GEN-END:variables
}

class RoundedPanel extends JPanel
    {
        private Color backgroundColor;
        private int cornerRadius = 15;

        public RoundedPanel(LayoutManager layout, int radius) {
            super(layout);
            cornerRadius = radius;
        }

        public RoundedPanel(LayoutManager layout, int radius, Color bgColor) {
            super(layout);
            cornerRadius = radius;
            backgroundColor = bgColor;
        }

        public RoundedPanel(int radius) {
            super();
            cornerRadius = radius;
        }

        public RoundedPanel(int radius, Color bgColor) {
            super();
            cornerRadius = radius;
            backgroundColor = bgColor;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Dimension arcs = new Dimension(cornerRadius, cornerRadius);
            int width = getWidth();
            int height = getHeight();
            Graphics2D graphics = (Graphics2D) g;
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (backgroundColor != null) {
                graphics.setColor(backgroundColor);
            } else {
                graphics.setColor(getBackground());
            }
            graphics.fillRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height); //paint background
            graphics.setColor(getForeground());
            graphics.drawRoundRect(0, 0, width-1, height-1, arcs.width, arcs.height); //paint border
        }
    }