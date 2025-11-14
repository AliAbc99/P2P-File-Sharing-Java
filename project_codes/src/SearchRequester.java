import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class SearchRequester {

    private final String peerIP; // IP address of the target peer
    private final int peerPort; // Port of the target peer
    DatagramSocket sender_socket;

    public SearchRequester(DatagramSocket sender_socket , String peerIP, int peerPort) {
        this.peerIP = peerIP;
        this.peerPort = peerPort;
        this.sender_socket = sender_socket;
    }

    // Method to send a search request
    public void sendSearchRequest(String query) {
        try  {
            String message = "SEARCH:" + query; // Format the search query
            byte[] buffer = message.getBytes();

            InetAddress peerAddress = InetAddress.getByName(peerIP); // Resolve the peer's IP address
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, peerAddress, peerPort);

            sender_socket.send(packet); // Send the UDP packet
            System.out.println("Search query \"" + query + "\" sent to peer " + peerIP + ":" + peerPort);
        } catch (Exception e) {
            System.err.println("Error sending search request to " + peerIP + ":" + peerPort);
            e.printStackTrace();
        }
    }
}
