import java.io.IOException;

public class Main {
    public static void main(String[] args) throws InterruptedException, IOException {
        if(args.length !=1){
            System.out.println("Wrong input");
            return;
        }
        String snakeName = args[0];
        (new GameManager(snakeName)).run();
    }
}