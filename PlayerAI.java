
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
    public static final int BLOCK_V = -3;
    private MapItems[][] map = new MapItems[16][16];
    List<Point> allBlocks;
    int[][] heightMap = new int[17][17];
    private Point goalPoint;
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

        for (Map.Entry entry : bombLocations.entrySet()) { 
            Point point = (Point) entry.getKey();
            Bomb bomb = (Bomb) entry.getValue();
            updateHeightMapBombs(point, bomb);
        }

        for(Point p: explosionLocations) {
            if (allBlocks.contains(p)) {
                allBlocks.remove(p);
            }
            updateHeightMapExplosions(p);
        }

        for (Point p : allBlocks) {
            updateHeightMapBlocks(p);
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
        LinkedList<Point> path = findGoal(curPosition, map);
        int minScore = 100;
        int minX = 100;
        int minY = 100;
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
                // TODO: hardcoded 14.
                if(!path.isEmpty()) {
                    Point suggested = path.pop();
                    if(x == suggested.x && y == suggested.y) {
                        bestMove = move;
                        System.out.println("BestMove = " + suggested);
                    } else if (heightMap[x][y] < 14){
                        if(minScore > heightMap[x][y]) {
                            minScore = heightMap[x][y];
                            bestMove = move;
                        }
                        validMoves.add(move);
                        if(heightMap[curPosition.x][curPosition.y] == -3) {
                            bombMove = true;
                        }
                    }
                }
            }
            if (allBlocks.contains(new Point(x, y))) {
                blocks.add(move);
            }
        }
        /**
         * There's no place to go, I'm stuck. :(
         */
        if (validMoves.isEmpty()) {
            System.out.println("I'm Dead");
            return Move.still.action;
        }

        for(Point p: explosionLocations) {
            cleanBomb(p);
        }
        /**
         * If I've got moves stored in movesSequence, use the oldest one in there (index 0). 
         * Else pick the first validMove available lol
         */
        //Move.Direction move = validMoves.get((int) (Math.random() * validMoves.size()));
        /*if (bombMove) {
            return bestMove.bombaction;
        }*/
        System.out.println(bestMove.action); 
        return bestMove.action;

    }

    private boolean hasGetAway(Point p) {
        int x = p.x;
        int y = p.y;
        if(map[x][y].isWalkable()) {
            if(map[x-1][y].isWalkable() && heightMap[x-1][y] <= 0) {
                return true;
            }
            if(map[x+1][y].isWalkable() && heightMap[x+1][y] <= 0) {
                return true;
            }
            if(map[x][y-1].isWalkable() &&heightMap[x][y-1] <= 0) {
                return true;
            }
            if(map[x][y+1].isWalkable() &&heightMap[x][y+1] <= 0) {
                return true;
            }
        }
        return false;
    }

    private void updateHeightMapExplosions(Point p) {
        int x = p.x;
        int y = p.y;
        heightMap[x][y] = 50;
    }
    private void updateHeightMapBlocks(Point p) {
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
            if(map[x-i][y].isWalkable()) {
                heightMap[x-i][y] = score;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x+i][y].isWalkable()) {
                heightMap[x+i][y] = score;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x][y-i].isWalkable()) {
                heightMap[x][y-i] = score;
            } else {
                break;
            }
        }
        for(int i = 1; i < bomb.getRange() + 1; i++) {
            if(map[x][y+i].isWalkable()) {
                heightMap[x][y+i] = score;
            } else {
                break;
            }
        }
    }

    private void cleanBomb(Point p) {
        heightMap[p.x][p.y] = BLANK_V;
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
    public LinkedList<Point> findGoal(Point start, MapItems[][] map) {
        //Keeps track of points we have to check
        Queue<Node> open = new LinkedList<>();

        //Keeps track of points we have already visited
        LinkedList<Node> visited = new LinkedList<>();

        LinkedList<Point> path = new LinkedList<>();
        path.add(start);
        open.add(new Node(null, start));
        Node neighbour = new Node(null, start);
        lala:
        while (!open.isEmpty()) {
            Node curNode = open.remove();

            //Check all the neighbours of the current point in question
            for (Move.Direction direction : Move.getAllMovingMoves()) {
                int x = curNode.getPoint().x + direction.dx;
                int y = curNode.getPoint().y + direction.dy;

                neighbour = new Node(curNode, new Point (x, y));

                // if the point is the destination, we are done
                if (map[neighbour.getPoint().x][neighbour.getPoint().y].isWalkable() && heightMap[neighbour.getPoint().x][neighbour.getPoint().y] <= -3) {
                    break lala;
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
        if(!visited.isEmpty()) {
            while (neighbour.getParent() != null) {
                path.add(neighbour.getPoint());
                System.out.println("Path " + neighbour.getPoint()); 
                neighbour = neighbour.getParent();
    /*            visited.pop();*/
                
            } 
        }

        return path;
    }

    public class Node {
        private Node mParent;
        private Point mPoint;
        public Node (Node parent, Point p) {
            mParent = parent;
            mPoint = p;
        }

        public Node getParent() {
            return mParent;
        }

        public Point getPoint() {
            return mPoint;
        }

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
