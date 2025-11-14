import javax.swing.*;
import java.io.File;
import java.net.*;

public class SearchListener implements Runnable {

    private final int listenPort; // Port to listen on
    public File sharedFolder; // Shared folder to search files
    DatagramSocket listen_socket;
    DefaultListModel<String> foundFilesModel;
    DefaultListModel<String> exclude_listModel;
    private volatile boolean running = true; // Control for stopping the thread

    public SearchListener(DefaultListModel<String> foundFilesModel,DatagramSocket listen_socket, int listenPort, File sharedFolder, DefaultListModel<String> exclude_listModel) {
        this.listenPort = listenPort;
        this.sharedFolder = sharedFolder;
        this.listen_socket = listen_socket;
        this.foundFilesModel = foundFilesModel;
        this.exclude_listModel = exclude_listModel;
    }

    @Override
    public void run() {

        try {
            listen_socket.setSoTimeout(500); // Timeout after 500ms to periodically check for interrupt
        } catch (SocketException e) {
            e.printStackTrace();
            return;
        }

      //  System.out.println("SearchListener started on port: " + listenPort);
        byte[] buffer = new byte[1024];



        while (running) {

            try {

                // Receive incoming search query
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                listen_socket.receive(packet);

                String received = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Received: " + received);

                // Process only SEARCH queries
                if (received.startsWith("SEARCH:")) {
                    String query = received.substring(7).trim(); // Extract the query
                    System.out.println("Searching for: " + query);

                    // Search for matching files and their sizes
                    String results = "RESULT:";
                    results = results + searchFilesWithSizes(sharedFolder, query);

                    // Send results back to the requester
                    InetAddress requesterAddress = packet.getAddress();
                    int requesterPort = packet.getPort();
                    DatagramPacket responsePacket = new DatagramPacket(
                            results.getBytes(), results.length(), requesterAddress, requesterPort);
                    listen_socket.send(responsePacket);

                    System.out.println("Sent results to " + requesterAddress + ":" + requesterPort);
                } else if (received.startsWith("RESULT:")) {
                    String results = received.substring(7).trim(); // Extract results without "RESULT:"
                    System.out.println("Processed Results: \n" + results);

                    // Extract peer address and port from the packet
                    String peerAddress = packet.getAddress().getHostAddress();
                    int peerPort = packet.getPort();
                    String peerInfo = peerAddress + ":" + peerPort;

                    // Split results into lines and add each file to the model
                    String[] files = results.split("\n"); // Each file is on a new line
                    SwingUtilities.invokeLater(() -> {
                        for (String file : files) {
                            if (!file.isEmpty()) {
                                // Extract file name by removing the part after '('
                                String fileName = file.split("\\s*\\(\\s*")[0].trim();

                                // Check if the file name is excluded
                                if (!isExcluded(fileName, exclude_listModel)) {
                                    String fileEntry = file + " (from " + peerInfo + ")";
                                    foundFilesModel.addElement(fileEntry);
                                }
                            }
                        }
                    });
                }
            }
            catch (SocketTimeoutException e) {
                // Timeout occurred, check if thread should stop
                if (Thread.interrupted()) {
                    break; // Exit the loop if the thread is interrupted
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Method to search for files and their sizes in the shared folder
    private String searchFilesWithSizes(File directory, String query) {
        if (!directory.isDirectory()) {
            return "Shared folder is not a directory.";
        }

        StringBuilder results = new StringBuilder();
        File[] files = directory.listFiles();

        if (files != null) {
            for (File file : files) {
                // Check if the file name contains the query (case-insensitive)
                if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    long fileSizeInKB = file.length() / 1024; // Convert size to KB
                    results.append(file.getName()).append(" (").append(fileSizeInKB).append(" KB)").append("\n");
                }
            }
        }
        System.out.println("Result for search is \n " + results.toString());
        return !results.isEmpty() ? results.toString() : "No matching files found.";
    }

    /**
     * Checks if a file name matches any exclusion pattern in the exclude_listModel.
     *
     * @param fileName          The file name to check.
     * @param exclude_listModel The list of exclusion patterns.
     * @return True if the file matches an exclusion pattern; false otherwise.
     */
    private boolean isExcluded(String fileName, DefaultListModel<String> exclude_listModel) {
        for (int i = 0; i < exclude_listModel.size(); i++) {
            String pattern = exclude_listModel.get(i);
            if (fileName.matches(convertToRegex(pattern))) { // Match against the regex
                return true; // File matches an exclusion pattern
            }
        }
        return false;
    }

    /**
     * Converts a file mask pattern to a regex pattern.
     * For example, "*.txt" -> ".*\\.txt".
     *
     * @param mask The file mask to convert.
     * @return A regex pattern equivalent to the mask.
     */
    private String convertToRegex(String mask) {
        return mask
                .replace(".", "\\.") // Escape dots
                .replace("*", ".*")  // Convert '*' to '.*'
                .replace("?", ".");  // Convert '?' to '.'
    }

    public void stop() {
        running = false; // Stop the thread
        Thread.currentThread().interrupt(); // Interrupt the thread to stop socket.receive()
    }
}

