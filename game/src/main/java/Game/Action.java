package Game;

import java.util.Map;
import java.util.HashMap;

public enum Action {
    STOP(0), UP(2), DOWN(8), LEFT(1), RIGHT(4);

    private short code;

    // Reverse-lookup map for getting an Game.Action from its code
    // A bit hilarious, actually that this feature is not implemented automatically in Java
    // TODO: maybe there exist a normal way - check it if there's time and necessity
    private static final Map<Short, Action> lookup = new HashMap<>();

    static {
        for (Action a : Action.values()) {
            lookup.put(a.getCode(), a);
        }
    }

    public static Action opposite(Action action) {
        if (action == Action.STOP) return Action.STOP;
        if (action == Action.UP) return Action.DOWN;
        if (action == Action.DOWN) return Action.UP;
        if (action == Action.LEFT) return Action.RIGHT;
        if (action == Action.RIGHT) return Action.LEFT;
        return null;
    }


    Action(int code) {
    	this.code = (short)code;
    }

    public short getCode() {
    	return code;
    }

    public static Action getByCode(short code) {
    	return lookup.get(code);
    }
}