import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.net.DatagramSocket;
import java.net.SocketException;

public class GUI extends JFrame {
    private final JFileChooser root_fileChooser = new JFileChooser();
    private final JFileChooser download_fileChooser = new JFileChooser();
    private JTextArea rootTextArea, destinationTextArea, searchTextArea;
    //private JList
    // private DefaultListModel
    private DefaultListModel<String> exclude_listModel;
    private JCheckBox checkBox;
    public NetworkUtils networkUtils;
    private BroadcastListener broadcast_listener;
    private Thread listenerThread;
    boolean isConnected = false;
    SearchListener search_listener;
    public DefaultListModel<String> foundFilesModel;
    private DefaultListModel<String> downloadingModel;
    private JList<String> downloadingList;
    public JList<String> foundFilesList;
    private FileDownloadListener fileDownloadListener;
    private Thread fileDownloadListenerThread;
    Thread searchThread;
    String peerIP;

    private  HashMap<String, Integer> peerTable ;

    public DatagramSocket broadcast_Socket;
    public DatagramSocket udp_Socket;
    public ServerSocket TCP_download_socket;

    private final int broadcastPort = 8888 ;
    private final int localPort = 8891;
    private final int TCP_downloadPort = 8899;

    public GUI(String title) {

        peerIP = "default";
        try {
            peerIP = NetworkUtils.getLocalIPAddress();
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }


        try {
            this.broadcast_Socket = new DatagramSocket(broadcastPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        try {
            udp_Socket = new DatagramSocket(localPort);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        try {
            TCP_download_socket = new ServerSocket(TCP_downloadPort);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("ip address: " + this.peerIP );
        setTitle("ip address: " + this.peerIP  + (isConnected ? " Connected " : " Not connected") );
        setLayout(new GridLayout(6, 1));
        root_fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        download_fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);



        this.networkUtils = new NetworkUtils();


        // Set up menu bar
        setupMenuBar();

        // Root of the P2P shared folder
        setupRootPanel();

        // Destination folder
        setupDestinationPanel();

        // Settings panel for file exclusion and root folder settings
        setupSettingsPanel();

        // Downloading files panel
        setupDownloadingPanel();

        // Found files and search section
        setupFoundFilesPanel();


        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(600, 850); //900,1000
        setLocationRelativeTo(null); // Center the window on the screen
        setVisible(true);
    }

    private void setupMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem connectItem = new JMenuItem("Connect");
        JMenuItem disconnectItem = new JMenuItem("Disconnect");
        JMenuItem exitItem = new JMenuItem("Exit");

        // Connector for broadcasting
        P2PConnector connector = new P2PConnector();

        exitItem.addActionListener(e -> System.exit(0));


        connectItem.addActionListener(e -> {

            try {
                String localIp = NetworkUtils.getLocalIPAddress();

                if (root_fileChooser.getSelectedFile() == null) {
                    JOptionPane.showMessageDialog(this, "Please select a folder for root " );
                    return;
                }

                // Start the file download listener
                if (fileDownloadListener == null) {
                    fileDownloadListener = new FileDownloadListener(TCP_download_socket, root_fileChooser.getSelectedFile());
                    fileDownloadListenerThread = new Thread(fileDownloadListener);
                    fileDownloadListenerThread.start();
                }

                // Start listening for incoming broadcasts
                if (broadcast_listener == null) {
                    isConnected = true;
                    peerTable = new HashMap<>();
                    broadcast_listener = new BroadcastListener( broadcastPort, broadcast_Socket, peerTable, localIp,localPort);
                    listenerThread = new Thread(broadcast_listener);
                    listenerThread.start();

                    search_listener = new SearchListener( foundFilesModel, udp_Socket, localPort, root_fileChooser.getSelectedFile() , exclude_listModel );
                    searchThread = new Thread(search_listener);
                    searchThread.start();
                }
                // Start broadcasting
                connector.broadcastConnectMessage(localPort, broadcastPort);

                JOptionPane.showMessageDialog(this, "Connected to network with IP: " + localIp);
                setTitle("ip address: " + this.peerIP  + (isConnected ? " Connected " : " Not connected") );
            } catch (SocketException exep) {
                JOptionPane.showMessageDialog(this, "Failed to connect to network: " + exep.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                exep.printStackTrace();
            }


        });



        disconnectItem.addActionListener(e -> {

            connector.broadcast_disConnectMessage(localPort, broadcastPort); // Broadcast disconnect message

            if (fileDownloadListener != null) {
                fileDownloadListener.stop();
                fileDownloadListenerThread.interrupt();
                fileDownloadListener = null;
                fileDownloadListenerThread = null;
            }

            if (broadcast_listener != null) {
                isConnected = false;
                broadcast_listener.stop(); // Stop the listener thread immediately
                listenerThread.interrupt(); // Interrupt the listener thread if it's waiting
                broadcast_listener = null; // Clean up listener reference
                listenerThread = null; // Clean up listener thread reference
                peerTable = null;
                //     System.out.println("broadcast_listener stopped");

                search_listener.stop();
                searchThread.interrupt();
                search_listener = null;
                searchThread = null;
                //   System.out.println("search_listener stopped");
            }
            // System.out.println("Before message dialog, current thread interrupted: " + Thread.currentThread().isInterrupted());
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Disconnected from network");
            });
            setTitle("ip address: " + this.peerIP  + (isConnected ? " Connected " : " Not connected") );
        });

        fileMenu.add(connectItem);
        fileMenu.add(disconnectItem);
        fileMenu.add(exitItem);
        JMenu helpMenu = new JMenu("Help");
        JMenuItem helpItem = new JMenuItem("About");

        helpItem.addActionListener(e -> showHelpDialog());
        helpMenu.add(helpItem);

        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }

    private void setupRootPanel() {
        JPanel rootPanel = new JPanel();
        rootPanel.setBorder(BorderFactory.createTitledBorder("Root of the P2P shared folder"));
        rootTextArea = new JTextArea(2, 20);
        rootTextArea.setBackground(Color.WHITE);
        JButton rootSetButton = new JButton("Set");

        rootSetButton.addActionListener(e -> selectRootFolder());
        rootPanel.add(new JScrollPane(rootTextArea));
        rootPanel.add(rootSetButton);
        add(rootPanel);
    }

    private void setupDestinationPanel() {
        JPanel destinationPanel = new JPanel();
        destinationPanel.setBorder(BorderFactory.createTitledBorder("Destination Folder"));
        destinationTextArea = new JTextArea(2, 20);
        destinationTextArea.setBackground(Color.WHITE);
        JButton destinationSetButton = new JButton("Set");

        destinationSetButton.addActionListener(e -> selectDestinationFolder());
        destinationPanel.add(new JScrollPane(destinationTextArea));
        destinationPanel.add(destinationSetButton);
        add(destinationPanel);
    }

    private void setupSettingsPanel() {
        JPanel settingsPanel = new JPanel();
        settingsPanel.setBorder(BorderFactory.createTitledBorder("Settings"));
        settingsPanel.setLayout(new GridLayout(1, 2));

        JPanel folderExclude = new JPanel();
        folderExclude.setBorder(BorderFactory.createTitledBorder("Folder Exclusion"));
        folderExclude.setLayout(new BorderLayout());

        JPanel checkboxPanel = new JPanel();
        checkBox = new JCheckBox("Check new files only in the root");
        checkboxPanel.add(checkBox);
        checkboxPanel.setPreferredSize(new Dimension(25, 25));
        folderExclude.add(checkboxPanel, BorderLayout.NORTH);

        settingsPanel.add(folderExclude);
        settingsPanel.add(createExclusionPanel("Exclude these masks"));
        add(settingsPanel);
    }

    private void setupDownloadingPanel() {
        JPanel downloadingPanel = new JPanel();
        downloadingPanel.setBorder(BorderFactory.createTitledBorder("Downloading Files"));
        downloadingPanel.setLayout(new BorderLayout());

        // List for active downloads
        downloadingModel = new DefaultListModel<>();
        downloadingList = new JList<>(downloadingModel);
        JScrollPane downloadingScrollPane = new JScrollPane(downloadingList);
        downloadingPanel.add(downloadingScrollPane, BorderLayout.CENTER);

        // Download button
        JButton downloadButton = new JButton("Download Selected File");
        downloadButton.addActionListener(e -> {
            String selectedFile = foundFilesList.getSelectedValue(); // Get selected file
            if (selectedFile == null) {
                JOptionPane.showMessageDialog(this, "Please select a file to download.");
                return;
            }

            // Parse file name and peer info from the selected entry
            String[] parts = selectedFile.split("\\(from ");
            if (parts.length < 2) {
                JOptionPane.showMessageDialog(this, "Invalid file format.");
                return;
            }
            String[] file_and_size = parts[0].split(" ");
            String fileName = file_and_size[0].trim();
            String peerInfo = parts[1].replace(")", "").trim();
            String[] peerParts = peerInfo.split(":");
            String peerAddress = peerParts[0];
            int peerPort = Integer.parseInt(peerParts[1]);

            // Destination folder
            File destinationFolder = new File(destinationTextArea.getText());
            if (!destinationFolder.exists() || !destinationFolder.isDirectory()) {
                JOptionPane.showMessageDialog(this, "Please set a valid destination folder.");
                return;
            }

            // System.out.println("The file: " + fileName + " is being looked for at path: " + destinationFolder.getAbsolutePath());
            // Start downloading
            DownloadManager download_manager = new DownloadManager(fileName, peerAddress, TCP_downloadPort, destinationFolder, downloadingModel, this);
            download_manager.downloadFile();
        });

        // Add button at the bottom
        downloadingPanel.add(downloadButton, BorderLayout.SOUTH);

        add(downloadingPanel);
    }




    private void setupFoundFilesPanel() {
        JPanel foundPanel = new JPanel();
        foundPanel.setBorder(BorderFactory.createTitledBorder("Found Files"));
        foundPanel.setLayout(new BorderLayout());

        // Found files model and list
        this.foundFilesModel = new DefaultListModel<>();
        this.foundFilesList = new JList<>(foundFilesModel);
        JScrollPane foundScrollPane = new JScrollPane(foundFilesList);

        // Create a new panel for the search components
        JPanel searchPanel = new JPanel();
        searchPanel.setLayout(new FlowLayout(FlowLayout.LEFT)); // Use FlowLayout to position components next to each other

        // Add searchTextArea to the search panel
        searchTextArea = new JTextArea(2, 30);
        searchTextArea.setBackground(Color.WHITE);
        searchTextArea.setToolTipText("Search for files matching this string");
        JScrollPane searchScrollPane = new JScrollPane(searchTextArea);
        searchPanel.add(searchScrollPane);  // Add JScrollPane containing the text area

        // Add searchButton to the search panel
        JButton searchButton = new JButton("Search");
        searchButton.addActionListener(e -> {
            // Clear previous results
            foundFilesModel.clear();
            // Start listening for search results
            searchForFiles();
        });
        searchPanel.add(searchButton);  // Add searchButton next to the text area

        // Add components to the foundPanel
        foundPanel.add(foundScrollPane);  // Add found files scroll pane at the top

        add(foundPanel);
        add(searchPanel);
    }





    private JPanel createExclusionPanel(String title) {
        JPanel panel = new JPanel();
        panel.setBorder(BorderFactory.createTitledBorder(title));
        panel.setLayout(new BorderLayout()); // Use BorderLayout for better control

        exclude_listModel = new DefaultListModel<>();
        JList<String> list = new JList<>(exclude_listModel);
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setPreferredSize(new Dimension(200, 150)); // Set preferred size for the list
        panel.add(scrollPane, BorderLayout.CENTER); // Add the list to the center

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5)); // Use FlowLayout for buttons
        JButton addButton = new JButton("Add");
        JButton delButton = new JButton("Del");

        // Add action listeners for buttons
        addButton.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(GUI.this, "Enter input:");
            if (input != null) {
                exclude_listModel.addElement(input);
            }
        });

        delButton.addActionListener(e -> {
            int selectedIndex = list.getSelectedIndex();
            if (selectedIndex != -1) {
                exclude_listModel.remove(selectedIndex);
            }
        });

        // Add buttons to the button panel
        buttonPanel.add(addButton);
        buttonPanel.add(delButton);

        // Add button panel to the bottom of the main panel
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }


    private void connectToNetwork() {
        try {
            String localIp = NetworkUtils.getLocalIPAddress();
            JOptionPane.showMessageDialog(this, "Connected to network with IP: " + localIp);
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to network: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void disconnectFromNetwork() {
        // Simulate disconnecting from the network
        JOptionPane.showMessageDialog(this, "Disconnected from the network.");
    }

    private void selectRootFolder() {
        int returnVal = root_fileChooser.showOpenDialog(GUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            rootTextArea.setText(root_fileChooser.getSelectedFile().getAbsolutePath());
        }
        if (search_listener != null) {
            search_listener.sharedFolder = root_fileChooser.getSelectedFile();
        }
    }

    private void selectDestinationFolder() {
        int returnVal = download_fileChooser.showOpenDialog(GUI.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            destinationTextArea.setText(download_fileChooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void searchForFiles() {


        if (broadcast_listener != null) {
            peerTable = broadcast_listener.getPeerTable();
            if (peerTable.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No other peer is connected to the network.");
                System.out.println("PeerTable is Empty");
                return;
            }
            String searchFileName = searchTextArea.getText().trim();
            if (searchFileName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a search query.");
                return;
            }


            // Iterate through all peers in the peerTable and send the search query
            for (String peerIP : peerTable.keySet()) {
                int peerPort = peerTable.get(peerIP);

                // Validate the port
                if (peerPort < 0 || peerPort > 65535) {
                    System.out.println("Skipping peer with invalid port: " + peerIP + " - " + peerPort);
                    continue;
                }

                // Use the SearchRequester class to send the query
                SearchRequester requester = new SearchRequester(udp_Socket, peerIP, peerPort);
                requester.sendSearchRequest(searchFileName);

            }
        }
        else {
            JOptionPane.showMessageDialog(this, "Please connect first.");
            return;
        }


    }





    private void showHelpDialog() {
        JFrame helpFrame = new JFrame("Help");
        JTextArea helpText = new JTextArea("\n\n\n\n                    "
                + "Ali Abbasi Dolatabadi\n                         "
                + " 20210702109");
        helpText.setEditable(false);
        helpFrame.add(helpText);
        helpFrame.setSize(300, 200);
        helpFrame.setLocationRelativeTo(GUI.this);
        helpFrame.setVisible(true);
    }


}
