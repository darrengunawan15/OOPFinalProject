package stars.main;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.fonts.jetbrains_mono.FlatJetBrainsMonoFont;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import java.awt.Font;
import java.io.IOException;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import org.json.simple.parser.ParseException;

public class Main {   
    public static void main (String[] args) throws UnsupportedLookAndFeelException, IOException, ParseException {
        FlatJetBrainsMonoFont.install();
        FlatLaf.registerCustomDefaultsSource("stars.themes");
        UIManager.put("defaultFont", new Font(FlatJetBrainsMonoFont.FAMILY,Font.PLAIN, 13));
        UIManager.setLookAndFeel(new FlatOneDarkIJTheme());
               
        // Load user data from JSON or create sample users
        final UserDatabase userDatabase;
        userDatabase = UserDatabase.loadFromJSON("src/stars/data/users.json");

        // Create and display the login form
        SwingUtilities.invokeLater(() -> {
            LoginForm loginForm = new LoginForm(userDatabase);
            loginForm.setVisible(true);
        });
        
//        new DashboardForm().setVisible(true);
    }
}
