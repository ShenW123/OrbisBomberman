
import static com.orbischallenge.bombman.api.game.MapItems.*;
import com.orbischallenge.bombman.api.game.MapItems;
import com.orbischallenge.bombman.api.game.PlayerAction;
import com.orbischallenge.bombman.api.game.PowerUps;
import java.awt.Point;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;


/**
 *
 * @author c.sham
 */
public class PlayerAI implements Player {
    public static final int BOMB_V = 10;
    public static final int WALL_V = -1;
    public static final int BLANK_V = 0;
    List<Point> allBlocks;
    LinkedList<Move.Direction> movesSequence;
    int[][] m_map = new int[17][17];

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
                    case WALL: m_map[i][j] = WALL_V;
                        break;
                    case BLANK: m_map[i][j] = BLANK_V;
                        break;
                    default: m_map[i][j] = BLANK_V;
                        break;
                }
                temp += String.valueOf(m_map[j][i] + " ");
            }
            System.out.println(temp);
            temp = "";
        }

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
        String temp="";
        for (int i = 1; i < 16; i++) {
            for (int j = 1; j < 16; j++) {
                switch(m_map[i][j]){
                    case BOMB_V:
                        if(map[i][j] != BOMB) {
                            m_map[i][j] = BLANK_V;
                            m_map[i-1][j] = BLANK_V;
                            m_map[i][j-1] = BLANK_V;
                            m_map[i+1][j] = BLANK_V;
                            m_map[i][j+1] = BLANK_V;
                        }
                        break;
                }
                switch(map[i][j]){
                    case BOMB: 
                        m_map[i][j] = BOMB_V;
                        m_map[i-1][j] = BOMB_V - 1;
                        m_map[i][j-1] = BOMB_V - 1;
                        m_map[i+1][j] = BOMB_V - 1;
                        m_map[i][j+1] = BOMB_V - 1;
                }
                temp += String.valueOf(m_map[j][i] + "    ");
            }
            System.out.println(temp);
            temp = "";
        }


        boolean bombMove = false;
        /**
         * Get Bomber's current position
         */
        Point curPosition = players[playerIndex].position;

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
                if (m_map[x][y] < 9){
                validMoves.add(move);
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
            if (movesSequence.size() != 0)
            {
            bombMove = true;
            }
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
        Move.Direction move = validMoves.get((int) (Math.random() * validMoves.size()));
        return move.action;

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
