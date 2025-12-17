import java.io.*;
import java.net.Socket;
import java.util.Base64;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class Client {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        String filename = "example.txt";

        try (Socket socket = new Socket("localhost", 2525);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

            out.write("REQUEST:" + filename + "\n");
            out.flush();

            String line;
            long fileSize = 0;
            int totalChunks = 0;
            String expectedHash = null;
            int receivedChunks = 0;

            MessageDigest digest = MessageDigest.getInstance("SHA-256");

            try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("downloaded_" + filename))) {
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("INFO:")) {
                        Map<String, String> info = parseInfo(line.substring(5));
                        fileSize = Long.parseLong(info.get("size"));
                        totalChunks = Integer.parseInt(info.get("chunks"));
                        expectedHash = info.get("sha256");
                        System.out.println("Tamaño del archivo: " + fileSize + " bytes, Total de chunks: " + totalChunks);
                    } else if (line.startsWith("CHUNK:")) {
                        byte[] data = Base64.getDecoder().decode(line.substring(6));
                        fos.write(data);
                        digest.update(data); // Calcular hash mientras se escribe
                        receivedChunks++;
                        System.out.println("Recibido chunk " + receivedChunks + " de " + totalChunks);
                    } else if (line.equals("END")) {
                        fos.flush();
                        System.out.println("Archivo recibido!");

                        // Verificar hash
                        byte[] hash = digest.digest();
                        StringBuilder sb = new StringBuilder();
                        for (byte b : hash) {
                            sb.append(String.format("%02x", b));
                        }
                        String calculatedHash = sb.toString();
                        if (calculatedHash.equalsIgnoreCase(expectedHash)) {
                            System.out.println("Checksum verificado correctamente: " + calculatedHash);
                        } else {
                            System.out.println("ERROR: Checksum no coincide!");
                            System.out.println("Esperado: " + expectedHash);
                            System.out.println("Calculado: " + calculatedHash);
                        }
                        break;
                    } else if (line.startsWith("ERROR:")) {
                        System.out.println(line);
                        break;
                    }
                }
            }
        }
    }

    // Función para parsear INFO: size=123; chunks=10; sha256=abcd...
    private static Map<String, String> parseInfo(String infoLine) {
        Map<String, String> info = new HashMap<>();
        String[] parts = infoLine.split(";");
        for (String part : parts) {
            String[] keyValue = part.trim().split("=");
            if (keyValue.length == 2) {
                info.put(keyValue[0].trim(), keyValue[1].trim());
            }
        }
        return info;
    }
}
