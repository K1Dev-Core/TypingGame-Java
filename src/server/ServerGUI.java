package server;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import shared.*;

public class ServerGUI extends JFrame {
    private TypingGameServer server;
    private Thread serverThread;
    private boolean serverRunning = false;
    
    private JButton startServerBtn;
    private JButton stopServerBtn;
    private JLabel serverStatusLabel;
    private JTextArea logArea;
    private JTable playersTable;
    private DefaultTableModel playersTableModel;
    private JTable wordsTable;
    private DefaultTableModel wordsTableModel;
    private JTextField wordField;
    private JTextField pronunciationField;
    private JTextField meaningField;
    private JButton addWordBtn;
    private JButton editWordBtn;
    private JButton deleteWordBtn;
    private ScheduledExecutorService updateScheduler;
    
    public ServerGUI() {
        setTitle("TypingGame Server Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        loadWordsFromFile();
        
        updateScheduler = Executors.newScheduledThreadPool(1);
        updateScheduler.scheduleAtFixedRate(this::updatePlayersList, 0, 2, TimeUnit.SECONDS);
        
        logMessage("Server GUI initialized");
    }
    
    private void initializeComponents() {
        startServerBtn = new JButton("Start Server");
        stopServerBtn = new JButton("Stop Server");
        stopServerBtn.setEnabled(false);
        
        serverStatusLabel = new JLabel("Server Status: STOPPED");
        serverStatusLabel.setForeground(Color.RED);
        serverStatusLabel.setFont(new Font("Arial", Font.BOLD, 14));
        
        logArea = new JTextArea(10, 50);
        logArea.setEditable(false);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setBackground(Color.BLACK);
        logArea.setForeground(Color.GREEN);
        
        String[] playerColumns = {"Player ID", "Name", "Character", "Room", "Status"};
        playersTableModel = new DefaultTableModel(playerColumns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        playersTable = new JTable(playersTableModel);
        playersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        String[] wordColumns = {"Word", "Pronunciation", "Meaning"};
        wordsTableModel = new DefaultTableModel(wordColumns, 0);
        wordsTable = new JTable(wordsTableModel);
        wordsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        wordField = new JTextField(15);
        pronunciationField = new JTextField(15);
        meaningField = new JTextField(15);
        
        addWordBtn = new JButton("Add Word");
        editWordBtn = new JButton("Edit Word");
        deleteWordBtn = new JButton("Delete Word");
        
        editWordBtn.setEnabled(false);
        deleteWordBtn.setEnabled(false);
    }
    
    private void setupLayout() {
        setLayout(new BorderLayout());
        
        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(startServerBtn);
        topPanel.add(stopServerBtn);
        topPanel.add(Box.createHorizontalStrut(20));
        topPanel.add(serverStatusLabel);
        
        JTabbedPane tabbedPane = new JTabbedPane();
        
        JPanel serverPanel = createServerPanel();
        JPanel playersPanel = createPlayersPanel();
        JPanel wordsPanel = createWordsPanel();
        
        tabbedPane.addTab("Server Control", serverPanel);
        tabbedPane.addTab("Online Players", playersPanel);
        tabbedPane.addTab("Word Management", wordsPanel);
        
        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }
    
    private JPanel createServerPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JPanel logPanel = new JPanel(new BorderLayout());
        logPanel.setBorder(new TitledBorder("Server Log"));
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        
        JPanel controlPanel = new JPanel(new FlowLayout());
        JButton clearLogBtn = new JButton("Clear Log");
        clearLogBtn.addActionListener(e -> logArea.setText(""));
        controlPanel.add(clearLogBtn);
        
        panel.add(logPanel, BorderLayout.CENTER);
        panel.add(controlPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createPlayersPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Connected Players"));
        
        JScrollPane scrollPane = new JScrollPane(playersTable);
        panel.add(scrollPane, BorderLayout.CENTER);
        
        JPanel infoPanel = new JPanel(new FlowLayout());
        JLabel infoLabel = new JLabel("Auto-refreshes every 2 seconds");
        infoLabel.setFont(new Font("Arial", Font.ITALIC, 12));
        infoPanel.add(infoLabel);
        
        panel.add(infoPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private JPanel createWordsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        
        JScrollPane scrollPane = new JScrollPane(wordsTable);
        scrollPane.setBorder(new TitledBorder("Word List"));
        
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(new TitledBorder("Word Input"));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        
        gbc.gridx = 0; gbc.gridy = 0;
        inputPanel.add(new JLabel("Word:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(wordField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        inputPanel.add(new JLabel("Pronunciation:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(pronunciationField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        inputPanel.add(new JLabel("Meaning:"), gbc);
        gbc.gridx = 1;
        inputPanel.add(meaningField, gbc);
        
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addWordBtn);
        buttonPanel.add(editWordBtn);
        buttonPanel.add(deleteWordBtn);
        
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        inputPanel.add(buttonPanel, gbc);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }
    
    private void setupEventHandlers() {
        startServerBtn.addActionListener(this::startServer);
        stopServerBtn.addActionListener(this::stopServer);
        
        wordsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int selectedRow = wordsTable.getSelectedRow();
                if (selectedRow >= 0) {
                    wordField.setText((String) wordsTableModel.getValueAt(selectedRow, 0));
                    pronunciationField.setText((String) wordsTableModel.getValueAt(selectedRow, 1));
                    meaningField.setText((String) wordsTableModel.getValueAt(selectedRow, 2));
                    editWordBtn.setEnabled(true);
                    deleteWordBtn.setEnabled(true);
                } else {
                    clearWordFields();
                    editWordBtn.setEnabled(false);
                    deleteWordBtn.setEnabled(false);
                }
            }
        });
        
        addWordBtn.addActionListener(this::addWord);
        editWordBtn.addActionListener(this::editWord);
        deleteWordBtn.addActionListener(this::deleteWord);
    }
    
    private void startServer(ActionEvent e) {
        if (!serverRunning) {
            server = new TypingGameServer();
            serverThread = new Thread(() -> {
                try {
                    server.start();
                } catch (IOException ex) {
                    SwingUtilities.invokeLater(() -> {
                        logMessage("Failed to start server: " + ex.getMessage());
                        updateServerStatus(false);
                    });
                }
            });
            
            serverThread.start();
            serverRunning = true;
            updateServerStatus(true);
            logMessage("Server started on port 8888");
        }
    }
    
    private void stopServer(ActionEvent e) {
        if (serverRunning && server != null) {
            try {
                server.stop();
                serverRunning = false;
                updateServerStatus(false);
                logMessage("Server stopped");
            } catch (IOException ex) {
                logMessage("Error stopping server: " + ex.getMessage());
            }
        }
    }
    
    private void updateServerStatus(boolean running) {
        serverRunning = running;
        startServerBtn.setEnabled(!running);
        stopServerBtn.setEnabled(running);
        
        if (running) {
            serverStatusLabel.setText("Server Status: RUNNING");
            serverStatusLabel.setForeground(Color.GREEN);
        } else {
            serverStatusLabel.setText("Server Status: STOPPED");
            serverStatusLabel.setForeground(Color.RED);
        }
    }
    
    private void updatePlayersList() {
        if (server != null && serverRunning) {
            SwingUtilities.invokeLater(() -> {
                playersTableModel.setRowCount(0);
                
                List<GameRoom> rooms = server.getRoomList();
                for (GameRoom room : rooms) {
                    for (Player player : room.players) {
                        Vector<String> row = new Vector<>();
                        row.add(player.id.substring(0, Math.min(8, player.id.length())));
                        row.add(player.name);
                        row.add(player.selectedCharacterId);
                        row.add(room.name);
                        row.add(room.roomState.toString());
                        playersTableModel.addRow(row);
                    }
                }
            });
        }
    }
    
    private void loadWordsFromFile() {
        try {
            List<String> lines = Files.readAllLines(Paths.get("res/words.txt"));
            for (String line : lines) {
                String[] parts = line.split("\\|");
                if (parts.length == 3) {
                    Vector<String> row = new Vector<>();
                    row.add(parts[0]);
                    row.add(parts[1]);
                    row.add(parts[2]);
                    wordsTableModel.addRow(row);
                }
            }
            logMessage("Loaded " + wordsTableModel.getRowCount() + " words from file");
        } catch (IOException e) {
            logMessage("Error loading words: " + e.getMessage());
        }
    }
    
    private void saveWordsToFile() {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter("res/words.txt"));
            for (int i = 0; i < wordsTableModel.getRowCount(); i++) {
                String word = (String) wordsTableModel.getValueAt(i, 0);
                String pronunciation = (String) wordsTableModel.getValueAt(i, 1);
                String meaning = (String) wordsTableModel.getValueAt(i, 2);
                writer.println(word + "|" + pronunciation + "|" + meaning);
            }
            writer.close();
            logMessage("Words saved to file");
        } catch (IOException e) {
            logMessage("Error saving words: " + e.getMessage());
        }
    }
    
    private void addWord(ActionEvent e) {
        String word = wordField.getText().trim().toUpperCase();
        String pronunciation = pronunciationField.getText().trim();
        String meaning = meaningField.getText().trim();
        
        if (word.isEmpty() || pronunciation.isEmpty() || meaning.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        for (int i = 0; i < wordsTableModel.getRowCount(); i++) {
            if (word.equals(wordsTableModel.getValueAt(i, 0))) {
                JOptionPane.showMessageDialog(this, "Word already exists", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }
        
        Vector<String> row = new Vector<>();
        row.add(word);
        row.add(pronunciation);
        row.add(meaning);
        wordsTableModel.addRow(row);
        
        clearWordFields();
        saveWordsToFile();
        logMessage("Added word: " + word);
    }
    
    private void editWord(ActionEvent e) {
        int selectedRow = wordsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String word = wordField.getText().trim().toUpperCase();
            String pronunciation = pronunciationField.getText().trim();
            String meaning = meaningField.getText().trim();
            
            if (word.isEmpty() || pronunciation.isEmpty() || meaning.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            
            String oldWord = (String) wordsTableModel.getValueAt(selectedRow, 0);
            for (int i = 0; i < wordsTableModel.getRowCount(); i++) {
                if (i != selectedRow && word.equals(wordsTableModel.getValueAt(i, 0))) {
                    JOptionPane.showMessageDialog(this, "Word already exists", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }
            
            wordsTableModel.setValueAt(word, selectedRow, 0);
            wordsTableModel.setValueAt(pronunciation, selectedRow, 1);
            wordsTableModel.setValueAt(meaning, selectedRow, 2);
            
            clearWordFields();
            saveWordsToFile();
            logMessage("Edited word: " + oldWord + " -> " + word);
        }
    }
    
    private void deleteWord(ActionEvent e) {
        int selectedRow = wordsTable.getSelectedRow();
        if (selectedRow >= 0) {
            String word = (String) wordsTableModel.getValueAt(selectedRow, 0);
            int confirm = JOptionPane.showConfirmDialog(this, 
                "Are you sure you want to delete the word '" + word + "'?", 
                "Confirm Delete", 
                JOptionPane.YES_NO_OPTION);
            
            if (confirm == JOptionPane.YES_OPTION) {
                wordsTableModel.removeRow(selectedRow);
                clearWordFields();
                saveWordsToFile();
                logMessage("Deleted word: " + word);
            }
        }
    }
    
    private void clearWordFields() {
        wordField.setText("");
        pronunciationField.setText("");
        meaningField.setText("");
        wordsTable.clearSelection();
    }
    
    private void logMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = java.time.LocalTime.now().toString().substring(0, 8);
            logArea.append("[" + timestamp + "] " + message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }
    
    @Override
    public void dispose() {
        if (updateScheduler != null) {
            updateScheduler.shutdown();
        }
        if (serverRunning && server != null) {
            try {
                server.stop();
            } catch (IOException e) {
                System.err.println("Error stopping server: " + e.getMessage());
            }
        }
        super.dispose();
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI().setVisible(true));
    }
}