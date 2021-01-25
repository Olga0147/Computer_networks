package ru.nsu.ccfit.zenina.Lab2;

import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

class ClientParam{
    private long startTime;
    private long previouslyTime;
    private long previouslyRead;

    ClientParam(long a, long b,long c){
        this.startTime = a;
        this.previouslyTime = b;
        this.previouslyRead = c;
    }

    long getPreviouslyTime() {
        return previouslyTime;
    }

    void setPreviouslyTime(long previouslyTime) {
        this.previouslyTime = previouslyTime;
    }


    long getPreviouslyRead() {
        return previouslyRead;
    }

    void setPreviouslyRead(long previouslyRead) {
        this.previouslyRead = previouslyRead;
    }

    long getStartTime() {
        return startTime;
    }
}

public class TimeCalc extends TimerTask {
    private ConcurrentHashMap<ClientInServer, ClientParam> Clients;

    TimeCalc() {Clients = new ConcurrentHashMap<>();}

    void addClient(ClientInServer cl){
        long t = System.currentTimeMillis();
        Clients.put(cl,new ClientParam(t,t,0));
    }

    void deleteClient(ClientInServer cl){
        printClientTime(cl,Clients.get(cl));
        Clients.remove(cl);
    }

    @Override
    public void run() {
        Clients.forEach(1,
                this::printClientTime
        );
    }

    private void printClientTime(ClientInServer k,ClientParam param){
        long totalRead = k.getTotalRead();
        int socketPort = k.getSocketPort();

        if(0 != totalRead){
            long currentTime = System.currentTimeMillis();
            double instantSpeed = (1.0 * totalRead - param.getPreviouslyRead()) / (currentTime - param.getPreviouslyTime());
            if(instantSpeed == Double.POSITIVE_INFINITY){
                System.out.println("("+socketPort+") Instant speed: " + instantSpeed + " bytes read:"+totalRead);
            }
            else {
                System.out.println("(" + socketPort + ") Instant speed: " + instantSpeed);
            }
            double averageSpeed = 1.0 * totalRead / (currentTime - param.getStartTime());

            System.out.println("("+socketPort+") Average speed: " + averageSpeed);
            param.setPreviouslyRead(totalRead);
            param.setPreviouslyTime(currentTime);

        }
    }


}
