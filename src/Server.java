import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;

public class Server {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(2525);
        System.out.println("Servidor TXTSP escuchando en puerto 2525...");

        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    private static void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            String line = in.readLine();
            if (line != null && line.startsWith("REQUEST:")) {
                String filename = line.substring(8).trim();
                File file = new File("files/" + filename);
                if (!file.exists()) {
                    out.write("ERROR: Archivo no encontrado\n");
                    out.flush();
                    return;
                }

                out.write("INFO:" + file.length() + "\n");
                out.flush();

                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        String chunk = Base64.getEncoder().encodeToString(buffer);
                        out.write("CHUNK:" + chunk + "\n");
                        out.flush();
                    }
                }
                out.write("END\n");
                out.flush();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
