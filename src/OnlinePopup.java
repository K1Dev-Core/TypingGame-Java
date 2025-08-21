import javax.swing.*;
import java.awt.*;
import java.util.List;
import shared.*;
import client.NetworkClient;

public class OnlinePopup extends JDialog {
    private GamePanel gamePanel;
    private UISettings uiSettings;
    private NetworkClient networkClient;
    private Player localPlayer;
    private GameRoom currentRoom;
    private boolean isHost = false;
    private boolean isNameSet = false;

    private JPanel namePanel;
    private JPanel roomPanel;
    private JTextField playerNameField;
    private JPanel roomListPanel;
    private JScrollPane roomScrollPane;
    private JLabel statusLabel;
    private CardLayout cardLayout;

    public OnlinePopup(GamePanel gamePanel, UISettings uiSettings) {
        super((Frame) SwingUtilities.getWindowAncestor(gamePanel), "โหมดออนไลน์", true);
        this.gamePanel = gamePanel;
        this.uiSettings = uiSettings;

        initializeComponents();
        setupLayout();
        setSize(500, 400);
        setLocationRelativeTo(gamePanel);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initializeComponents() {
        cardLayout = new CardLayout();

        playerNameField = new JTextField(15);
        playerNameField.setFont(uiSettings.fontPlain16);

        roomListPanel = new JPanel();
        roomListPanel.setLayout(new BoxLayout(roomListPanel, BoxLayout.Y_AXIS));
        roomListPanel.setBackground(new Color(32, 36, 42));

        roomScrollPane = new JScrollPane(roomListPanel);
        roomScrollPane.setPreferredSize(new Dimension(400, 250));
        roomScrollPane.setBorder(BorderFactory.createTitledBorder("ห้องที่ว่าง"));

        statusLabel = new JLabel("ใส่ชื่อเพื่อเข้าสู่โหมดออนไลน์", JLabel.CENTER);
        statusLabel.setFont(uiSettings.fontPlain16);
        statusLabel.setForeground(Color.WHITE);

        setupNamePanel();
        setupRoomPanel();
    }

    private void setupNamePanel() {
        namePanel = new JPanel(new BorderLayout());
        namePanel.setBackground(new Color(18, 20, 24));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(new Color(18, 20, 24));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);

        JLabel titleLabel = new JLabel("โหมดออนไลน์", JLabel.CENTER);
        titleLabel.setFont(uiSettings.fontBold24);
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        centerPanel.add(titleLabel, gbc);

        JLabel nameLabel = new JLabel("ชื่อผู้เล่น:");
        nameLabel.setFont(uiSettings.fontPlain16);
        nameLabel.setForeground(Color.WHITE);
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        centerPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        centerPanel.add(playerNameField, gbc);

        JButton enterButton = new JButton("เข้าสู่ห้อง");
        enterButton.setFont(uiSettings.fontPlain16);
        enterButton.addActionListener(e -> enterRoomList());
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        centerPanel.add(enterButton, gbc);

        namePanel.add(centerPanel, BorderLayout.CENTER);

        JLabel bottomLabel = new JLabel("กรุณาใส่ชื่อเพื่อเข้าสู่โหมดออนไลน์", JLabel.CENTER);
        bottomLabel.setFont(uiSettings.fontPlain16);
        bottomLabel.setForeground(Color.LIGHT_GRAY);
        namePanel.add(bottomLabel, BorderLayout.SOUTH);
    }

    private void setupRoomPanel() {
        roomPanel = new JPanel(new BorderLayout());
        roomPanel.setBackground(new Color(18, 20, 24));

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.setBackground(new Color(18, 20, 24));

        JLabel playerLabel = new JLabel();
        playerLabel.setFont(uiSettings.fontPlain16);
        playerLabel.setForeground(Color.WHITE);
        topPanel.add(playerLabel);

        JButton connectButton = new JButton("เชื่อมต่อเซิฟเวอร์");
        connectButton.addActionListener(e -> connectToServer());
        topPanel.add(connectButton);

        JButton createRoomButton = new JButton("สร้างห้อง");
        createRoomButton.addActionListener(e -> createRoom());
        topPanel.add(createRoomButton);

        JButton backButton = new JButton("ย้อนกลับ");
        backButton.addActionListener(e -> showNamePanel());
        topPanel.add(backButton);

        roomPanel.add(topPanel, BorderLayout.NORTH);
        roomPanel.add(roomScrollPane, BorderLayout.CENTER);
        roomPanel.add(statusLabel, BorderLayout.SOUTH);
    }

    private void setupLayout() {
        setLayout(cardLayout);
        getContentPane().setBackground(new Color(18, 20, 24));

        add(namePanel, "NAME");
        add(roomPanel, "ROOM");

        cardLayout.show(getContentPane(), "NAME");
    }

    private void enterRoomList() {
        if (playerNameField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "กรุณาใส่ชื่อ", "ข้อผิดพลาด", JOptionPane.ERROR_MESSAGE);
            return;
        }

        localPlayer = new Player(java.util.UUID.randomUUID().toString(),
                playerNameField.getText().trim());

        JLabel playerLabel = (JLabel) ((JPanel) roomPanel.getComponent(0)).getComponent(0);
        playerLabel.setText("ผู้เล่น: " + localPlayer.name);

        isNameSet = true;
        cardLayout.show(getContentPane(), "ROOM");
        statusLabel.setText("กดเชื่อมต่อเซิฟเวอร์เพื่อดูห้องที่ว่าง");
    }

    private void showNamePanel() {
        if (networkClient != null) {
            networkClient.disconnect();
            networkClient = null;
        }
        cardLayout.show(getContentPane(), "NAME");
        statusLabel.setText("กรุณาใส่ชื่อเพื่อเข้าสู่โหมดออนไลน์");
    }

    private void connectToServer() {
        if (!isNameSet || localPlayer == null) {
            statusLabel.setText("กรุณาใส่ชื่อก่อน");
            return;
        }

        statusLabel.setText("กำลังเชื่อมต่อ...");

        networkClient = new NetworkClient(new NetworkClient.NetworkListener() {
            @Override
            public void onMessageReceived(NetworkMessage message) {
                handleNetworkMessage(message);
            }

            @Override
            public void onDisconnected() {
                SwingUtilities.invokeLater(() -> {
                    statusLabel.setText("การเชื่อมต่อขาด - กดเชื่อมต่อใหม่");
                });
            }
        });

        if (networkClient.connect("localhost", 8888)) {
            statusLabel.setText("เชื่อมต่อสำเร็จ");
            requestRoomList();
        } else {
            statusLabel.setText("เชื่อมต่อไม่สำเร็จ");
        }
    }

    private void createRoom() {
        if (networkClient != null && networkClient.isConnected()) {
            NetworkMessage createMessage = new NetworkMessage(
                    NetworkMessage.MessageType.CREATE_ROOM,
                    localPlayer.id,
                    null,
                    localPlayer);
            networkClient.sendMessage(createMessage);
        }
    }

    private void requestRoomList() {
        if (networkClient != null && networkClient.isConnected()) {
            NetworkMessage requestMessage = new NetworkMessage(
                    NetworkMessage.MessageType.ROOM_LIST,
                    localPlayer.id,
                    null,
                    null);
            networkClient.sendMessage(requestMessage);
        }
    }

    private void joinRoom(GameRoom room) {
        if (networkClient != null && networkClient.isConnected() && room.players.size() < 2) {
            NetworkMessage joinMessage = new NetworkMessage(
                    NetworkMessage.MessageType.JOIN_ROOM,
                    localPlayer.id,
                    room.id,
                    localPlayer);
            networkClient.sendMessage(joinMessage);
        }
    }

    private void handleNetworkMessage(NetworkMessage message) {
        SwingUtilities.invokeLater(() -> {
            switch (message.type) {
                case ROOM_LIST:
                    if (message.data instanceof List) {
                        updateRoomList((List<GameRoom>) message.data);
                    }
                    break;
                case CREATE_ROOM:
                case JOIN_ROOM:
                    if (message.data instanceof GameRoom) {
                        currentRoom = (GameRoom) message.data;
                        isHost = currentRoom.host.id.equals(localPlayer.id);
                        if (currentRoom.players.size() >= 2) {
                            startOnlineGame();
                        } else {
                            statusLabel.setText("รอผู้เล่นอีกคน...");
                        }
                    }
                    break;
                case ROOM_UPDATE:
                    if (message.data instanceof GameRoom) {
                        GameRoom room = (GameRoom) message.data;
                        if (currentRoom != null && room.id.equals(currentRoom.id)) {
                            currentRoom = room;
                            if (currentRoom.players.size() >= 2) {
                                startOnlineGame();
                            }
                        }
                    }
                    break;
                case START_GAME:
                    startOnlineGame();
                    break;
                default:
                    break;
            }
        });
    }

    private void updateRoomList(List<GameRoom> rooms) {
        roomListPanel.removeAll();

        for (GameRoom room : rooms) {
            if (room.players.size() < 2) {
                JPanel roomPanel = new JPanel(new BorderLayout());
                roomPanel.setBorder(BorderFactory.createEtchedBorder());

                JLabel nameLabel = new JLabel(room.host.name + " (" + room.players.size() + "/2)");
                JButton joinButton = new JButton("เข้าร่วม");
                joinButton.addActionListener(e -> joinRoom(room));

                roomPanel.add(nameLabel, BorderLayout.CENTER);
                roomPanel.add(joinButton, BorderLayout.EAST);
                roomListPanel.add(roomPanel);
            }
        }

        roomListPanel.revalidate();
        roomListPanel.repaint();
    }

    private void startOnlineGame() {
        dispose();
        gamePanel.startOnlineGame(currentRoom, networkClient, localPlayer, isHost);
    }
}
