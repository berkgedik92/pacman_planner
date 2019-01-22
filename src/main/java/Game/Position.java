package Game;

import java.lang.Math;

/**
 * Created by USER on 9/30/2016.
 */
public class Position {

    public int x; //left to right
    public int y; //top to bottom

    public Position(int y, int x) {
        this.x = x;
        this.y = y;
    }
    
    public Position(String pos) {
    	String[] parsedPos = pos.split("_");
        this.x = Integer.parseInt(parsedPos[1]);
        this.y = Integer.parseInt(parsedPos[2]);
    }

    public Position(Position oPos) {
    	this(oPos.y, oPos.x);
    }

    public static Position giveConsequence(Position pos, Action act) {
        Position result = new Position(pos.y, pos.x);

        switch (act) {
            case DOWN: result.y++; break;
            case UP: result.y--; break;
            case LEFT: result.x--; break;
            case RIGHT: result.x++; break;
        }

        return result;
    }

    public int manhattanDistance(Position pos) {
        return Math.abs(x - pos.x) + Math.abs(x - pos.y);
    }

    // seems redundant now??
    public static boolean isEqual(Position pos1, Position pos2) {
        return (pos1.x == pos2.x) && (pos1.y == pos2.y);
    }


    // equals and hashCode overrides are necessary for Position HashSet functioning
    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;

        if (obj == null || obj.getClass() != this.getClass())
            return false;

        Position pos = (Position)obj;
        return (x == pos.x) && (y == pos.y);
    }

    @Override
    public int hashCode() {
        return 41 * x + 11 * y;
    }

    public String toString() {
        return x + " " + y;
    }
}
