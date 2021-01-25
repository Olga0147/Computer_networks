package ru.nsu.ccfit.zenina.Lab2;


import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class Client {
    private static final int BUFFER_SIZE = 1024;
    private static final int RESPONSE_SIZE = 7;
    private static String filePath;
    private static String serverName;
    private static int serverPort;
    private DataOutputStream dataOutputStream;

    public static void main(String[] args) {
        filePath = args[0];
        serverName = args[1];
        serverPort = Integer.parseInt(args[2]);
        Client client = new Client();
        client.start();
    }

    private void start() {
        try {
            Socket socket = new Socket(serverName, serverPort);
            System.out.println("Local port: "+socket.getLocalPort());

            dataOutputStream = new DataOutputStream(socket.getOutputStream());
            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());

            if(sendFile()){
                System.exit(1);
            }

            byte[] response = new byte[RESPONSE_SIZE];
            dataInputStream.readFully(response);
            String message = new String(response, StandardCharsets.UTF_8);
            System.out.println(message);

            try {
                socket.close();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } catch (Exception e) {
            System.out.println("Error in connection to server: " + e.getMessage());
            System.exit(1);
        }
    }

    private boolean sendFile() throws IOException {
        String fileName = filePath;
        File file = new File(fileName);
        if(!file.exists()){
            System.out.println("File "+ fileName+ " does not exist");
            return true;
        }
        long fileSize = file.length();
        System.out.println("Build file: " + file.getName());
        dataOutputStream.writeLong(fileSize);
        byte[] bytes = fileName.getBytes(StandardCharsets.UTF_8);
        dataOutputStream.writeInt(bytes.length);
        dataOutputStream.write(bytes);
        FileInputStream fileInputStream = new FileInputStream(file);
        int read;
        byte[] buffer = new byte[BUFFER_SIZE];
        while ((read = fileInputStream.read(buffer, 0, buffer.length)) > 0) {
            dataOutputStream.write(buffer, 0, read);
            dataOutputStream.flush();
        }
        System.out.println("Send  file: "+file.getName());
        return false;
    }
}