import java.io.*;
import java.net.*;

public class FileDownloadListener implements Runnable {
    private final File sharedFolder;
    private volatile boolean running = true; // Control flag for stopping the listener
    private final ServerSocket serverSocket;

    public FileDownloadListener(ServerSocket serverSocket, File sharedFolder) {
        this.serverSocket = serverSocket;
        this.sharedFolder = sharedFolder;
    }

    @Override
    public void run() {


        //System.out.println("FileDownloadListener started on port " + serverSocket.getLocalPort());

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                handleClientRequest(clientSocket);
            } catch (IOException e) {
                if (running) {
                    System.err.println("Error handling client request: " + e.getMessage());
                }
            }
        }

    }

    private void handleClientRequest(Socket clientSocket) {
        try (DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
             DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream())) {

            String request = dis.readUTF();
            if (request.startsWith("DOWNLOAD:")) {
                String requestedFile = request.substring(9).trim(); // Extract the file name
              //  System.out.println("requestedFile: " + requestedFile + " looking for it in: " + sharedFolder);
                File fileToSend = new File(sharedFolder, requestedFile);

                if (!fileToSend.exists() || !fileToSend.isFile()) {
                    dos.writeUTF("NOT_FOUND");
                    System.out.println("File not found: " + requestedFile);
                    return;
                }

                dos.writeUTF("OK");
                dos.writeLong(fileToSend.length()); // Send file size

                // Send file in 256KB chunks
                try (FileInputStream fis = new FileInputStream(fileToSend)) {
                    byte[] buffer = new byte[256 * 1024];
                    int bytesRead;

                    while ((bytesRead = fis.read(buffer)) > 0) {
                        dos.write(buffer, 0, bytesRead);
                    }

                    System.out.println("File sent: " + requestedFile );
                }
            } else {
                System.out.println("Invalid request: " + request);
            }
        } catch (IOException e) {
            System.err.println("Error handling client request: " + e.getMessage());
        }
    }

    public void stop() {
        running = false; // Set the flag to stop the loop
       // closeServerSocket();
    }

    private void closeServerSocket() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }
}
