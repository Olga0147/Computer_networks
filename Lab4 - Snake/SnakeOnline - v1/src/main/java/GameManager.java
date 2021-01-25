import com.google.protobuf.InvalidProtocolBufferException;
import me.ippolitov.fit.snakes.SnakesProto;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class GameManager {
    // Node role
    private static int NORMAL = 0;
    private static int MASTER = 1;
    private static int DEPUTY = 2;
    private static int VIEWER = 3;
    private int nodeRole;
    // player type
    public static int HUMAN = 0;
    public static int ROBOT = 1;

    private int myId = 0;
    private String myName;

    Player deputy = null;
    Integer deputyId = null;

    private String masterIp;
    private Integer masterPort;
    private Integer masterId;

    private String oldMasterIp = null;
    private Integer oldMasterId = null;

    class Player {
        public String Name;
        public String ipAddress = "";
        public int port = 0;
        public int role;
        public int type = HUMAN;
        public int score = 0;
        public Player(String Name, int role, int port, String ipAddress) {
            this.Name = Name;
            this.role = role;
            this.port = port;
            this.ipAddress = ipAddress;
        }

        public void add1Score() {
            this.score++;
        }
    }
    class MsgShell{
        Long sendTime;
        DatagramPacket dp;

        public MsgShell(DatagramPacket datagramPacket, long l) {
            this.dp=datagramPacket;
            this.sendTime=l;

        }
    }

    HashMap<Integer,Player> players = new HashMap<>();
    private LinkedBlockingQueue<MsgShell> needToSendMsgs = new LinkedBlockingQueue<>();
    private HashMap<Integer,Point> steerDirections;//snake id , LastSteer

    int state_order;
    int needResendMsgCount = 0;

    Boolean firstTime = true;
    Boolean isAlive = true;
    Boolean updLis = true;


    private int cellW = 30;
    private int cellH = 30;

    private int width = 20;
    private int height = 20;
    private int foodPerPlayer = 10;
    private int foodStatic = 10;
    private int stateDelay = 200;
    private int deadFoodProb = 0;
    private int pingDelayMs = 400;
    private int nodeTimeoutMs = 800;

    private WindowManager windowManager;
    private SnakeManager snakeManager;
    private NetworkManager networkManager;

    Thread updateListenerThread = null; // Slave recv from Master and set answer to senderThread
    Thread senderThread;
    Thread announcementSenderThread = null;//Master send announcement
    Thread requestsListenerThread = null;//Master recv from Slaves and set answer to senderThread

    public GameManager(String snakeName) {
        this.myName = snakeName;
        this.windowManager = new WindowManager(width, height, cellW, cellH);
        this.networkManager = new NetworkManager();
        this.senderThread = new Thread(new Sender());
        this.senderThread.start();
    }

    public void run() throws  IOException {
        while(true) {
            Update();
            Draw();
        }
    }

    private void StartNewGame() {
        state_order = 0;
        nodeRole = MASTER;
        myId = 0;
        snakeManager = new SnakeMaster(width, height, foodPerPlayer, foodStatic, new HashMap<>(), new HashSet<>());
        ((SnakeMaster)snakeManager).addSnake(0);
        steerDirections = new HashMap<>();
        steerDirections.put(myId,new Point(0,0));
        players.put(myId, new Player(myName, MASTER, 0, "localhost"));
        announcementSenderThread = new Thread(new AnnouncementSender());
        announcementSenderThread.start();
        requestsListenerThread = new Thread(new RequestsListener());
        requestsListenerThread.start();
    }

    private void JoinGame() throws IOException {
        nodeRole = NORMAL;

        if(networkManager.getAvailableGames().size() <= 0){System.out.println("Join no game close");windowManager.closeWindow();}

        HashMap<String, NetworkManager.Master> masters = networkManager.getAvailableGames();

        for(Map.Entry<String, NetworkManager.Master> e : masters.entrySet()) {
            if(e.getValue().msg.getAnnouncement().getCanJoin()) {
                windowManager.setYes(-1);
                windowManager.setNo(-1);
                windowManager.setView(-1);
                windowManager.newWindow(e.getKey());
                while(windowManager.getYes() == -1 && windowManager.getNo() == -1 && windowManager.getView() == -1){System.out.println("Waiting...");}

                if(windowManager.getNo() == 1){System.out.println("Continue");continue;}

                else if(windowManager.getView() == 1){
                    masterIp = e.getValue().masterIP;
                    masterPort = e.getValue().masterPort;
                    windowManager.SetGridSize(e.getValue().msg.getAnnouncement().getConfig().getWidth(), e.getValue().msg.getAnnouncement().getConfig().getHeight());

                    SnakesProto.GameMessage replyMessage = SendJoinMsg(masterIp, masterPort,1);

                    if (replyMessage != null && replyMessage.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                        System.out.println("JOINED TO " + masterIp + " " + masterPort +"(AS VIEWER)");
                        myId = replyMessage.getReceiverId();
                        masterId = replyMessage.getSenderId();
                        players.put(masterId,new Player(replyMessage.getJoin().getName(),MASTER,masterPort,masterIp));
                        snakeManager = new SnakeSlave(e.getValue().msg.getAnnouncement().getConfig().getWidth(), e.getValue().msg.getAnnouncement().getConfig().getHeight(), new HashMap<>(), new HashSet<>());
                        updateListenerThread = new Thread(new UpdateListener());
                        updLis = true;
                        updateListenerThread.start();
                        nodeRole = VIEWER;
                        return;
                    } else {
                        assert replyMessage != null;
                        System.out.println("GOT ERROR: " + replyMessage.getError().getErrorMessage());

                    }
                }
                else {
                    masterIp = e.getValue().masterIP;
                    masterPort = e.getValue().masterPort;
                    windowManager.SetGridSize(e.getValue().msg.getAnnouncement().getConfig().getWidth(), e.getValue().msg.getAnnouncement().getConfig().getHeight());

                    SnakesProto.GameMessage replyMessage = SendJoinMsg(masterIp, masterPort,0);

                    if (replyMessage != null && replyMessage.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                        System.out.println("JOINED TO " + masterIp + " " + masterPort);
                        myId = replyMessage.getReceiverId();
                        masterId = replyMessage.getSenderId();
                        players.put(masterId,new Player(replyMessage.getJoin().getName(),MASTER,masterPort,masterIp));
                        snakeManager = new SnakeSlave(e.getValue().msg.getAnnouncement().getConfig().getWidth(), e.getValue().msg.getAnnouncement().getConfig().getHeight(), new HashMap<>(), new HashSet<>());
                        updateListenerThread = new Thread(new UpdateListener());
                        updLis = true;
                        updateListenerThread.start();
                        return;
                    } else {
                        assert replyMessage != null;
                        System.out.println("GOT ERROR: " + replyMessage.getError().getErrorMessage());

                    }
                }
            }
        }
        System.out.println("Join no game avaliable close");
        windowManager.closeWindow();
    }

    private void Update() throws  IOException {
        int key = windowManager.GetLastKey();
        try {
            TimeUnit.MILLISECONDS.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //start game
        if(key == -2 && firstTime) {
            firstTime = false;
            windowManager.closeMenu();
            JoinGame();
        }
        else if(key == -3 && firstTime) {
            firstTime = false;
            windowManager.closeMenu();
            StartNewGame();
        }

        //play game
        if(snakeManager != null) {

            int snakeId = myId;
            Point dir = null;

            if (nodeRole !=VIEWER && key == KeyEvent.VK_UP) {
                dir = dirToPoint(SnakesProto.Direction.DOWN);
            } else if (nodeRole !=VIEWER && key == KeyEvent.VK_DOWN) {
                dir = dirToPoint(SnakesProto.Direction.UP);
            } else if (nodeRole !=VIEWER && key == KeyEvent.VK_LEFT) {
                dir = dirToPoint(SnakesProto.Direction.LEFT);
            } else if (nodeRole !=VIEWER && key == KeyEvent.VK_RIGHT) {
                dir = dirToPoint(SnakesProto.Direction.RIGHT);
            }

            if(dir != null) {

                if (steerDirections != null && steerDirections.containsKey(snakeId)) {
                    steerDirections.replace(snakeId, dir);
                }
                if (nodeRole != MASTER) {
                    makeSteerMsg(pointToDir(dir), masterIp, masterPort);
                }
            }

            snakeManager.iterate(steerDirections,players);

            if(nodeRole == MASTER) {
                //find deadSnakes - make them viewers
                for (Integer playerIdx:players.keySet()) {
                    if (playerIdx != myId) {
                        if(players.get(playerIdx).role!=VIEWER && snakeManager.snakeEmpty(playerIdx)){
                            //System.out.println("++++++++++++++++++++++++++++++++FOUND DEAD SNAKE");
                            if(players.get(playerIdx).role==DEPUTY ){deputy=null;}
                            players.get(playerIdx).role=VIEWER;
                            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                    .setReceiverRole(SnakesProto.NodeRole.VIEWER)
                                    .build();
                            SnakesProto.GameMessage gameMessage1 = SnakesProto.GameMessage.newBuilder()
                                    .setRoleChange(roleChangeMsg)
                                    .setSenderId(myId)
                                    .setReceiverId(playerIdx)
                                    .setMsgSeq(networkManager.getSeq())
                                    .build();
                            needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage1, players.get(playerIdx).ipAddress, players.get(playerIdx).port), -1));
                        }
                    }
                }

                //if i am MASTER and my snake is dead i turned to be VIEWER and DEPUTY turned to be MASTER
                if(snakeManager.snakeEmpty(myId) && deputy!= null){
                    SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                            .setReceiverRole(SnakesProto.NodeRole.MASTER)
                            .setSenderRole(SnakesProto.NodeRole.VIEWER)
                            .build();
                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                            .setRoleChange(roleChangeMsg)
                            .setSenderId(myId)
                            .setReceiverId(deputyId)
                            .setMsgSeq(networkManager.getSeq())
                            .build();
                    networkManager.sendUnicastMsg(gameMessage, deputy.ipAddress, deputy.port);
                    makeMyselfViewer();
                    return;
                }

                //make config msg for players about  this game
                state_order++;
                SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder()
                        .setDeadFoodProb(deadFoodProb)
                        .setFoodPerPlayer(foodPerPlayer)
                        .setFoodStatic(foodStatic)
                        .setHeight(height)
                        .setWidth(width)
                        .setNodeTimeoutMs(nodeTimeoutMs)
                        .setPingDelayMs(pingDelayMs)
                        .build();
                SnakesProto.GamePlayers.Builder gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();
                for (Integer playerIdx:players.keySet()) {
                    SnakesProto.GamePlayer gamePlayer = SnakesProto.GamePlayer.newBuilder()
                            .setId(playerIdx)
                            .setPort(players.get(playerIdx).port)
                            .setRole(SnakesProto.NodeRole.forNumber(players.get(playerIdx).role))
                            .setScore(players.get(playerIdx).score)
                            .setType(SnakesProto.PlayerType.forNumber(players.get(playerIdx).type))
                            .setIpAddress(players.get(playerIdx).ipAddress)
                            .setName(players.get(playerIdx).Name)
                            .build();
                    gamePlayersBuilder.addPlayers(gamePlayer);
                }
                SnakesProto.GamePlayers gamePlayers = gamePlayersBuilder.build();
                SnakesProto.GameState.Builder stateBuilder = SnakesProto.GameState.newBuilder()
                        .setStateOrder(networkManager.getSeq())
                        .setPlayers(gamePlayers)
                        .setConfig(gameConfig);
                for (Point food : snakeManager.getFoods()) {
                    stateBuilder.addFoods(SnakesProto.GameState.Coord.newBuilder().setX(food.x).setY(food.y).build());
                }
                HashMap<Integer, SnakeManager.Snake> snakes = snakeManager.getSnakes();
                for (int snakeIdx : snakes.keySet()) {
                    boolean AZ = players.containsKey(snakeIdx) && players.get(snakeIdx).role != VIEWER;
                    if (snakes.get(snakeIdx).p.size() == 0) {
                        continue;
                    }
                    SnakesProto.GameState.Snake.Builder snakeBuilder = SnakesProto.GameState.Snake.newBuilder()
                            .setHeadDirection(pointToDir(snakes.get(snakeIdx).lastDir))
                            .setPlayerId(snakeIdx)
                            .setState(AZ ? SnakesProto.GameState.Snake.SnakeState.ALIVE : SnakesProto.GameState.Snake.SnakeState.ZOMBIE);
                    for (int snakeCellIdx = 0; snakeCellIdx < snakes.get(snakeIdx).p.size(); ++snakeCellIdx) {
                        int x = snakes.get(snakeIdx).p.get(snakeCellIdx).x;
                        int y = snakes.get(snakeIdx).p.get(snakeCellIdx).y;
                        snakeBuilder.addPoints(SnakesProto.GameState.Coord.newBuilder().setX(x).setY(y).build());
                    }
                    SnakesProto.GameState.Snake snake = snakeBuilder.build();
                    stateBuilder.addSnakes(snake);
                }
                stateBuilder.setStateOrder(state_order);
                SnakesProto.GameState state = stateBuilder.build();
                SnakesProto.GameMessage.StateMsg stateMsg = SnakesProto.GameMessage.StateMsg.newBuilder()
                        .setState(state)
                        .build();
                for (Integer playerIdx:players.keySet()) {
                    if (playerIdx != myId) {
                        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                                .setState(stateMsg)
                                .setSenderId(myId)
                                .setReceiverId(playerIdx)
                                .setMsgSeq(networkManager.getSeq())
                                .build();
                        needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage, players.get(playerIdx).ipAddress, players.get(playerIdx).port),-1));

                    }
                }
                //select deputy
                if (players.size()>1 && deputy==null){
                    //System.out.println("changed deputy");
                    for (Integer playerIdx:players.keySet()) {
                        if (playerIdx != myId) {
                        if(players.get(playerIdx).role == NORMAL){
                            deputy = players.get(playerIdx);
                            deputyId=playerIdx;
                            deputy.role=DEPUTY;
                            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                    .setReceiverRole(SnakesProto.NodeRole.DEPUTY)
                                    .build();
                            SnakesProto.GameMessage gameMessage1 = SnakesProto.GameMessage.newBuilder()
                                    .setRoleChange(roleChangeMsg)
                                    .setSenderId(myId)
                                    .setReceiverId(playerIdx)
                                    .setMsgSeq(networkManager.getSeq())
                                    .build();
                            needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage1, players.get(playerIdx).ipAddress, players.get(playerIdx).port), -1));
                            break;
                        }
                        }
                    }
                }
            }

            if(nodeRole == MASTER && snakeManager.getAliveSnakesCount(players.keySet()) == 0 ){System.out.println("No snake (master) close");windowManager.closeWindow();}
        }
    }

    private void Draw() {
        if(snakeManager != null) {
            // draw snakes
            int[][] field = snakeManager.getField();
            for (int x = 0; x < width; ++x) {
                for (int y = 0; y < height; ++y) {
                    if (field[x][y] == -1) {
                        windowManager.ClearCell(x, y);
                    } else {
                        windowManager.FillCell(x, y, new Color(field[x][y] == myId ? 255 : 0, 0, field[x][y] == myId ? 0 : 255));
                    }
                }
            }
            // draw food
            HashSet<Point> foods = snakeManager.getFoods();
            for (Point food : foods) {
                windowManager.FillCell(food.x, food.y, new Color(0, 255, 0));
            }
            // update drawing panel
            windowManager.Present();
            // wait
            if (nodeRole == MASTER) {
                try {
                    TimeUnit.MILLISECONDS.sleep(stateDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class UpdateListener implements  Runnable {
        @Override
        public void run() {
            while(nodeRole != MASTER && updLis) {

                SnakesProto.GameMessage msg = null;
                DatagramPacket recvPacket = null;
                try {
                    recvPacket = networkManager.recvUnicastMsg();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(recvPacket == null){continue;}

                String ip = recvPacket.getAddress().getHostAddress();
                Integer port = recvPacket.getPort();
                try {
                    ByteBuffer buf = ByteBuffer.wrap(recvPacket.getData(), 0, recvPacket.getLength());
                    msg = SnakesProto.GameMessage.parseFrom(buf);
                } catch (InvalidProtocolBufferException e) {
                    e.printStackTrace();
                }
                if(msg == null){continue;}

                if( msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.STATE) {

                    pingDelayMs = msg.getState().getState().getConfig().getPingDelayMs();
                    int current_state_order = msg.getState().getStateOrBuilder().getStateOrder();
                    if(current_state_order <= state_order){
                        makeAck(msg, ip, port,-1);
                        continue;}
                    else{state_order = current_state_order;}

                    //System.out.println("RECV STATE "+ msg.getMsgSeq());
                    HashMap<Integer, SnakeManager.Snake> snakes = new HashMap<>();
                    HashSet<Point> foods = new HashSet<>();

                    for(SnakesProto.GameState.Coord f : msg.getState().getState().getFoodsList()) {
                        foods.add(new Point(f.getX(), f.getY()));
                    }

                    for(SnakesProto.GameState.Snake s : msg.getState().getState().getSnakesList()) {

                        snakes.put(s.getPlayerId(),new SnakeManager.Snake(dirToPoint(s.getHeadDirection()), new ArrayList<>()));
                        for(SnakesProto.GameState.Coord sc : s.getPointsList()) {
                            snakes.get(s.getPlayerId()).p.add(new Point(sc.getX(), sc.getY()));

                        }
                    }
                    printScore(msg);
                    ((SnakeSlave)snakeManager).setState(snakes, foods);
                    makeAck(msg, ip, port,-1);
                    rebuildPlayers(msg);
                    rebuildSnakes(msg);

                    if(snakeManager.getAliveSnakesCount(players.keySet()) == 0){
                        System.out.println("No snake (slave) close");
                        windowManager.closeWindow();
                        return;}

                }
                else if( msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ROLE_CHANGE){

                    if(msg.getRoleChange().getReceiverRole()== SnakesProto.NodeRole.DEPUTY){
                        System.out.println("I AM DEPUTY "+ msg.getMsgSeq());
                        nodeRole = DEPUTY;}
                    else if(msg.getRoleChange().getReceiverRole()== SnakesProto.NodeRole.VIEWER){
                        System.out.println("I AM VIEWER "+ msg.getMsgSeq());
                        nodeRole = VIEWER;
                    }
                    else if(nodeRole == DEPUTY &&
                            msg.getRoleChange().getReceiverRole() == SnakesProto.NodeRole.MASTER &&
                            msg.getRoleChange().getSenderRole() == SnakesProto.NodeRole.VIEWER){
                       // System.out.println("MASTER TOLD ME : I AM MASTER"+ players.get(masterId).port);
                        players.get(masterId).role=VIEWER;
                        makeMyselfMaster();
                        for (Map.Entry<Integer, Player> p:players.entrySet()) {
                            SnakesProto.GameMessage.RoleChangeMsg roleChangeMsg = SnakesProto.GameMessage.RoleChangeMsg.newBuilder()
                                    .setSenderRole(SnakesProto.NodeRole.MASTER)
                                    .build();
                            SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                                    .setRoleChange(roleChangeMsg)
                                    .setSenderId(myId)
                                    .setReceiverId(p.getKey())
                                    .setMsgSeq(networkManager.getSeq())
                                    .build();
                            networkManager.sendUnicastMsg(gameMessage, p.getValue().ipAddress,p.getValue().port);
                        }
                        break;
                    }
                    else if(msg.getRoleChange().getSenderRole() == SnakesProto.NodeRole.MASTER &&
                            deputy!= null && deputy.ipAddress.equals(ip) && deputy.port == port){
                                //System.out.println("I HAVE NEW MASTER");
                                oldMasterId = masterId;
                                oldMasterIp = masterIp;
                                needResendMsgCount = needToSendMsgs.size();
                                players.get(oldMasterId).role = VIEWER;

                                masterId = deputyId;
                                masterIp = deputy.ipAddress;
                                masterPort = deputy.port;
                                players.get(deputyId).role = MASTER;
                                continue;
                    }
                    else {continue;}
                    makeAck(msg, ip, port,-1);
                }
                else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK){
                   // System.out.println("RECV ACK "+ msg.getMsgSeq());
                    checkNeedToSendMsgs(msg);
                }
                else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.JOIN){
                    SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder()
                            .setErrorMessage("I AM BETWEEN MASTER AND SLAVE")
                            .build();
                    SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                            .setError(errorMsg)
                            .setMsgSeq(msg.getMsgSeq())
                            .build();
                    networkManager.sendUnicastMsg(gameMessage,ip,port);
                }
                else{continue;}


            }
        }

    }

    private class RequestsListener implements Runnable {
        @Override
        public void run() {
            while(isAlive) {
                if(nodeRole == MASTER) {

                    SnakesProto.GameMessage msg = null;
                    DatagramPacket recvPacket = null;
                    try {
                        recvPacket = networkManager.recvUnicastMsg();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //System.out.println("HERE ip="+networkManager.getUnicastIp()+" "+networkManager.getUnicastPort());

                    if(recvPacket == null){continue;}

                    String ip = recvPacket.getAddress().getHostAddress();
                    Integer port = recvPacket.getPort();
                    try {
                        ByteBuffer buf = ByteBuffer.wrap(recvPacket.getData(), 0, recvPacket.getLength());
                        msg = SnakesProto.GameMessage.parseFrom(buf);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }

                    if (msg == null) {
                        continue;
                    }

                    if (players.containsKey(msg.getSenderId()) && players.get(msg.getSenderId()).role == VIEWER) {
                        if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                            //System.out.println("RECV ACK " + msg.getMsgSeq());
                            checkNeedToSendMsgs(msg);
                        } else {
                            //System.out.println("JUST ANSWER TO VIEWER " + msg.getMsgSeq());
                            makeAck(msg, ip, port, -1);
                        }
                        continue;
                    }


                    if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.JOIN) {

                        if (msg.getJoin().getName().equals(myName)) {
                            continue;
                        }

                        int id = ((SnakeMaster) snakeManager).getId();
                       // System.out.println("RECV JOIN " + msg.getMsgSeq() + " id = " + id);

                        if (msg.getJoin().getOnlyView()) {
                            players.put(id, new Player(msg.getJoin().getName(), VIEWER, port, ip));
                            makeAck(msg, ip, port, id);
                        } else {

                            if (((SnakeMaster) snakeManager).canAddSnake()) {
                                ((SnakeMaster) snakeManager).addSnake(id);
                                steerDirections.put(id, new Point(0, 0));
                                players.put(id, new Player(msg.getJoin().getName(), NORMAL, port, ip));
                                makeAck(msg, ip, port, id);
                            } else {
                                SnakesProto.GameMessage.ErrorMsg errorMsg = SnakesProto.GameMessage.ErrorMsg.newBuilder()
                                        .setErrorMessage("NO SPACE")
                                        .build();
                                SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                                        .setError(errorMsg)
                                        .setMsgSeq(msg.getMsgSeq())
                                        .build();
                                needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage, ip, port), (long) -1));
                            }
                        }
                    } else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.STEER) {
                       // System.out.println("RECV STEER " + msg.getMsgSeq());
                        Point dir = dirToPoint(msg.getSteer().getDirection());
                        steerDirections.replace(msg.getSenderId(), dir);
                        makeAck(msg, ip, port, -1);
                    }
                    else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.PING) {
                        //System.out.println("RECV PING " + msg.getMsgSeq());
                        makeAck(msg, ip, port, -1);
                    }
                    else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                       //System.out.println("RECV ACK " + msg.getMsgSeq());
                        checkNeedToSendMsgs(msg);
                    }
                    else if (msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.ROLE_CHANGE) {
                        if (msg.getRoleChange().getSenderRole() == SnakesProto.NodeRole.VIEWER) {
                            //make this player viewer - om
                            players.get(msg.getSenderId()).role = VIEWER;
                            makeAck(msg, ip, port, -1);
                        }
                    }
                }
            }
        }

    }

    private class AnnouncementSender implements Runnable {
        @Override
        public void run() {
            while(isAlive) {
                if (nodeRole == MASTER) {
                    try {
                        SnakesProto.GameConfig gameConfig = SnakesProto.GameConfig.newBuilder()
                                .setDeadFoodProb(deadFoodProb)
                                .setFoodPerPlayer(foodPerPlayer)
                                .setFoodStatic(foodStatic)
                                .setHeight(height)
                                .setWidth(width)
                                .setNodeTimeoutMs(nodeTimeoutMs)
                                .setPingDelayMs(pingDelayMs)
                                .build();
                        SnakesProto.GamePlayers.Builder gamePlayersBuilder = SnakesProto.GamePlayers.newBuilder();
                        System.out.println("SCORE:");
                        for (Integer playerIdx:players.keySet()) {
                            SnakesProto.GamePlayer gamePlayer = SnakesProto.GamePlayer.newBuilder()
                                    .setId(playerIdx)
                                    .setPort(players.get(playerIdx).port)
                                    .setRole(SnakesProto.NodeRole.forNumber(players.get(playerIdx).role))
                                    .setScore(players.get(playerIdx).score)
                                    .setType(SnakesProto.PlayerType.forNumber(players.get(playerIdx).type))
                                    .setIpAddress(players.get(playerIdx).ipAddress)
                                    .setName(players.get(playerIdx).Name)
                                    .build();
                            gamePlayersBuilder.addPlayers(gamePlayer);
                            System.out.println(players.get(playerIdx).score + " : " + players.get(playerIdx).Name);
                        }
                        SnakesProto.GamePlayers gamePlayers = gamePlayersBuilder.build();
                        SnakesProto.GameMessage.AnnouncementMsg announcementMsg = SnakesProto.GameMessage.AnnouncementMsg.newBuilder()
                                .setCanJoin(((SnakeMaster) snakeManager).canAddSnake())
                                .setConfig(gameConfig)
                                .setPlayers(gamePlayers)
                                .build();
                        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                                .setAnnouncement(announcementMsg)
                                .setMsgSeq(networkManager.getSeq())
                                .build();
                        networkManager.sendMulticastMsg(gameMessage);


                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException | NullPointerException e) {
                        System.out.println("Interrupted (AnnouncementSender)");
                        return;
                    }
                }
            }
        }

    }

    private class Sender implements Runnable {

        @Override
        public void run() {
            Long lastSendTime = System.currentTimeMillis();

            while (true) {

                Iterator iterator = needToSendMsgs.iterator();

                while (true) {

                    if (Math.abs(lastSendTime - System.currentTimeMillis()) > pingDelayMs) {
                        if (nodeRole != MASTER) {
                            SnakesProto.GameMessage gameMessage = makePingMsg();
                            if (gameMessage != null) {
                                networkManager.sendUnicastMsg(gameMessage, masterIp, masterPort);
                               // System.out.println("SEND " + gameMessage.getTypeCase() + " " + gameMessage.getMsgSeq());
                                lastSendTime = System.currentTimeMillis();
                                needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage, masterIp, masterPort), lastSendTime));
                                continue;
                            }
                        }
                    }

                    MsgShell msgShell = null;
                    try {
                        msgShell = (MsgShell) iterator.next();
                    } catch (java.util.NoSuchElementException e) {
                        break;
                    }

                    DatagramPacket datagramPacket = msgShell.dp;
                    SnakesProto.GameMessage gameMessage = null;
                    Long sendTime = msgShell.sendTime;
                    try {
                        ByteBuffer buf = ByteBuffer.wrap(datagramPacket.getData(), 0, datagramPacket.getLength());
                        gameMessage = SnakesProto.GameMessage.parseFrom(buf);
                    } catch (InvalidProtocolBufferException e) {
                        e.printStackTrace();
                    }

                    if (datagramPacket == null || gameMessage == null) {
                        continue;
                    }


                    //if we have msg for player that is not exist now
                    if (nodeRole == MASTER && !players.containsKey(gameMessage.getReceiverId())) {
                        needToSendMsgs.remove(msgShell);
                        continue;
                    }

                    if (nodeRole != MASTER && oldMasterIp != null && gameMessage.getReceiverId() == oldMasterId) {
                        //rebuild msg and add to needToSendMsg
                        datagramPacket.setPort(masterPort);
                        try {
                            datagramPacket.setAddress(InetAddress.getByName(masterIp));
                        } catch (UnknownHostException e) {
                            e.printStackTrace();
                        }

                        needToSendMsgs.remove(msgShell);

                        msgShell.dp = datagramPacket;
                        msgShell.sendTime = (long) -1;

                        if (gameMessage.getTypeCase() == SnakesProto.GameMessage.TypeCase.PING) {
                            continue;
                        }

                        needToSendMsgs.add(msgShell);
                        sendTime = (long) -1;
                        needResendMsgCount--;
                    }

                    String ip = datagramPacket.getAddress().getHostAddress();
                    Integer port = datagramPacket.getPort();

                    if (gameMessage.getTypeCase() != SnakesProto.GameMessage.TypeCase.ACK &&
                            gameMessage.getTypeCase() != SnakesProto.GameMessage.TypeCase.ANNOUNCEMENT) {

                        long currentTime = System.currentTimeMillis();

                        if (sendTime == -1) {
                            //send msg first time
                            networkManager.sendUnicastMsg(gameMessage, ip, port);
                            lastSendTime = System.currentTimeMillis();
                           // System.out.println("SEND FIRST " + gameMessage.getTypeCase() + " " + gameMessage.getMsgSeq());
                            msgShell.sendTime = currentTime;

                        } else if (Math.abs(currentTime - sendTime) > gameMessage.getState().getState().getConfig().getPingDelayMs()) {

                            if (Math.abs(currentTime - sendTime) > gameMessage.getState().getState().getConfig().getNodeTimeoutMs()) {
                                //node is dead
                                System.out.println("NODE IS DEAD :" + gameMessage.getReceiverId());
                                needToSendMsgs.remove(msgShell);

                                if (nodeRole == NORMAL || nodeRole == VIEWER) {
                                   // System.out.println("-01 ("+players.get(gameMessage.getReceiverId()).role+")");

                                    if(snakeManager.getAliveSnakesCount(players.keySet()) == 0){
                                        System.out.println("No snake (sender) close");
                                        windowManager.closeWindow();
                                        return;}

                                    if (players.get(gameMessage.getReceiverId()).role == MASTER) {
                                        oldMasterId = masterId;
                                        oldMasterIp = masterIp;
                                        needResendMsgCount = needToSendMsgs.size();

                                        players.remove(masterId);

                                        if (deputy != null) {
                                            masterId = deputyId;
                                            masterIp = deputy.ipAddress;
                                            masterPort = deputy.port;
                                            players.get(deputyId).role = MASTER;
                                        } else {
                                            System.out.println("NO MASTER AND NO DEPUTY close");
                                            windowManager.closeWindow();
                                        }

                                        deputy = null;
                                        players.remove(gameMessage.getReceiverId());///////////////организовать пересылку

                                    }

                                } else if (nodeRole == DEPUTY) {
                                    if ( players.get(gameMessage.getReceiverId()).role == MASTER) {
                                        players.remove(masterId);
                                        makeMyselfMaster();
                                        System.out.println("I AM MASTER");
                                    }
                                } else if (nodeRole == MASTER) {
                                    if (players.get(gameMessage.getReceiverId()).role == NORMAL || players.get(gameMessage.getReceiverId()).role == VIEWER) {
                                        players.remove(gameMessage.getReceiverId());
                                    } else if (players.get(gameMessage.getReceiverId()).role == DEPUTY) {
                                        players.remove(gameMessage.getReceiverId());
                                        deputy = null;
                                        deputyId = null;
                                    }
                                }
                            } else {
                                //send msg again
                                //System.out.println("SEND AGAIN " + gameMessage.getTypeCase() + " " + gameMessage.getMsgSeq() + " "+ip+" "+port);
                                networkManager.sendUnicastMsg(gameMessage, ip, port);
                                lastSendTime = System.currentTimeMillis();
                            }
                        } else {
                            //it's not time to send the message
                        }


                    } else if (gameMessage.getTypeCase() == SnakesProto.GameMessage.TypeCase.ACK) {
                        //send and remove
                       // System.out.println("SEND " + gameMessage.getTypeCase() + " " + gameMessage.getMsgSeq());
                        networkManager.sendUnicastMsg(gameMessage, ip, port);
                        lastSendTime = System.currentTimeMillis();
                        needToSendMsgs.remove(msgShell);
                    }

                }
            }
        }

    }

    private SnakesProto.GameMessage SendJoinMsg(String addr, Integer port, int view) throws IOException {
       // System.out.println("SEND JOIN TO " + addr + " "+port);

        SnakesProto.GameMessage.JoinMsg joinMsg;

        if(view == 0){
            joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder()
                    .setOnlyView(false)
                    .setPlayerType(SnakesProto.PlayerType.forNumber(HUMAN))
                    .setName(myName)
                    .build();
        } else{
            joinMsg = SnakesProto.GameMessage.JoinMsg.newBuilder()
                    .setOnlyView(true)
                    .setPlayerType(SnakesProto.PlayerType.forNumber(HUMAN))
                    .setName(myName)
                    .build();
        }
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                .setJoin(joinMsg)
                .setMsgSeq(networkManager.getSeq())
                .build();
        networkManager.sendUnicastMsg(gameMessage, addr, port);


        DatagramPacket recvPacket = networkManager.recvUnicastMsg();

        while(recvPacket == null){
            recvPacket = networkManager.recvUnicastMsg();
        }

        try {
            ByteBuffer buf = ByteBuffer.wrap(recvPacket.getData(), 0, recvPacket.getLength());
            return SnakesProto.GameMessage.parseFrom(buf);
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void makeSteerMsg(SnakesProto.Direction dir ,String ip, Integer port){


        SnakesProto.GameMessage.SteerMsg steerMsg = SnakesProto.GameMessage.SteerMsg.newBuilder()
                .setDirection(dir)
                .build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                .setSteer(steerMsg)
                .setSenderId(myId)
                .setReceiverId(masterId)
                .setMsgSeq(networkManager.getSeq())
                .build();
        //System.out.println("MADE STEER MSG " + gameMessage.getMsgSeq());
        needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage, ip, port), -1));
    }

    private void makeAck(SnakesProto.GameMessage msg, String ip, Integer port, Integer i) {
        int id;
        if(msg.getTypeCase() == SnakesProto.GameMessage.TypeCase.JOIN){
            id = i;
        }
        else{
            id = msg.getSenderId();
        }

        SnakesProto.GameMessage.AckMsg ackMsg = SnakesProto.GameMessage.AckMsg.newBuilder().build();
        SnakesProto.GameMessage gameMessage = SnakesProto.GameMessage.newBuilder()
                .setAck(ackMsg)
                .setMsgSeq(msg.getMsgSeq())
                .setReceiverId(id)
                .setSenderId(myId)
                .build();
        needToSendMsgs.add(new MsgShell(datagramPacketMaker(gameMessage, ip, port), -1));
    }

    private SnakesProto.GameMessage makePingMsg() {

        if(masterId == null || masterIp == null  || masterPort == null ){return null;}

        SnakesProto.GameMessage.PingMsg pingMsg = SnakesProto.GameMessage.PingMsg.newBuilder().build();
        return SnakesProto.GameMessage.newBuilder()
                .setPing(pingMsg)
                .setMsgSeq(networkManager.getSeq())
                .setReceiverId(masterId)
                .setSenderId(myId)
                .build();
    }

    DatagramPacket datagramPacketMaker(SnakesProto.GameMessage gameMessage,String ip,Integer port){
        InetAddress IPAddress = null;
        try {
            IPAddress = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        byte[] sendData = gameMessage.toByteArray();
        return (new DatagramPacket(sendData, sendData.length, IPAddress, port));
    }

    private void checkNeedToSendMsgs(SnakesProto.GameMessage msgAck) {

        for (MsgShell msgShell : needToSendMsgs) {
            DatagramPacket dp = msgShell.dp;
            ByteBuffer buf1 = ByteBuffer.wrap(dp.getData(), 0, dp.getLength());
            SnakesProto.GameMessage currentMsg = null;
            try {
                currentMsg = SnakesProto.GameMessage.parseFrom(buf1);
            } catch (InvalidProtocolBufferException e) {
                e.printStackTrace();
            }

            assert currentMsg != null;
            if (currentMsg.getMsgSeq() == msgAck.getMsgSeq() && msgAck.getSenderId() == currentMsg.getReceiverId()) {
                needToSendMsgs.remove(msgShell);
                //System.out.println("REMOVED "+msgAck.getMsgSeq() );
                break;
            }
        }
    }

    private void printScore(SnakesProto.GameMessage msg){
        System.out.println("SCORE:");
        for(SnakesProto.GameState.Snake i : msg.getState().getState().getSnakesList()) {
            System.out.println(msg.getState().getState().getPlayers().getPlayers(i.getPlayerId()).getScore()+" : " + msg.getState().getState().getPlayers().getPlayers(i.getPlayerId()).getName());
        }
    }

    private SnakesProto.Direction pointToDir(Point p){
        if(p.x == 0 && p.y == 1){return SnakesProto.Direction.UP;}
        else if(p.x == 0 && p.y == -1){return SnakesProto.Direction.DOWN;}
        else if(p.x == 1 && p.y == 0){return SnakesProto.Direction.RIGHT;}
        else {return SnakesProto.Direction.LEFT;}
    }

    private Point dirToPoint(SnakesProto.Direction p){
        if(p == SnakesProto.Direction.UP){return new Point(0,1);}
        else if(p == SnakesProto.Direction.DOWN){return new Point(0,-1);}
        else if(p == SnakesProto.Direction.RIGHT){return new Point(1,0);}
        else if(p == SnakesProto.Direction.LEFT){return new Point(-1,0);}
        else{return new Point(0,0); }
    }

    private void rebuildPlayers(SnakesProto.GameMessage msg){

        LinkedList<Integer> indexList = new LinkedList<>();

        for (SnakesProto.GamePlayer p :msg.getState().getState().getPlayers().getPlayersList()) {

            indexList.add(p.getId());

            if(p.getRole() == SnakesProto.NodeRole.MASTER && p.getId() == masterId){continue;}


            if(players.containsKey(p.getId())){
                makeEqual(p,players.get(p.getId()));
            }
            else{

                Player p1 = new Player(p.getName(),p.getRole().getNumber(),p.getPort(),p.getIpAddress());
                p1.type = p.getType().getNumber();
                players.put(p.getId(),p1);
                if(p.getRole() == SnakesProto.NodeRole.DEPUTY){
                    deputy = p1;
                    deputyId = p.getId();
                }
            }
           // System.out.println(p.getId()+" "+p.getRole());
        }

        for (Integer i : indexList) {
            if(!players.containsKey(i)){
                players.remove(i);
            }
        }

    }

    private void rebuildSnakes(SnakesProto.GameMessage msg){

        for(SnakesProto.GameState.Snake s : msg.getState().getState().getSnakesList()) {

            if(!snakeManager.snakes.containsKey(s.getPlayerId()))
            {snakeManager.snakes.remove(s.getPlayerId());}
        }
    }

    private void makeEqual(SnakesProto.GamePlayer p, Player p1){
        if(!p.getName().equals(p1.Name)){
            p1.Name = p.getName();
        }
        if(p.getScore() != p1.score){
            p1.score=p.getScore();
        }
        if(!p.getIpAddress().equals(p1.ipAddress)){
            p1.ipAddress = p.getIpAddress();
        }
        if(p.getPort() != p1.port){
            p1.port = p.getPort();
        }
        if(p.getRole().getNumber() != p1.role){
            p1.role=p.getRole().getNumber();
        }
        if(p.getType().getNumber() != p1.type){
            p1.type = p.getType().getNumber();
        }
        if(p.getRole() == SnakesProto.NodeRole.DEPUTY){
            deputy = p1;
            deputyId = p.getId();
        }

    }

    private void makeMyselfMaster()  {
       // System.out.println("make me master start......");
        updLis = false;
        updateListenerThread = null;


        snakeManager = new SnakeMaster(width, height, foodPerPlayer, foodStatic, snakeManager.snakes, snakeManager.foods);

        steerDirections = new HashMap<>();

        for (Map.Entry<Integer, SnakeManager.Snake> s:snakeManager.snakes.entrySet()) {
                 steerDirections.put(s.getKey(), s.getValue().lastDir);
        }


        players.get(myId).role=MASTER;
        deputy = null;
        deputyId = null;
        nodeRole = MASTER;

        announcementSenderThread = new Thread(new AnnouncementSender());
        announcementSenderThread.start();

        requestsListenerThread = new Thread(new RequestsListener());
        requestsListenerThread.start();

    }

    private void makeMyselfViewer(){

        isAlive = false;//for announcementSender and requestListener
        announcementSenderThread=null;
        requestsListenerThread= null;


        snakeManager = new SnakeSlave(width, height, snakeManager.snakes, snakeManager.foods);

        players.get(myId).role=VIEWER;
        nodeRole = VIEWER;

        needToSendMsgs.clear();

        players.get(deputyId).role=MASTER;
        masterPort = deputy.port;
        masterIp = deputy.ipAddress;
        masterId = deputyId;
        deputy= null;

        updateListenerThread = new Thread(new UpdateListener());
        updLis = true;
        updateListenerThread.start();
       // System.out.println("I BECOME V ---------------------------------");
    }
}
