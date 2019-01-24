package Game;

/**
 * Created by USER on 5/29/2017.
 */
public class GameCycle extends Thread{

    private static GameCycle instance = null;
    private boolean isFinished = false;

    public static GameCycle getInstance() {
        if (instance == null)
            instance = new GameCycle();
        return instance;
    }

    private GameCycle() {}

    public void finish() {
        isFinished = true;
    }

    /*How much milliseconds should we wait at least before painting the next frame
    (I'm saying at least because if creatures decide their action late, we will wait for them
    so waiting time might be longer*/
    private static final int frameDelay = 400;

    @Override
    public void run() {
        while (!isFinished) {
            try {
                Board.getInstance().playTurn();
                Thread.sleep(frameDelay);
            } catch (Exception e) {
                System.err.println(e.toString());
                isFinished = true;
            }
        }
    }
}
