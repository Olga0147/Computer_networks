package ru.nsu.ccfit.zenina.Lab1;

public class Main {
    public static void main(String[] args) {
        MulticastUDP mu = new MulticastUDP(args[0]);
        mu.work();
    }
}
