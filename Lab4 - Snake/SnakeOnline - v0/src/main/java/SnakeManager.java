import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;

public class SnakeManager {
    // field parameters
    protected Integer fieldSizeX;
    protected Integer fieldSizeY;
    // snakes array, each snake starts from head
    protected ArrayList<ArrayList<Point>> snakes;
    // food positions
    protected HashSet<Point> foods;

    public void iterate(ArrayList<Point> steerDirections) {

    }

    public HashSet<Point> getFoods() {
        return new HashSet<>(foods);
    }

    public ArrayList<ArrayList<Point>> getSnakes() {
        return new ArrayList<>(snakes);
    }

    public int getSnakesCount() {
        return snakes.size();
    }

    public int getAliveSnakesCount() {
        int count = 0;
        for (ArrayList<Point> snake : snakes) {
            if (snake.size() >= 2) {
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
        for(int snakeIdx = 0; snakeIdx < snakes.size(); ++snakeIdx) {
            for (int snakeCellIdx = 0; snakeCellIdx < snakes.get(snakeIdx).size(); ++snakeCellIdx) {
                field[snakes.get(snakeIdx).get(snakeCellIdx).x][snakes.get(snakeIdx).get(snakeCellIdx).y] = snakeIdx;
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
}
