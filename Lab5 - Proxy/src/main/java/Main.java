//https://ru.wikipedia.org/wiki/SOCKS
public class Main {
    public static void main(String[] args) {

        int port= Integer.parseInt(args[0]);
        if(port<0 || port>65536){
            System.out.println("Error: wrong port");
            return;
        }
//port=12345;
        SocksProxy socksProxy = new SocksProxy(port);
        socksProxy.run();
    }
}
