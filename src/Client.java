void main() throws IOException {
    String filename = "example.txt";

    try (Socket socket = new Socket("localhost", 2525);
         BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
         BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {

        out.write("REQUEST:" + filename + "\n");
        out.flush();

        String line;
        try (BufferedOutputStream fos = new BufferedOutputStream(new FileOutputStream("downloaded_" + filename))) {
            while ((line = in.readLine()) != null) {
                if (line.startsWith("INFO:")) {
                    IO.println("Tamaño del archivo: " + line.substring(5));
                } else if (line.startsWith("CHUNK:")) {
                    byte[] data = Base64.getDecoder().decode(line.substring(6));
                    fos.write(data);
                } else if (line.equals("END")) {
                    IO.println("Archivo recibido!");
                    break;
                } else if (line.startsWith("ERROR:")) {
                    IO.println(line);
                    break;
                }
            }
        }
    }
}
