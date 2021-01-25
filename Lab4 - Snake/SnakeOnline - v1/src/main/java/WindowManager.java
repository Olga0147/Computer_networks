import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class WindowManager {

    private Integer colNum, rowNum, cellW, cellH;

    private JFrame mainWindow;
    private JPanel drawingPanel;
    JMenu playMenu;

    private Color emptyColor = new Color(255, 255, 255);
    private Color gridColor = new Color(128,128,128);
    private Color cells[][];
    private Integer cellBorder = 2;

    private int yes = -1;
    private int no = -1;
    private  int view = -1;

    private ArrayList<Integer> pressedKeysQueue = new ArrayList<>();

    public WindowManager(int colNum, int rowNum, int cellW, int cellH) {
        this.colNum = colNum;
        this.rowNum = rowNum;
        this.cellH = cellH;
        this.cellW = cellW;
        this.cells = new Color[rowNum][colNum];
        for(int y = 0; y < rowNum; ++y) {
            for(int x = 0; x < colNum; ++x) {
                //cells[x][y] = emptyColor;
                Color startC = new Color(ThreadLocalRandom.current().nextInt(50, 255 + 1), ThreadLocalRandom.current().nextInt(50, 255 + 1), ThreadLocalRandom.current().nextInt(50, 255 + 1));
                cells[x][y] = startC;
            }
        }
        initWindow();
    }

    public int GetLastKey() {
        int key = -1;
        if(pressedKeysQueue.size() > 0) {
            key = pressedKeysQueue.get(0);
        }
        pressedKeysQueue.clear();
        return key;
    }

    public void FillCell(int x, int y, Color color) {
        cells[x][y] = color;
    }

    public void ClearCell(int x, int y) {
        cells[x][y] = emptyColor;
    }

    public void Present() {
        drawingPanel.paintImmediately(0,0,drawingPanel.getWidth(), drawingPanel.getHeight());
    }

    private void initWindow() {
        this.mainWindow = new JFrame("Snake Online");
        Integer clientW = colNum * cellW;
        Integer clientH = rowNum * cellH;
        mainWindow.setSize(clientW, clientH);
        mainWindow.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        mainWindow.setLocationRelativeTo(null);

        this.drawingPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                super.paint(g);
                DrawGrid(g);
                DrawCells(g);
            }
        };
        drawingPanel.setPreferredSize(new Dimension(clientW, clientW));

        JMenuBar menuBar = new JMenuBar();
        playMenu = new JMenu("Play");
        JMenuItem newGameItem = new JMenuItem("New game");
        newGameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pressedKeysQueue.add(-3);
            }
        });
        playMenu.add(newGameItem);
        JMenuItem joinGameItem = new JMenuItem("Join game");
        joinGameItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pressedKeysQueue.add(-2);
            }
        });
        playMenu.add(joinGameItem);
        menuBar.add(playMenu);
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new GridLayout(1, 1));
        controlsPanel.add(menuBar);

        mainWindow.add(controlsPanel, BorderLayout.NORTH);
        mainWindow.add(drawingPanel);
        mainWindow.pack();
        mainWindow.setFocusable(true);
        mainWindow.addKeyListener(new KeyboardListener());
        mainWindow.setVisible(true);
    }

    public void SetGridSize(int colNum, int rowNum) {
        this.colNum = colNum;
        this.rowNum = rowNum;
        this.cells = new Color[rowNum][colNum];
        for(int y = 0; y < rowNum; ++y) {
            for(int x = 0; x < colNum; ++x) {
                cells[x][y] = emptyColor;
            }
        }
        Integer clientW = colNum * cellW;
        Integer clientH = rowNum * cellH;
        mainWindow.setSize(clientW, clientH);
        drawingPanel.setPreferredSize(new Dimension(clientW, clientW));
        mainWindow.pack();
    }

    private void DrawGrid(Graphics g) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setStroke(new BasicStroke(2 * cellBorder));
        g.setColor(gridColor);

        for(int x = 0; x < colNum + 1; ++x) {
            g.drawLine(x * cellW, 0, x * cellW, drawingPanel.getHeight());
        }
        for(int y = 0; y < rowNum + 1; ++y) {
            g.drawLine(0, y * cellH, drawingPanel.getWidth(), y * cellH);
        }
    }

    private void DrawCells(Graphics g) {
        for(int x = 0; x < colNum; ++x) {
            for(int y = 0; y < rowNum; ++y) {
                g.setColor(cells[x][y]);
                g.fillRect(x * cellW + cellBorder, y * cellH + cellBorder, cellW - 2 * cellBorder, cellH - 2 * cellBorder);
            }
        }
    }

    public int getYes() {
        return yes;
    }
    public void setYes(int yes) {
        this.yes = yes;
    }
    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
    }

    public int getView() {
        return view;
    }

    public void setView(int view) {
        this.view = view;
    }

    private class KeyboardListener implements KeyListener {
        @Override
        public void keyTyped(KeyEvent e) {
        }
        @Override
        public void keyPressed(KeyEvent e) {
            //e.getKeyCode() in [KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_RIGHT, KeyEvent.VK_LEFT]
            pressedKeysQueue.add(pressedKeysQueue.size(), e.getKeyCode());
        }
        @Override
        public void keyReleased(KeyEvent e) {
        }
    }

    public  void closeWindow(){
        mainWindow.dispatchEvent(new WindowEvent(mainWindow, WindowEvent.WINDOW_CLOSING));
    }

    public void hideWindow(){
        mainWindow.setVisible(false);
    }

    public void closeMenu(){
        playMenu.setEnabled(false);
    }

    public void newWindow(String ip){
        JFrame newWindow = new JFrame("Snake Online");
        Integer clientW = colNum * cellW;
        Integer clientH = rowNum * cellH;
        newWindow.setSize(clientW, clientH);
        newWindow.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        newWindow.setLocationRelativeTo(mainWindow);

        JPanel mainPanel = new JPanel();

        JLabel label = new JLabel(ip);
        mainPanel.add(label);

        JButton yesB = new JButton("yes");
        yesB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setYes(1);
                newWindow.dispatchEvent(new WindowEvent(newWindow, WindowEvent.WINDOW_CLOSING));
            }
        });
        mainPanel.add(yesB);
        JButton noB = new JButton("next");
        noB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setNo(1);
                hideWindow();
                newWindow.dispatchEvent(new WindowEvent(newWindow, WindowEvent.WINDOW_CLOSING));
            }
        });
        mainPanel.add(noB);

        JButton viewB = new JButton("yes,but view");
        viewB.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setView(1);
                newWindow.dispatchEvent(new WindowEvent(newWindow, WindowEvent.WINDOW_CLOSING));
            }
        });
        mainPanel.add(viewB);

        newWindow.getContentPane().add(mainPanel);

        newWindow.pack();
        newWindow.setFocusable(true);
        newWindow.setVisible(true);
    }
}
