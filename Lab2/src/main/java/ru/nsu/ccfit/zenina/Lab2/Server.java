package ru.nsu.ccfit.zenina.Lab2;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

public class Server {
    static final String DIR_NAME = "./uploads";
    private static final int PERIOD = 3000;
    private static int serverPort;
    private Timer timer;

    public static void main(String[] args) {
        serverPort = Integer.parseInt(args[0]);
        Server server = new Server();
        server.start();
    }
    private Server(){
        File uploadsDirectory = new File(DIR_NAME);
        if (!uploadsDirectory.exists()) {
            boolean mkdir = uploadsDirectory.mkdir();
            if (!mkdir) {
                System.exit(1);
            }
        }
        timer = new Timer();
    }
    private void start() {
        ServerSocket serverSocket;
        try {
            serverSocket = new ServerSocket(serverPort);

            Timer timerMain = new Timer();
            TimeCalc TC = new TimeCalc();
            timerMain.scheduleAtFixedRate(TC, 0, Server.PERIOD);

            while (!Thread.interrupted()) {
                Socket socket;
                try {
                    socket = serverSocket.accept();
                   // System.out.println("New connection: " + socket.getInetAddress().getHostAddress());
                    System.out.println("-----------------------");
                    System.out.println("New connection: " + socket.getPort());
                    Thread client = new Thread(new ClientInServer(socket, TC,timer));
                    client.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}

//----------------------------------------------------------------------------------------------------------------------
class ClientInServer implements Runnable {
    private static final int BUF_SIZE = 1024;
    private AtomicLong totalRead;
    private Socket socket;
    private TimeCalc TC;
    private Timer timer;

    ClientInServer(Socket socket, TimeCalc tm, Timer timer) {
        this.TC = tm;
        this.socket = socket;
        this.totalRead = new AtomicLong();
        this.timer = timer;
    }

    @Override
    public void run() {

        try (DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
             DataOutputStream dataOutputStream = new DataOutputStream(socket.getOutputStream())) {

            long fileSize = dataInputStream.readLong();
            int fileNameSize = dataInputStream.readInt();

            int read;
            byte[] stringBuffer = new byte[fileNameSize];
            int totalFileNameRead = 0;
            while ((totalFileNameRead < fileNameSize) && (((read = dataInputStream.read(stringBuffer, totalFileNameRead, fileNameSize - totalFileNameRead)) != -1))) {
                totalFileNameRead += read;
            }
            String fileName = new String(stringBuffer, StandardCharsets.UTF_8);

            String name = (new File(fileName)).getName();
            File tempFile = (new File(Server.DIR_NAME + "/" + name));

            File file;
            int i = 0;
            if (tempFile.exists()) {

                while (new File(Server.DIR_NAME + "/" + i + name).exists()) {
                    i++;
                }
                name = i + name;
                file = new File(Server.DIR_NAME + "/" + name);
            } else {
                file = tempFile;
            }

            System.out.println("(" + socket.getPort() + ") New name of file: " + name);

            TCa t = new TCa(this);
            timer.scheduleAtFixedRate(t, 2, 3);

           // TC.addClient(this);

            try (OutputStream outputStream = new FileOutputStream(file)) {
                byte[] buffer = new byte[BUF_SIZE];
                while ((totalRead.longValue() < fileSize) && ((read = dataInputStream.read(buffer, 0, buffer.length)) != -1)) {
                    outputStream.write(buffer, 0, read);
                    totalRead.getAndAdd(read);
                }
                outputStream.flush();


                if (fileSize == totalRead.longValue()) {
                    dataOutputStream.writeBytes("Success");
                    System.out.println("(" + socket.getPort() + ") Success end");
                } else {

                    dataOutputStream.writeBytes("Error");
                    System.out.println("(" + socket.getPort() + ") Error: incorrect saving file");
                }

            } catch (IOException e) {
                System.out.println(e.getMessage());

            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    long getTotalRead() {
        return totalRead.get();
    }

    int getSocketPort() {
        return this.socket.getPort();
    }

    static class TCa extends TimerTask {
        private long startTime;
        private long previousTime;
        private long previouslyRead;
        private ClientInServer cl;

        TCa(ClientInServer cl) {
            this.startTime = System.currentTimeMillis();
            this.cl = cl;
            this.previousTime = this.startTime;
        }

        @Override
        public void run() {
           printClient();
        }

        void printClient(){
            int socketPort = cl.getSocketPort();
            long totalRead = cl.getTotalRead();
            if (totalRead != 0) {
                long currentTime = System.currentTimeMillis();
                double instantSpeed = (1.0 * totalRead - previouslyRead) / (currentTime - previousTime);
                if(instantSpeed == Double.POSITIVE_INFINITY){
                    System.out.println("("+socketPort+") Instant speed: " + instantSpeed + " bytes read:"+totalRead);
                }
                double averageSpeed = 1.0 * totalRead / (currentTime - startTime);
                System.out.println("("+socketPort+") Average speed: " + averageSpeed);
                previouslyRead = totalRead;
                previousTime = currentTime;
            }
        }
    }
}

