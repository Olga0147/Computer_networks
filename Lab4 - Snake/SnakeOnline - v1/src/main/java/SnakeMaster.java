import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class SnakeMaster extends SnakeManager {
    // food parameters
    private Integer foodStatic;
    private Integer foodPerPlayer;
    private Integer biggestId;

    public SnakeMaster(int fieldSizeX, int fieldSizeY, int foodPerPlayer, int foodStatic,HashMap<Integer,Snake> snakes, HashSet<Point> foods) {
        this.fieldSizeX = fieldSizeX;
        this.fieldSizeY = fieldSizeY;
        this.foodPerPlayer = foodPerPlayer;
        this.foodStatic = foodStatic;
        this.snakes = snakes;
        this.foods = foods;
    }

    private void findBiggestId(){
          int current = -1;
        for ( Integer k : snakes.keySet()) {
            if(k > current){ current = k;}
        }
        biggestId = current;
    }

    public Integer getId(){
        findBiggestId();
        biggestId+=1;
        return biggestId;
    }

    public boolean canAddSnake() {
        int field[][] = getField();
        // try to find free 5x5 square without food in center
        Point newHeadCell = new Point(-1, -1);
        for(int x = 0; x < fieldSizeX; ++x) {
            for(int y = 0; y < fieldSizeY; ++y) {
                Point centerPoint = new Point(x,y);
                if(
                        foods.contains(centerPoint) ||
                                foods.contains(clamp(new Point(centerPoint.x, centerPoint.y + 1))) &&
                                        foods.contains(clamp(new Point(centerPoint.x, centerPoint.y - 1))) &&
                                        foods.contains(clamp(new Point(centerPoint.x + 1, centerPoint.y))) &&
                                        foods.contains(clamp(new Point(centerPoint.x - 1, centerPoint.y)))
                ) {
                    continue;
                }
                boolean cellBusy = false;
                for(int offsetX = -2; offsetX <= 2; ++offsetX) {
                    for(int offsetY = -2; offsetY <= 2; ++offsetY) {
                        Point testPoint = clamp(new Point(x + offsetX, y + offsetY));
                        if(field[testPoint.x][testPoint.y] != -1) {
                            cellBusy = true;
                            break;
                        }
                    }
                    if(cellBusy) {
                        break;
                    }
                }
                if(!cellBusy) {
                    newHeadCell.x = x;
                    newHeadCell.y = y;
                    break;
                }
            }
            if(newHeadCell.x != -1) {
                break;
            }
        }
        // if square not found just return error
        if(newHeadCell.x == -1) {
            return false;
        }
        return true;
    }

    public int addSnake(Integer id) {
        int field[][] = getField();
        // try to find free 5x5 square without food in center
        Point newHeadCell = new Point(-1, -1);
        for(int x = 0; x < fieldSizeX; ++x) {
            for(int y = 0; y < fieldSizeY; ++y) {
                Point centerPoint = new Point(x,y);
                if(
                        foods.contains(centerPoint) ||
                                foods.contains(clamp(new Point(centerPoint.x, centerPoint.y + 1))) &&
                                        foods.contains(clamp(new Point(centerPoint.x, centerPoint.y - 1))) &&
                                        foods.contains(clamp(new Point(centerPoint.x + 1, centerPoint.y))) &&
                                        foods.contains(clamp(new Point(centerPoint.x - 1, centerPoint.y)))
                ) {
                    continue;
                }
                boolean cellBusy = false;
                for(int offsetX = -2; offsetX <= 2; ++offsetX) {
                    for(int offsetY = -2; offsetY <= 2; ++offsetY) {
                        Point testPoint = clamp(new Point(x + offsetX, y + offsetY));
                        if(field[testPoint.x][testPoint.y] != -1) {
                            cellBusy = true;
                            break;
                        }
                    }
                    if(cellBusy) {
                        break;
                    }
                }
                if(!cellBusy) {
                    newHeadCell.x = x;
                    newHeadCell.y = y;
                    break;
                }
            }
            if(newHeadCell.x != -1) {
                break;
            }
        }
        // if square not found just return error
        if(newHeadCell.x == -1) {
            return -1;
        }
        // otherwise randomly choose body cell position from free cells
        ArrayList<Point> newSnake = new ArrayList<>();
        newSnake.add(newHeadCell);
        Point offsets[] = { new Point(1,0), new Point(-1,0), new Point(0,1), new Point(0, -1)};
        for(int offsetIdxMin = 0; offsetIdxMin < 4; ++offsetIdxMin) {
            int offsetIdx = offsetIdxMin + (int) Math.round(Math.random() * (3 - offsetIdxMin));
            Point newBodyCell = clamp(new Point(newHeadCell.x + offsets[offsetIdx].x,newHeadCell.y + offsets[offsetIdx].y));
            if(field[newBodyCell.x][newBodyCell.y] == -1) {
                newSnake.add(newBodyCell);
                break;
            }
        }
        // add snake to snakes array
        snakes.put(id, new Snake(new Point(0,0),newSnake));
        // return success
        return 0;
    }

    public void iterate(HashMap<Integer,Point> steerDirections, HashMap<Integer,GameManager.Player> players) {
        // if game just started create food
        if(foods.size() == 0) {
            regenerateFoods();
        }
        // move snakes according to the rules
        moveSnakes(steerDirections, players);
        // and kill them if necessary
        killSnakes();
        // add food if needed
        regenerateFoods();
    }

    public Integer getFoodCount() {
        return foodStatic + foodPerPlayer * getAliveSnakesCount();
    }

    private void moveSnakes(HashMap<Integer,Point> steerDirections, HashMap<Integer,GameManager.Player> players) {
        // move snakes
        HashSet<Point> ateFoods = new HashSet<>();
        for (Integer snakeIdx:snakes.keySet()) {
            // if snake is dead then continue
            if(snakes.get(snakeIdx).p.size() < 2) {
                continue;
            }
            // determine snake crawling direction
            Point snakeHead = new Point(snakes.get(snakeIdx).p.get(0));
            Point snakeNeck = new Point(snakes.get(snakeIdx).p.get(1));
            Point direction = new Point();
            if (snakeHead.x - snakeNeck.x > 1) {
                direction.x = -1;
            } else if (snakeHead.x - snakeNeck.x < -1) {
                direction.x = 1;
            } else {
                direction.x = snakeHead.x - snakeNeck.x;
            }
            if (snakeHead.y - snakeNeck.y > 1) {
                direction.y = -1;
            } else if (snakeHead.y - snakeNeck.y < -1) {
                direction.y = 1;
            } else {
                direction.y = snakeHead.y - snakeNeck.y;
            }
            // steer snake of just let it move
            if (direction.x != 0) {

                direction.y += steerDirections.get(snakeIdx).y;
                if(steerDirections.get(snakeIdx).y != 0) {
                    direction.x = 0;
                }
            }
            if (direction.y != 0) {
                direction.x += steerDirections.get(snakeIdx).x;
                if(steerDirections.get(snakeIdx).x != 0) {
                    direction.y = 0;
                }
            }
            snakes.get(snakeIdx).lastDir = steerDirections.get(snakeIdx);
            // try to eat food
            Point newSnakeHead = clamp(new Point(snakeHead.x + direction.x, snakeHead.y + direction.y));
            boolean ateFood = false;
            for (int foodIdx = 0; foodIdx < foods.size(); ++foodIdx) {
                // if able to eat something then increase snake size
                if (foods.contains(newSnakeHead)) {
                    ateFoods.add(new Point(newSnakeHead));
                    snakes.get(snakeIdx).p.set(0, newSnakeHead);
                    snakes.get(snakeIdx).p.add(1, snakeHead);
                    ateFood = true;
                    if(players.containsKey(snakeIdx)){players.get(snakeIdx).add1Score();}
                    break;
                }
            }
            // if nothing ate then just move snake
            if (!ateFood) {
                snakes.get(snakeIdx).p.add(0, newSnakeHead);
                snakes.get(snakeIdx).p.remove(snakes.get(snakeIdx).p.size() - 1);
            }
            // ensure that each snake cell is in field bounds
            for(int snakeCellIdx = 0; snakeCellIdx < snakes.get(snakeIdx).p.size(); ++snakeCellIdx) {
                snakes.get(snakeIdx).p.set(snakeCellIdx, clamp(snakes.get(snakeIdx).p.get(snakeCellIdx)));
            }
        }
        // remove ate food
        for(Object food : ateFoods) {
            foods.remove(food);
        }
    }

    private void killSnakes() {
        HashSet<Integer> killedSnakes = new HashSet<>();
        for (Integer snakeIdx:snakes.keySet()){
            if(snakes.get(snakeIdx).p.size() < 2) {
                continue;
            }
            Point snakeHead = snakes.get(snakeIdx).p.get(0);
            boolean killed = false;
            for (Integer otherSnakeIdx:snakes.keySet()){
                for(int otherSnakeCellIdx = 0; otherSnakeCellIdx < snakes.get(otherSnakeIdx).p.size(); ++otherSnakeCellIdx) {
                    if(otherSnakeIdx == snakeIdx && otherSnakeCellIdx == 0) {
                        continue;
                    }
                    if(snakes.get(otherSnakeIdx).p.get(otherSnakeCellIdx).equals(snakeHead)) {
                        killedSnakes.add(snakeIdx);
                        killed = true;
                        break;
                    }
                }
                if(killed) {
                    break;
                }
            }
        }
        for(Integer killIdx : killedSnakes) {
            snakes.get(killIdx).p.clear();
        }
    }

    private void regenerateFoods() {
        ArrayList<Point> freeCells = getFreeCells();
        while(foods.size() < getFoodCount() && freeCells.size() > 0) {
            int newFoodIdx =  (int)Math.round(Math.random() * (freeCells.size() - 1));
            foods.add(new Point(freeCells.get(newFoodIdx)));
            freeCells.remove(newFoodIdx);
        }
    }

    public boolean snakeEmpty(int id){
        return snakes.get(id).p.isEmpty();
    }
}
