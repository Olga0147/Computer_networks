package ru.nsu.ccfit.zenina.Lab3;


import java.net.InetAddress;
import java.net.UnknownHostException;

public class Main {
    private static final int SIMPLE_ARGS_SIZE = 3;
    private static final int ROOT_ARGS_SIZE = 5;

    public static void main(String[] args) throws UnknownHostException {
        TreeNode treeNode = null;
        if (args.length >= SIMPLE_ARGS_SIZE) {
            String nodeName = args[0];
            int port = Integer.parseInt(args[1]);
            int percentageLoss = Integer.parseInt(args[2]);
            String address = InetAddress.getLocalHost().getHostAddress();
            if (args.length == ROOT_ARGS_SIZE) {
                String parentAddress = args[3];
                int parentPort = Integer.parseInt(args[4]);
                treeNode = new TreeNode(nodeName, percentageLoss, port, address, parentAddress, parentPort);
            } else if (args.length == SIMPLE_ARGS_SIZE) {
                treeNode = new TreeNode(nodeName, percentageLoss, address, port);
            } else {
                System.out.println("Invalid arguments size");
                System.exit(1);
            }
        } else {
            System.out.println("Invalid arguments size");
            System.exit(1);
        }

        treeNode.start();
    }
}
