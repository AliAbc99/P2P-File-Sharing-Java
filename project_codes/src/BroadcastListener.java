import java.io.IOException;
import java.net.*;
import java.util.HashMap;

public class BroadcastListener implements Runnable {
    private DatagramSocket broadcastSocket;
    public HashMap<String, Integer> peerTable; // To store peer address and port
    private volatile boolean running = true; // Control for stopping the thread
    private final String localIp;
    private final int localPort;
    public int broadcastPort;

    public HashMap<String, Integer> getPeerTable() {
        return peerTable;
    }

    public BroadcastListener(int broadcastPort, DatagramSocket broadcastSocket , HashMap<String, Integer> peerTable, String localIp, int localPort) throws SocketException {
        this.broadcastPort = broadcastPort;
        this.broadcastSocket = broadcastSocket;
        this.peerTable = peerTable;
        this.localIp = localIp;
        this.localPort = localPort;

    }

    public void responseConnect(int localPort, String ResponseIPAdress ) throws IOException {
        // Message to broadcast: "CONNECT:<port>"

       try ( DatagramSocket socket = new DatagramSocket()){
           String message = "CONNECT:" + localPort;
           byte[] buffer = message.getBytes();

           // Broadcast address
           InetAddress broadcastAddress = InetAddress.getByName(ResponseIPAdress);
           // Create and send the packet
           DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, broadcastPort);
           System.out.println("send response back to :" + broadcastAddress + " with port: " + broadcastPort);
           socket.send(packet);
           System.out.println("Broadcast message sent: " + message);
       }
       catch (Exception e){
           e.printStackTrace();
       }

    }

    @Override
    public void run() {


        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        // Set a timeout to periodically wake up and check if the thread is interrupted
        try {
            broadcastSocket.setSoTimeout(500); // Timeout after 500ms to periodically check for interrupt
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }

        while (running) {
            try {
                // Wait for a broadcast message, but with a timeout
                broadcastSocket.receive(packet);

                // Extract message, sender address, and port
                String message = new String(packet.getData(), 0, packet.getLength());
                if (message.startsWith("CONNECT:")) {
                    int peerPort = Integer.parseInt(message.split(":")[1]);
                    String peerAddress = packet.getAddress().getHostAddress();
                    if (peerAddress.equals(localIp)) {
                        continue;
                    }


                    // Add peer to the table
                    if (!peerTable.containsKey(peerAddress) ) {
                        responseConnect(localPort, peerAddress);
                        peerTable.put(peerAddress, peerPort);
                        System.out.println("current peers: " + peerTable);
                        System.out.println("New peer added: " + peerAddress + ":" + peerPort);
                    }
                   /* else{
                        System.out.println("peer: " + peerAddress +  " already exist");
                    } */
                } else if (message.startsWith("DISCONNECT:")) {
                    int peerPort = Integer.parseInt(message.split(":")[1]);
                    String peerAddress = packet.getAddress().getHostAddress();
                    if (peerAddress.equals(localIp)) {
                        continue;
                    }

                    // Delete peer from the table
                    if (peerTable.containsKey(peerAddress)) {
                        peerTable.remove(peerAddress, peerPort);
                        System.out.println("current peers: " + peerTable);
                        System.out.println("Peer deleted: " + peerAddress + ":" + peerPort);
                    }
                }
            } catch (SocketTimeoutException e) {
                // Handle the timeout: no message received, check for interruption
                if (Thread.interrupted()) {
                    break; // Exit the loop if the thread is interrupted
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }



    }

    public void stop() {
        running = false; // Stop the thread
        Thread.currentThread().interrupt(); // Interrupt the thread to stop socket.receive()
    }
}
