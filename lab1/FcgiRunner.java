import com.fastcgi.FCGIInterface;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

public class FcgiRunner {
    public static void main(String[] args) throws Exception {
        ServerSocket socket = new ServerSocket();
        socket.bind(new InetSocketAddress("127.0.0.1", 39999));
        Field srvSocketField = FCGIInterface.class.getDeclaredField("srvSocket");
        srvSocketField.setAccessible(true);
        srvSocketField.set(null, socket);
        FCGIInterface fcgi = new FCGIInterface();
        Field acceptCalled = FCGIInterface.class.getDeclaredField("acceptCalled");
        acceptCalled.setAccessible(true);
        acceptCalled.setBoolean(null, true);
        System.out.println("Ready");
        int rc = fcgi.FCGIaccept();
        System.out.println("RC=" + rc);
    }
}
