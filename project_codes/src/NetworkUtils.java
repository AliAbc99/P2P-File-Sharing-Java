import java.net.*;

public class NetworkUtils {

    // Get local IP address
    public static String getLocalIPAddress() throws SocketException {
        InetAddress localHost = null;
		try {
			localHost = InetAddress.getLocalHost();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        return localHost.getHostAddress();
    }

    // Get the broadcast address
    public static String getBroadcastAddress(String subnet) {
        try {
            String[] subnetParts = subnet.split("\\.");
            int[] subnetInt = new int[subnetParts.length];
            for (int i = 0; i < subnetParts.length; i++) {
                subnetInt[i] = Integer.parseInt(subnetParts[i]);
            }

            // Calculate the broadcast address (assuming a typical subnet mask of 255.255.255.0)
            subnetInt[3] = 255;

            return subnetInt[0] + "." + subnetInt[1] + "." + subnetInt[2] + "." + subnetInt[3];
        } catch (Exception e) {
            return "255.255.255.255";  // Default broadcast address
        }
    }

    // Get peer's IP address based on socket or connection
    public static String getPeerIPAddress(Socket socket) {
        return socket.getInetAddress().getHostAddress();
    }
}
