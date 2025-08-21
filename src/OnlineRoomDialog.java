import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.util.List;
import java.util.ArrayList;
import shared.*;
import client.NetworkClient;

public class OnlineRoomDialog extends JDialog {
    private GamePanel gamePanel;
    private UISettings uiSettings;
    private NetworkClient networkClient;
    private Player localPlayer;

    private JPanel mainPanel;
    private JPanel namePanel;
    private JPanel roomPanel;
    private CardLayout cardLayout;

    // Name input components
    private JTextField nameField;
    private JButton enterButton;

    // Room list components
    private JPanel roomListPanel;
    private JScrollPane roomScrollPane;
    private JLabel statusLabel;
    private JLabel playerLabel;
    private JButton createRoomButton;
    private JButton refreshButton;
    // backButton removed - using close button instead

    // Room view components (for when user is in a room)
    private JPanel roomViewPanel;
    private JLabel roomNameLabel;
    private JLabel playersInRoomLabel;
    private JButton leaveRoomButton;
    private JButton startGameButton;
    private GameRoom currentRoom;

    private List<GameRoom> availableRooms = new ArrayList<>();
    private boolean isConnected = false;
    private Timer autoRefreshTimer;

    public OnlineRoomDialog(GamePanel parent, UISettings uiSettings) {
        super((Frame) SwingUtilities.getWindowAncestor(parent), "โหมดออนไลน์", true);
        this.gamePanel = parent;
        this.uiSettings = uiSettings;

        setupDialog();
        createComponents();
        layoutComponents();
        setupListeners();

        if (!parent.getGameState().getPlayerName().isEmpty()) {
            localPlayer = new Player("player-" + System.currentTimeMillis(),
                    parent.getGameState().getPlayerName());
            playerLabel.setText("ผู้เล่น: " + localPlayer.name);
            showRoomPanel();
        } else {
            showNamePanel();
        }
    }

    private void setupDialog() {
        setSize(600, 500);
        setLocationRelativeTo(gamePanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setUndecorated(true); // Remove window decorations

        getContentPane().setBackground(new Color(25, 30, 35));
    }

    private void createComponents() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);
        mainPanel.setBackground(new Color(25, 30, 35));

        namePanel = createNamePanel();
        roomPanel = createRoomPanel();
        roomViewPanel = createRoomViewPanel();

        mainPanel.add(namePanel, "NAME");
        mainPanel.add(roomPanel, "ROOM");
        mainPanel.add(roomViewPanel, "ROOM_VIEW");
    }

    private JPanel createNamePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 30, 35));

        // Title bar with close button
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(20, 25, 30));
        titleBar.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleBarLabel = new JLabel("โหมดออนไลน์");
        titleBarLabel.setFont(uiSettings.fontBold20);
        titleBarLabel.setForeground(Color.WHITE);
        titleBar.add(titleBarLabel, BorderLayout.WEST);

        JButton closeButton = createStyledButton("✕", new Color(231, 76, 60));
        closeButton.setPreferredSize(new Dimension(40, 30));
        closeButton.addActionListener(e -> dispose());
        titleBar.add(closeButton, BorderLayout.EAST);

        panel.add(titleBar, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setBackground(new Color(25, 30, 35));
        contentPanel.setBorder(new EmptyBorder(60, 40, 60, 40));

        JLabel titleLabel = new JLabel("เข้าสู่ระบบ");
        titleLabel.setFont(uiSettings.fontBold24);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(titleLabel);

        contentPanel.add(Box.createVerticalStrut(40));

        JLabel subtitleLabel = new JLabel("กรุณาใส่ชื่อผู้เล่น");
        subtitleLabel.setFont(uiSettings.fontPlain16);
        subtitleLabel.setForeground(new Color(180, 180, 180));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(subtitleLabel);

        contentPanel.add(Box.createVerticalStrut(30));

        nameField = new JTextField(20);
        nameField.setFont(uiSettings.fontPlain16);
        nameField.setMaximumSize(new Dimension(300, 40));
        nameField.setBackground(new Color(45, 52, 60));
        nameField.setForeground(Color.WHITE);
        nameField.setCaretColor(Color.WHITE);
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 90), 2),
                new EmptyBorder(8, 12, 8, 12)));
        nameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(nameField);

        contentPanel.add(Box.createVerticalStrut(30));

        enterButton = createStyledButton("เข้าสู่โหมดออนไลน์", new Color(52, 152, 219));
        enterButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        contentPanel.add(enterButton);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRoomPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(25, 30, 35));

        // Title bar with close button
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(20, 25, 30));
        titleBar.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleBarLabel = new JLabel("รายการห้อง");
        titleBarLabel.setFont(uiSettings.fontBold20);
        titleBarLabel.setForeground(Color.WHITE);
        titleBar.add(titleBarLabel, BorderLayout.WEST);

        JButton closeButton = createStyledButton("✕", new Color(231, 76, 60));
        closeButton.setPreferredSize(new Dimension(40, 30));
        closeButton.addActionListener(e -> dispose());
        titleBar.add(closeButton, BorderLayout.EAST);

        panel.add(titleBar, BorderLayout.NORTH);

        // Content panel
        JPanel contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(25, 30, 35));
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(25, 30, 35));

        playerLabel = new JLabel("ผู้เล่น: ");
        playerLabel.setFont(uiSettings.fontBold16);
        playerLabel.setForeground(Color.WHITE);
        topPanel.add(playerLabel, BorderLayout.WEST);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(new Color(25, 30, 35));

        createRoomButton = createStyledButton("สร้างห้อง", new Color(46, 204, 113));
        refreshButton = createStyledButton("รีเฟรช", new Color(241, 196, 15));
        // Remove back button

        buttonPanel.add(createRoomButton);
        buttonPanel.add(refreshButton);
        topPanel.add(buttonPanel, BorderLayout.EAST);

        contentPanel.add(topPanel, BorderLayout.NORTH);

        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(new Color(32, 38, 45));

        roomScrollPane = new JScrollPane(roomListPanel);
        roomScrollPane.setBackground(new Color(32, 38, 45));
        roomScrollPane.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 90), 1),
                "ห้องที่ว่าง",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                uiSettings.fontPlain16,
                Color.WHITE));
        roomScrollPane.setPreferredSize(new Dimension(0, 300));

        contentPanel.add(roomScrollPane, BorderLayout.CENTER);

        statusLabel = new JLabel("กดรีเฟรชเพื่อเชื่อมต่อเซิฟเวอร์", JLabel.CENTER);
        statusLabel.setFont(uiSettings.fontPlain16);
        statusLabel.setForeground(new Color(180, 180, 180));
        statusLabel.setBorder(new EmptyBorder(15, 0, 15, 0));
        contentPanel.add(statusLabel, BorderLayout.SOUTH);

        panel.add(contentPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRoomViewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(30, 35, 42));
        panel.setBorder(new EmptyBorder(20, 40, 40, 40));

        // Title bar with close button
        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(new Color(25, 30, 35));
        titleBar.setBorder(new EmptyBorder(10, 15, 10, 15));

        JLabel titleLabel = new JLabel("ห้องแข่งขัน");
        titleLabel.setFont(uiSettings.fontBold20);
        titleLabel.setForeground(Color.WHITE);
        titleBar.add(titleLabel, BorderLayout.WEST);

        JButton closeButton = createStyledButton("✕", new Color(231, 76, 60));
        closeButton.setPreferredSize(new Dimension(40, 30));
        closeButton.addActionListener(e -> dispose());
        titleBar.add(closeButton, BorderLayout.EAST);

        panel.add(titleBar, BorderLayout.NORTH);

        // Room info section
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setBackground(new Color(30, 35, 42));

        roomNameLabel = new JLabel("ห้อง: ");
        roomNameLabel.setFont(uiSettings.fontBold20);
        roomNameLabel.setForeground(Color.WHITE);
        roomNameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(roomNameLabel);

        infoPanel.add(Box.createVerticalStrut(20));

        playersInRoomLabel = new JLabel("ผู้เล่นในห้อง: ");
        playersInRoomLabel.setFont(uiSettings.fontPlain16);
        playersInRoomLabel.setForeground(new Color(180, 180, 180));
        playersInRoomLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(playersInRoomLabel);

        infoPanel.add(Box.createVerticalStrut(20));

        JLabel waitingLabel = new JLabel("รอผู้เล่นเข้าร่วม...");
        waitingLabel.setFont(uiSettings.fontPlain16);
        waitingLabel.setForeground(new Color(255, 215, 0));
        waitingLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(waitingLabel);

        panel.add(infoPanel, BorderLayout.CENTER);

        // Button section (only start game button, no leave room button)
        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(new Color(30, 35, 42));

        // Start Game button (only for host)
        startGameButton = createStyledButton("เริ่มเกม", new Color(46, 204, 113));
        startGameButton.addActionListener(e -> startGame());
        startGameButton.setVisible(false); // Hidden by default
        buttonPanel.add(startGameButton);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JButton createStyledButton(String text, Color bgColor) {
        JButton button = new JButton(text);
        button.setFont(uiSettings.fontPlain16);
        button.setForeground(Color.WHITE);
        button.setBackground(bgColor);
        button.setBorder(BorderFactory.createEmptyBorder(12, 24, 12, 24));
        button.setFocusPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));

        Color hoverColor = bgColor.brighter();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }

            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
        });

        return button;
    }

    private void layoutComponents() {
        setLayout(new BorderLayout());
        add(mainPanel, BorderLayout.CENTER);
    }

    private void setupListeners() {
        enterButton.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showMessage("กรุณาใส่ชื่อผู้เล่น", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
                return;
            }

            localPlayer = new Player("player-" + System.currentTimeMillis(), name);
            gamePanel.getGameState().setPlayerName(name);
            playerLabel.setText("ผู้เล่น: " + name);
            showRoomPanel();
        });

        createRoomButton.addActionListener(e -> createRoom());
        refreshButton.addActionListener(e -> refreshRoomList());
        // backButton removed - using close button instead
    }

    private void showNamePanel() {
        cardLayout.show(mainPanel, "NAME");
        nameField.requestFocus();
    }

    private void showRoomPanel() {
        cardLayout.show(mainPanel, "ROOM");
        // Don't auto-connect, let user click refresh
        statusLabel.setText("กดรีเฟรชเพื่อเชื่อมต่อและดูห้องที่ว่าง");
        createRoomButton.setEnabled(false);

        // Stop auto-refresh when showing room panel
        stopAutoRefresh();
    }

    private void showRoomView(GameRoom room) {
        currentRoom = room;
        roomNameLabel.setText("ห้อง: " + room.name);
        updatePlayersInRoom(room);

        // Show start button only if user is host and room has enough players
        boolean isHost = localPlayer != null && room.players.size() > 0 &&
                room.players.get(0).id.equals(localPlayer.id);
        startGameButton.setVisible(isHost && room.players.size() >= 2);

        cardLayout.show(mainPanel, "ROOM_VIEW");
    }

    private void updatePlayersInRoom(GameRoom room) {
        StringBuilder playersText = new StringBuilder(
                "ผู้เล่นในห้อง (" + room.players.size() + "/" + room.maxPlayers + "): ");
        for (int i = 0; i < room.players.size(); i++) {
            if (i > 0)
                playersText.append(", ");
            playersText.append(room.players.get(i).name);
        }

        // Add room state info
        String stateText = "";
        switch (room.roomState) {
            case WAITING_FOR_PLAYERS:
                stateText = " [รอผู้เล่น]";
                break;
            case WAITING_FOR_HOST:
                stateText = " [รอหัวห้องเริ่ม]";
                break;
            case COUNTDOWN:
                stateText = " [นับถอยหลัง " + room.countdown + "]";
                break;
            case GAME_STARTED:
                stateText = " [เกมเริ่มแล้ว]";
                break;
            case GAME_ENDED:
                stateText = " [เกมจบแล้ว]";
                break;
        }

        playersInRoomLabel.setText(playersText.toString() + stateText);

        // Update start button visibility
        boolean isHost = localPlayer != null && room.players.size() > 0 &&
                room.players.get(0).id.equals(localPlayer.id);
        startGameButton.setVisible(isHost && room.players.size() >= 2 &&
                (room.roomState == GameRoom.RoomState.WAITING_FOR_HOST ||
                        room.roomState == GameRoom.RoomState.WAITING_FOR_PLAYERS));
        startGameButton.setEnabled(room.roomState != GameRoom.RoomState.COUNTDOWN);
    }

    private void leaveRoom() {
        if (networkClient != null && currentRoom != null && localPlayer != null) {
            try {
                NetworkMessage leaveMessage = new NetworkMessage(NetworkMessage.MessageType.LEAVE_ROOM,
                        localPlayer.id, currentRoom.id, null);
                networkClient.sendMessage(leaveMessage);
                currentRoom = null;
                showRoomPanel();
                refreshRoomList(); // Refresh room list after leaving
            } catch (Exception ex) {
                System.err.println("Error leaving room: " + ex.getMessage());
                showMessage("เกิดข้อผิดพลาดในการออกจากห้อง", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void startGame() {
        if (networkClient != null && currentRoom != null && localPlayer != null) {
            // Check if user is host
            if (currentRoom.players.size() > 0 && currentRoom.players.get(0).id.equals(localPlayer.id)) {
                if (currentRoom.players.size() < 2) {
                    showMessage("ต้องมีผู้เล่นอย่างน้อย 2 คน", "ไม่สามารถเริ่มเกมได้", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    NetworkMessage startMessage = new NetworkMessage(NetworkMessage.MessageType.START_GAME,
                            localPlayer.id, currentRoom.id, null);
                    networkClient.sendMessage(startMessage);
                    startGameButton.setEnabled(false);

                    // Start online game immediately and close dialog
                    gamePanel.startOnlineGame(currentRoom, networkClient, localPlayer, true);
                    dispose(); // Close dialog after starting the game
                } catch (Exception ex) {
                    System.err.println("Error starting game: " + ex.getMessage());
                    showMessage("เกิดข้อผิดพลาดในการเริ่มเกม", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
                }
            } else {
                showMessage("เฉพาะหัวห้องเท่านั้นที่สามารถเริ่มเกมได้", "ไม่อนุญาต", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void connectToServer() {
        if (localPlayer == null) {
            showMessage("กรุณาใส่ชื่อก่อน", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }

        statusLabel.setText("กำลังเชื่อมต่อเซิฟเวอร์...");
        createRoomButton.setEnabled(false);
        refreshButton.setEnabled(false);

        // Disconnect existing connection
        if (networkClient != null) {
            try {
                networkClient.disconnect();
            } catch (Exception ex) {
                // Ignore disconnect errors
            }
            networkClient = null;
        }

        // Create new connection
        SwingUtilities.invokeLater(() -> {
            try {
                networkClient = new NetworkClient(new NetworkClient.NetworkListener() {
                    @Override
                    public void onMessageReceived(NetworkMessage message) {
                        System.out.println("Received message: " + message.type);
                        SwingUtilities.invokeLater(() -> handleNetworkMessage(message));
                    }

                    @Override
                    public void onDisconnected() {
                        System.out.println("Network disconnected!");
                        SwingUtilities.invokeLater(() -> {
                            isConnected = false;
                            statusLabel.setText("การเชื่อมต่อขาด - กดรีเฟรชเพื่อเชื่อมต่อใหม่");
                            createRoomButton.setEnabled(false);
                            refreshButton.setEnabled(true);
                        });
                    }
                });

                if (networkClient.connect("localhost", 8888)) {
                    isConnected = true;
                    statusLabel.setText("เชื่อมต่อสำเร็จ - กำลังโหลดห้อง...");

                    // Request room list after a short delay
                    Timer timer = new Timer(500, e -> {
                        requestRoomList();
                        startAutoRefresh(); // Start auto-refresh after initial load
                    });
                    timer.setRepeats(false);
                    timer.start();
                } else {
                    statusLabel.setText("เชื่อมต่อไม่สำเร็จ - ตรวจสอบว่าเซิฟเวอร์เปิดอยู่");
                    refreshButton.setEnabled(true);
                }
            } catch (Exception ex) {
                statusLabel.setText("เกิดข้อผิดพลาด: " + ex.getMessage());
                refreshButton.setEnabled(true);
            }
        });
    }

    private void refreshRoomList() {
        if (!isConnected || networkClient == null) {
            // Not connected, need to connect first
            connectToServer();
            return;
        }

        // Already connected, just request room list
        statusLabel.setText("กำลังโหลดห้อง...");
        refreshButton.setEnabled(false);

        try {
            // Send ROOM_LIST request to server
            NetworkMessage request = new NetworkMessage(NetworkMessage.MessageType.ROOM_LIST,
                    localPlayer.id, null, null);
            networkClient.sendMessage(request);

            // Enable button after a delay
            Timer timer = new Timer(1000, e -> refreshButton.setEnabled(true));
            timer.setRepeats(false);
            timer.start();

        } catch (Exception ex) {
            System.err.println("Error requesting room list: " + ex.getMessage());
            statusLabel.setText("เกิดข้อผิดพลาดในการโหลดห้อง");
            refreshButton.setEnabled(true);
        }
    }

    private void startAutoRefresh() {
        stopAutoRefresh(); // Stop existing timer if any
        autoRefreshTimer = new Timer(1000, e -> { // Auto-refresh every 1 second (more frequent)
            if (isConnected && networkClient != null) {
                requestRoomList();
            }
        });
        autoRefreshTimer.start();
    }

    private void stopAutoRefresh() {
        if (autoRefreshTimer != null && autoRefreshTimer.isRunning()) {
            autoRefreshTimer.stop();
        }
    }

    private void requestRoomList() {
        if (networkClient != null && isConnected) {
            // Just show empty room list for now since server might not support ROOM_LIST
            updateRoomList(new ArrayList<>());
            statusLabel.setText("เชื่อมต่อแล้ว - คุณสามารถสร้างห้องได้");
            createRoomButton.setEnabled(true);
            refreshButton.setEnabled(true);
        }
    }

    private void createRoom() {
        System.out.println("createRoom() called, isConnected=" + isConnected + ", networkClient="
                + (networkClient != null) + ", localPlayer=" + (localPlayer != null));

        if (!isConnected || networkClient == null || localPlayer == null) {
            showMessage("กรุณาเชื่อมต่อเซิฟเวอร์ก่อน", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String roomName = JOptionPane.showInputDialog(this, "ชื่อห้อง:", "สร้างห้องใหม่", JOptionPane.PLAIN_MESSAGE);
        if (roomName != null && !roomName.trim().isEmpty()) {
            try {
                String roomId = "room-" + System.currentTimeMillis();
                GameRoom room = new GameRoom(roomId, roomName.trim(), localPlayer);

                System.out.println("Sending CREATE_ROOM message for room: " + roomName.trim());
                networkClient.sendMessage(new NetworkMessage(NetworkMessage.MessageType.CREATE_ROOM,
                        localPlayer.id, roomId, room));

                statusLabel.setText("กำลังสร้างห้อง: " + roomName.trim());
                createRoomButton.setEnabled(false);
            } catch (Exception ex) {
                System.err.println("Error creating room: " + ex.getMessage());
                ex.printStackTrace();
                showMessage("เกิดข้อผิดพลาดในการสร้างห้อง: " + ex.getMessage(),
                        "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {
        try {
            switch (message.type) {
                case ROOM_LIST:
                    if (message.data instanceof List) {
                        updateRoomList((List<GameRoom>) message.data);
                    }
                    break;
                case CREATE_ROOM:
                    if (message.data instanceof GameRoom) {
                        GameRoom room = (GameRoom) message.data;

                        // Show room view immediately after creating room
                        SwingUtilities.invokeLater(() -> {
                            showRoomView(room);
                            gamePanel.startOnlineGame(room, networkClient, localPlayer, true);
                        });
                    }
                    break;
                case COUNTDOWN_START:
                    SwingUtilities.invokeLater(() -> {
                        showMessage("ผู้เล่นครบแล้ว! กำลังเริ่มเกม...", "พร้อมเริ่มเกม",
                                JOptionPane.INFORMATION_MESSAGE);
                        // Also send to GamePanel if it exists
                        if (gamePanel != null) {
                            gamePanel.handleOnlineMessage(message);
                        }
                    });
                    break;
                case COUNTDOWN_UPDATE:
                    if (message.data instanceof Integer) {
                        int countdown = (Integer) message.data;
                        SwingUtilities.invokeLater(() -> {
                            showMessage("เริ่มเกมใน " + countdown + " วินาที", "เตรียมพร้อม",
                                    JOptionPane.INFORMATION_MESSAGE);
                            // Also send to GamePanel if it exists
                            if (gamePanel != null) {
                                gamePanel.handleOnlineMessage(message);
                            }
                        });
                    }
                    break;
                case GAME_START:
                    SwingUtilities.invokeLater(() -> {
                        // Game is starting - now we can close the dialog
                        showMessage("เริ่มเกม! คำแรก: " + message.data, "เริ่มแข่ง!", JOptionPane.INFORMATION_MESSAGE);
                        // Send to GamePanel
                        if (gamePanel != null) {
                            gamePanel.handleOnlineMessage(message);
                        }
                        dispose(); // Close dialog when game actually starts
                    });
                    break;
                case PLAYER_JOIN:
                    // Update player count in room view
                    if (currentRoom != null && message.data instanceof Player) {
                        Player joinedPlayer = (Player) message.data;
                        // Add player to current room if not already there
                        boolean playerExists = currentRoom.players.stream()
                                .anyMatch(p -> p.id.equals(joinedPlayer.id));
                        if (!playerExists) {
                            currentRoom.players.add(joinedPlayer);
                        }
                        SwingUtilities.invokeLater(() -> {
                            updatePlayersInRoom(currentRoom);
                        });
                    }
                    break;
                case ROOM_UPDATE:
                    if (currentRoom != null && message.data instanceof GameRoom) {
                        GameRoom updatedRoom = (GameRoom) message.data;
                        if (updatedRoom.id.equals(currentRoom.id)) {
                            currentRoom = updatedRoom;
                            SwingUtilities.invokeLater(() -> {
                                updatePlayersInRoom(currentRoom);
                            });
                        }
                    }
                    break;
                case JOIN_ROOM:
                    if (message.data instanceof GameRoom) {
                        GameRoom room = (GameRoom) message.data;
                        statusLabel.setText("เข้าห้องสำเร็จ! กำลังเข้าเกม...");

                        SwingUtilities.invokeLater(() -> {
                            dispose();
                            gamePanel.startOnlineGame(room, networkClient, localPlayer, false);
                        });
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            System.err.println("Error handling network message: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    private void updateRoomList(List<GameRoom> rooms) {
        this.availableRooms = new ArrayList<>(rooms);
        roomListPanel.removeAll();

        if (rooms.isEmpty()) {
            JLabel emptyLabel = new JLabel("ไม่มีห้องที่ว่าง - สร้างห้องใหม่ได้เลย!");
            emptyLabel.setFont(uiSettings.fontPlain16);
            emptyLabel.setForeground(new Color(150, 150, 150));
            emptyLabel.setHorizontalAlignment(JLabel.CENTER);
            emptyLabel.setBorder(new EmptyBorder(30, 0, 30, 0));
            roomListPanel.add(emptyLabel);
        } else {
            for (GameRoom room : rooms) {
                roomListPanel.add(createRoomCard(room));
                roomListPanel.add(Box.createVerticalStrut(10));
            }
        }

        statusLabel.setText("พบ " + rooms.size() + " ห้อง");
        createRoomButton.setEnabled(isConnected);

        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    private JPanel createRoomCard(GameRoom room) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(45, 52, 60));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(70, 80, 90), 1),
                new EmptyBorder(15, 20, 15, 20)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel infoPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        infoPanel.setBackground(new Color(45, 52, 60));

        JLabel nameLabel = new JLabel(room.name);
        nameLabel.setFont(uiSettings.fontBold16);
        nameLabel.setForeground(Color.WHITE);
        infoPanel.add(nameLabel);

        JLabel playersLabel = new JLabel("ผู้เล่น: " + room.players.size() + "/2");
        playersLabel.setFont(uiSettings.fontPlain16);
        playersLabel.setForeground(new Color(180, 180, 180));
        infoPanel.add(playersLabel);

        card.add(infoPanel, BorderLayout.CENTER);

        JButton joinButton = createStyledButton("เข้าร่วม", new Color(52, 152, 219));
        joinButton.setEnabled(room.players.size() < 2);
        joinButton.addActionListener(e -> joinRoom(room));

        card.add(joinButton, BorderLayout.EAST);

        return card;
    }

    private void joinRoom(GameRoom room) {
        if (networkClient != null && localPlayer != null && isConnected) {
            try {
                networkClient.sendMessage(new NetworkMessage(NetworkMessage.MessageType.JOIN_ROOM,
                        localPlayer.id, room.id, localPlayer));
                statusLabel.setText("กำลังเข้าห้อง: " + room.name);
            } catch (Exception ex) {
                showMessage("เกิดข้อผิดพลาดในการเข้าห้อง: " + ex.getMessage(),
                        "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            showMessage("การเชื่อมต่อขาด กรุณากดรีเฟรช", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showMessage(String message, String title, int messageType) {
        JOptionPane.showMessageDialog(this, message, title, messageType);
    }

    @Override
    public void dispose() {
        stopAutoRefresh(); // Stop auto-refresh timer

        if (networkClient != null) {
            try {
                networkClient.disconnect();
            } catch (Exception ex) {
                // Ignore disconnect errors during disposal
            }
        }
        super.dispose();
    }
}
