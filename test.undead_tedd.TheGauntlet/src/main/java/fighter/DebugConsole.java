package fighter;

import javax.swing.*;
import java.awt.*;

public class DebugConsole extends JFrame {

    private JTextArea logArea;

    public DebugConsole() {
        setTitle("Debug Console");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Center the console window on screen

        // Create a non-editable text area for displaying logs
        logArea = new JTextArea();
        logArea.setEditable(false);

        // Add the log area inside a scroll pane (so it can scroll)
        JScrollPane scrollPane = new JScrollPane(logArea);
        add(scrollPane, BorderLayout.CENTER);

        setVisible(true); // Show the console window
    }

    // Method to log a message in the console
    public void log(String message) {
        logArea.append(message + "\n");  // Add the message to the text area
        logArea.setCaretPosition(logArea.getDocument().getLength()); // Scroll to the latest message
    }
}
