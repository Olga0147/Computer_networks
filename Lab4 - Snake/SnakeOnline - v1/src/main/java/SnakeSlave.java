import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SnakeSlave extends SnakeManager {
    public SnakeSlave(int fieldSizeX, int fieldSizeY, HashMap<Integer,Snake> snakes, HashSet<Point> foods) {
        this.fieldSizeX = fieldSizeX;
        this.fieldSizeY = fieldSizeY;
        this.snakes = snakes;
        this.foods = foods;
    }

    public void setState(HashMap<Integer, Snake> snakes, HashSet<Point> foods) {
        this.snakes = new HashMap<>(snakes);
        this.foods = new HashSet<>(foods);
    }
}
