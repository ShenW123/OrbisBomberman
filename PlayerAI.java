import static com.orbischallenge.bombman.api.game.MapItems.*;
import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;
import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Map;


/**
 *
 * @author c.sham
 */
public class PlayerAI implements Player {
    public static final int BOMB_V = 9;
    public static final int WALL_V = 1;
    public static final int BLANK_V = 0;
    //public static final int BLOCK_V = -3;
    private MapItems[][] map = new MapItems[16][16];
    List<Point> allBlocks;
    LinkedList<Move.Direction> movesSequence;
    int[][] heightMap = new int[17][17];

    /**
     * Gets called every time a new game starts.
     *
     * @param map The map.
     * @param blocks All the blocks on the map.
     * @param players Current position, bomb range, and bomb count for both Bombers.
     * @param playerIndex Your player index.
     */
    @Override
    public void newGame(MapItems[][] map, List<Point> blocks, Bomber[] players, int playerIndex) {
        allBlocks = blocks;
        movesSequence = new LinkedList<>();
        String temp="";
        for (int i = 1; i < 16; i++) {
            for (int j = 1; j < 16; j++) {
                switch(map[i][j]){
                    case WALL: heightMap[i][j] = WALL_V;
                        break;
                    case BLANK: heightMap[i][j] = BLANK_V;
                        break;
                    default: heightMap[i][j] = BLANK_V;
                        break;
                }
                temp += String.valueOf(heightMap[j][i] + " ");
            }
            System.out.println(temp);
            temp = "";
        }
        this.map = map;

    }

    /**
     * Gets called every time a move is requested from the game server.
     *
     * Provided is a very random and not smart AI which random moves without checking for
     * explosions, and places bombs whenever bombs can be used to destroy blocks.
     *
     * @param map The current map
     * @param bombLocations Bombs currently on the map and it's range, owner and time left Exploding
     * bombs are excluded.
     * @param powerUpLocations Power-ups current on the map and it's type
     * @param players Current position, bomb range, and bomb count for both Bombers
     * @param explosionLocations Explosions currently on the map.
     * @param playerIndex Your player index.
     * @param moveNumber The current move number.
     * @return the PlayerAction you want your Bomber to perform.
     */
    @Override
    public PlayerAction getMove(MapItems[][] map, HashMap<Point, Bomb> bombLocations, HashMap<Point, PowerUps> powerUpLocations, Bomber[] players, List<Point> explosionLocations, int playerIndex, int moveNumber) {

        this.map = map;

        /*for (Point p : allBlocks) {
            updateHeightMapBlocks(p);
        }*/

        updateHeightMapPlayers(players[1-playerIndex].position);

        for (Map.Entry entry : bombLocations.entrySet()) { 
            Point point = (Point) entry.getKey();
            Bomb bomb = (Bomb) entry.getValue();
            updateHeightMapBombs(point, bomb);
        }

        for(Point p: explosionLocations) {
            updateHeightMapExplosions(p);
            if (allBlocks.contains(p)) {
                allBlocks.remove(p);
            }
        }

        String temp="";
        for (int i = 1; i < 16; i++) {
            for (int j = 1; j < 16; j++) {
                temp += String.valueOf(heightMap[j][i] + "  ");
            }
            System.out.println(temp);
            temp = "";
        }
        boolean bombMove = false;
        /**
         * Get Bomber's current position
         */
        Point curPosition = players[playerIndex].position;
        int minScore = 100; /*heightMap[curPosition.x][curPosition.y];*/
        Move.Direction bestMove = new Move.Direction(0, 0, PlayerAction.STAYPUT, PlayerAction.PLACEBOMB);

        /**
         * Find which neighbours of Bomber's current position are currently unoccupied, so that I
         * can move into. Also counts how many blocks are neighbours.
         */
        LinkedList<Move.Direction> validMoves = new LinkedList<>();
        LinkedList<Move.Direction> blocks = new LinkedList<>();
        for (Move.Direction move : Move.getAllMovingMoves()) {
            int x = curPosition.x + move.dx;
            int y = curPosition.y + move.dy;

            if (map[x][y].isWalkable()) {
                validMoves.add(move);
                // TODO: hardcoded 14.
                if (heightMap[x][y] < 14){
                    if(minScore > heightMap[x][y]) {
                        minScore = heightMap[x][y];
                        bestMove = move;
                    }
                    if(heightMap[curPosition.x][curPosition.y] == -3) {
                        bombMove = true;
                    }
                } else {
                    bestMove = validMoves.get((int) (Math.random() * validMoves.size()));
                }
            }
            if (allBlocks.contains(new Point(x, y))) {
                blocks.add(move);
            }
        }

        /**
         * If there are blocks around, I should place a bomb in my current square.
         */
        if (!blocks.isEmpty()) {
            bombMove = true;
        }

        /**
         * There's no place to go, I'm stuck. :(
         */
        if (validMoves.isEmpty()) {
            System.out.println("I'm Dead");
            return Move.still.action;
        }

        /**
         * If I've got moves stored in movesSequence, use the oldest one in there (index 0). 
         * Else pick the first validMove available lol
         */
        //Move.Direction move = validMoves.get((int) (Math.random() * validMoves.size()));
        /*if (bombMove) {
            return bestMove.bombaction;
        }*/

        for(Point p: explosionLocations) {
            cleanBomb(p);
        }

        cleanPlayer(players[1-playerIndex].position);
        System.out.println(bestMove.action); 
        return bestMove.action;

    }

    /*private void updateHeightMapBlocks(Point p) {
        int x = p.x;
        int y = p.y;
        if(map[x-1][y].isWalkable()) {
            heightMap[x-1][y] = BLOCK_V;
        }
        if(map[x][y-1].isWalkable()) {
            heightMap[x][y-1] = BLOCK_V;
        }
        if(map[x+1][y].isWalkable()) {
            heightMap[x+1][y] = BLOCK_V;
        }
        if(map[x][y+1].isWalkable()) {
            heightMap[x][y+1] = BLOCK_V;
        }
    }*/
    private void updateHeightMapPlayers(Point p) {
        int x = p.x;
        int y = p.y;
        int range = 3;
        int score = 5;
        System.out.println("score " + score); 
        // Increase score of bomb coord. 
        heightMap[x][y] = score;
        for(int i = 1; i < range + 1; i++) {
            if(map[x-i][y].isWalkable() && heightMap[x-i][y] <= score-i) {
                heightMap[x-i][y] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x+i][y].isWalkable() && heightMap[x+i][y] <= score-i) {
                heightMap[x+i][y] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x][y-i].isWalkable() && heightMap[x][y-i] <= score-i) {
                heightMap[x][y-i] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x][y+i].isWalkable() && heightMap[x][y+i] <= score-i) {
                heightMap[x][y+i] = score - i;
            } else {
                break;
            }
        }
    }
    private void updateHeightMapBombs(Point p, Bomb bomb) {
        int x = p.x;
        int y = p.y;
        // TODO: score for chaining
        int score = 15 - bomb.getTimeleft();
        System.out.println("score " + score); 
        // Increase score of bomb coord. 
        heightMap[x][y] = score;
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x-i][y].isWalkable() && heightMap[x-i][y] == score-i-1) {
                heightMap[x-i][y] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x+i][y].isWalkable() && heightMap[x+i][y] == score-i-1) {
                heightMap[x+i][y] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x][y-i].isWalkable() && heightMap[x][y-i] == score-i-1) {
                heightMap[x][y-i] = score - i;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x][y+i].isWalkable() && heightMap[x][y+i] == score-i-1) {
                heightMap[x][y+i] = score - i;
            } else {
                break;
            }
        }
    }

    private void updateHeightMapExplosions(Point p) {
        heightMap[p.x][p.y] = 20;
    }
    private void cleanBomb(Point p) {
        heightMap[p.x][p.y] = BLANK_V;
    }
    private void cleanPlayer(Point p) {
        int x = p.x;
        int y = p.y;
        int range = 3;
        int score = 10;
        for(int i = 1; i < range + 1; i++) {
            if(map[x-i][y].isWalkable()) {
                heightMap[x-i][y] = BLANK_V;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x+i][y].isWalkable()) {
                heightMap[x+i][y] = BLANK_V;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x][y-i].isWalkable()) {
                heightMap[x][y-i] = BLANK_V;
            } else {
                break;
            }
        }
        for(int i = 1; i < range + 1; i++) {
            if(map[x][y+i].isWalkable()) {
                heightMap[x][y+i] = BLANK_V;
            } else {
                break;
            }
        }
    }

    /**
     * Uses Breadth First Search to find if a walkable path from point A to point B exists.
     *
     * This method does not consider the if tiles are dangerous or not. As long as all the tiles in
     * are walkable.
     *
     * @param start The starting point
     * @param end The end point
     * @param map The map use to check if a path exists between point A and point B
     * @return True if there is a walkable path between point A and point B, False otherwise.
     */
    public boolean isThereAPath(Point start, Point end, MapItems[][] map) {
        //Keeps track of points we have to check
        Queue<Point> open = new LinkedList<>();

        //Keeps track of points we have already visited
        List<Point> visited = new LinkedList<>();

        open.add(start);
        while (!open.isEmpty()) {
            Point curPoint = open.remove();

            //Check all the neighbours of the current point in question
            for (Move.Direction direction : Move.getAllMovingMoves()) {
                int x = curPoint.x + direction.dx;
                int y = curPoint.y + direction.dy;

                Point neighbour = new Point(x, y);

                // if the point is the destination, we are done
                if (end.equals(neighbour)) {
                    return true;
                }

                // if we have already visited this point, we skip it
                if (visited.contains(neighbour)) {
                    continue;
                }

                // if bombers can walk onto this point, then we add it to the list of points we should check
                if (map[x][y].isWalkable()) {
                    open.add(neighbour);
                }

                // add to visited so we don't check it again
                visited.add(neighbour);
            }
        }
        return false;
    }

    /**
     * Returns the Manhattan Distance between the two points.
     *
     * @param start the starting point
     * @param end the end point
     * @return the Manhattan Distance between the two points.
     */
    public int manhattanDistance(Point start, Point end) {
        return (Math.abs(start.x - end.x) + Math.abs(start.y - end.y));
    }
}