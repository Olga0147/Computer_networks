import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class SnakeManager {
    // field parameters
    protected Integer fieldSizeX;
    protected Integer fieldSizeY;
    // snakes array, each snake starts from head
    protected HashMap<Integer,Snake> snakes;
    // food positions
    protected HashSet<Point> foods;
    //snake
    public static class Snake{
        Point lastDir;
        ArrayList<Point> p;

        public Snake(Point point, ArrayList<Point> newSnake) {
            this.lastDir = point;
            this.p = newSnake;
        }
    }

    public void iterate(HashMap<Integer,Point> steerDirections, HashMap<Integer,GameManager.Player> players) {

    }

    public HashSet<Point> getFoods() {
        return new HashSet<>(foods);
    }

    public HashMap<Integer,Snake> getSnakes() {
        return new HashMap<>(snakes);
    }

    public int getSnakesCount() {
        return snakes.size();
    }

    public int getAliveSnakesCount(Set<Integer> players) {
        int count = 0;
        for (Integer k : snakes.keySet()) {
            if (snakes.get(k).p.size() >= 2 && players.contains(k)) {
                ++count;
            }
        }
        return count;
    }

    public int getAliveSnakesCount() {
        int count = 0;
        for (Integer k : snakes.keySet()) {
            if (snakes.get(k).p.size() >= 2 ) {
                ++count;
            }
        }
        return count;
    }

    public int[][] getField() {
        int field [][] = new int[fieldSizeX][fieldSizeY];
        for(int x = 0; x < fieldSizeX; ++x) {
            for(int y = 0; y < fieldSizeY; ++y) {
                field[x][y] = -1;
            }
        }
        for (Integer snakeIdx:snakes.keySet()){
            for (int snakeCellIdx = 0; snakeCellIdx < snakes.get(snakeIdx).p.size(); ++snakeCellIdx) {
                field[snakes.get(snakeIdx).p.get(snakeCellIdx).x][snakes.get(snakeIdx).p.get(snakeCellIdx).y] = snakeIdx;
            }
        }
        return field;
    }

    public ArrayList<Point> getFreeCells() {
        int[][] field = getField();
        ArrayList<Point> freeCells = new ArrayList<>();
        for(int x = 0; x < fieldSizeX; ++x) {
            for(int y = 0; y < fieldSizeY; ++y) {
                if(field[x][y] == -1) {
                    freeCells.add(new Point(x,y));
                }
            }
        }
        return freeCells;
    }

    protected int clampX(int x) {
        return (fieldSizeX + x) % fieldSizeX;
    }

    protected int clampY(int y) {
        return (fieldSizeY + y) % fieldSizeY;
    }

    protected Point clamp(Point p) {
        p.x = (fieldSizeX + p.x) % fieldSizeX;
        p.y = (fieldSizeY + p.y) % fieldSizeY;
        return p;
    }

    public boolean snakeEmpty(int id){
        return snakes.get(id).p.isEmpty();
    }
}
