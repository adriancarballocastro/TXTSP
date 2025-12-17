import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class Server {
    public static void main(String[] args) throws IOException {
        //Abrir puerto
        ServerSocket serverSocket = new ServerSocket(2525);
        System.out.println("Servidor TXTSP escuchando en puerto 2525...");

        //Recibir todos las peticiones de los clientes
        while (true) {
            Socket socket = serverSocket.accept();
            new Thread(() -> handleClient(socket)).start();
        }
    }

    //Procesar cada peticion de forma individual.
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

                // Calcular SHA-256 del archivo
                String checksum = calculateSHA256(file);

                // Calcular número total de chunks
                long fileSize = file.length();
                int chunkSize = 1024;
                int totalChunks = (int) ((fileSize + chunkSize - 1) / chunkSize);

                // Enviar INFO con metadatos
                out.write("INFO: size=" + fileSize + "; chunks=" + totalChunks + "; sha256=" + checksum + "\n");
                out.flush();

                // Enviar el archivo en chunks
                try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
                    byte[] buffer = new byte[chunkSize];
                    int read;
                    while ((read = fis.read(buffer)) != -1) {
                        byte[] actualData = Arrays.copyOf(buffer, read); // Evitar bytes basura en el último chunk
                        String chunk = Base64.getEncoder().encodeToString(actualData);
                        out.write("CHUNK:" + chunk + "\n");
                        out.flush();
                    }
                }
                out.write("END\n");
                out.flush();
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static String calculateSHA256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (BufferedInputStream fis = new BufferedInputStream(new FileInputStream(file))) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
