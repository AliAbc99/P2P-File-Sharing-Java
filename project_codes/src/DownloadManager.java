import javax.swing.*;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public class DownloadManager {
    private String fileName;
    private String peerAddress;
    private int peerPort;
    private File destinationFolder;
    private DefaultListModel<String> downloadingModel;
    GUI application_gui;

    public DownloadManager(String fileName, String peerAddress, int peerPort, File destinationFolder, DefaultListModel<String> downloadingModel, GUI application_gui) {
        this.fileName = fileName;
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.destinationFolder = destinationFolder;
        this.downloadingModel = downloadingModel;
        this.application_gui = application_gui;
    }

    public void downloadFile() {
        new Thread(() -> {
            try (Socket socket = new Socket(peerAddress, peerPort);
                 DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                 DataInputStream dis = new DataInputStream(socket.getInputStream())) {

                // Send file request to the peer
                dos.writeUTF("DOWNLOAD:" + fileName);

                // Read response
                String response = dis.readUTF();
                if (response.equals("NOT_FOUND")) {
                    SwingUtilities.invokeLater(() -> downloadingModel.addElement(fileName + " - File not found on peer."));
                    return;
                }

                // Prepare to receive file
                long fileSize = dis.readLong();
                File outputFile = new File(destinationFolder, fileName);
                try (FileOutputStream fos = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[256 * 1024]; // 256KB buffer
                    long totalRead = 0;
                    int bytesRead;

                    while ((bytesRead = dis.read(buffer)) > 0) {
                        fos.write(buffer, 0, bytesRead);
                        fos.flush(); // Ensure the data is fully written
                        totalRead += bytesRead;


                        // Update progress every 10% instead of each byte read
                        int progress = (int) ((totalRead * 100) / fileSize);
                        if (progress % 10 == 0) {
                            SwingUtilities.invokeLater(() -> downloadingModel.addElement(fileName + " - " + progress + "% completed"));
                        }
                    }
                    // Flush and close the file output stream after download is complete
                    fos.flush();
                    SwingUtilities.invokeLater(() -> downloadingModel.addElement(fileName + " - Download completed."));

                } catch (IOException e) {
                    SwingUtilities.invokeLater(() -> downloadingModel.addElement(fileName + " - Download failed: " + e.getMessage()));
                    return; // Return early if download fails
                }

                FileHasher hasher = null;
                hasher = new FileHasher();

                Map<String, List<String>> fileHashes = hasher.calculateHashes(this.destinationFolder);

                boolean isDuplicate = hasher.isFileDuplicate(fileHashes, outputFile);

                if (isDuplicate) {
                    List<String> duplicate_files_list = hasher.giveDuplicateFileNames(fileHashes, outputFile);

                    // Format the list of duplicate file names into a single string
                    String duplicateFilesMessage = String.join("\n", duplicate_files_list);
                    //  System.out.println("The file has a duplicate in destination folder. duplicate files: " + duplicateFilesMessage);
                    // Display the message in a JOptionPane
                    JOptionPane.showMessageDialog(
                            this.application_gui,
                            "The file has duplicates in destination folder:\n" + duplicateFilesMessage,
                            "Duplicate Files Found",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
                else {
                    JOptionPane.showMessageDialog(this.application_gui, "The file doesn't have a duplicate in destination folder.");
                    //    System.out.println("The file doesn't have a duplicate in destination folder.");
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> downloadingModel.addElement(fileName + " - Download failed: " + e.getMessage()));
            }
        }).start();
    }
}
