import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class P2PConnector {
    //private final int broadcastPort = 8888; // Port for broadcasting

    public void broadcastConnectMessage(int localPort, int broadcastPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true); // Enable broadcast

            // Message to broadcast: "CONNECT:<port>"
            String message = "CONNECT:" + localPort;
            byte[] buffer = message.getBytes();

            // Broadcast address
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            // Create and send the packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, broadcastPort);
            socket.send(packet);

            System.out.println("Broadcast message sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void broadcast_disConnectMessage(int localPort, int broadcastPort) {
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setBroadcast(true); // Enable broadcast

            // Message to broadcast: "CONNECT:<port>"
            String message = "DISCONNECT:" + localPort;
            byte[] buffer = message.getBytes();

            // Broadcast address
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            // Create and send the packet
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, broadcastPort);
            socket.send(packet);

            System.out.println("Broadcast message sent: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
