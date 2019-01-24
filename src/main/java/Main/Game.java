package Main;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import Game.*;

public class Game extends JFrame implements Runnable, IBoardStateObserver {

    private boolean isFinished = false;
    private Thread thread;

    @Override
    public void initialize(int rowAmount, int colAmount) {}

    @Override
    public void stateUpdateSignal(Position pacmanPosition, List<Position> monsterPositions, short[] boardData) { }

    @Override
    public void finishSignal() {
        isFinished = true;
    }

    /*How much milliseconds should we wait at least before painting the next frame
        (I'm saying at least because if creatures decide their action late, we will wait for them
        so waiting time might be longer*/
    private static final int frameDelay = 400;
    private BoardState state;

    @Override
    public void run() {
        while (!isFinished) {
            try {
                //Make pacman make a decision
                Config config = Config.getInstance();
                state.pacman.makeDecision(config.isAutomatic() ? null : Board.getInstance().moveAgentByKeyboard(state), state);

                //Make all monsters make a decision
                for (int i = 0; i < state.monsterAmount; i++)
                    state.monsters.get(i).makeDecision(state);

                //Check the maze after to see what happened after all actions (collision, death, dot collection etc)
                state.checkMaze();
                state.pushToObservers();
                Thread.sleep(frameDelay);
            } catch (Exception e) {
                System.err.println(e.toString());
                isFinished = true;
            }
        }
    }

    private Game() {

        Config config = Config.getInstance();
        List<String> lines;

        try {
            lines = Files.readAllLines(Paths.get(config.getMazeFile()));
        }
        catch (Exception e) {
            throw new RuntimeException("Could not read the maze file");
        }

        //Get row amount, col amount and monster amount
        String[] firstLine = lines.get(0).split(",");
        int rowAmount = new Integer(firstLine[0]);
        int colAmount = new Integer(firstLine[1]);
        int monsterAmount = new Integer(firstLine[2]);
        short[] boardData = new short[rowAmount * colAmount];

        //Get initial Pacman and monster positions
        String[] secondLine = lines.get(1).split(",");
        List<Position> initialPositions = new ArrayList<>();
        for (int i = 0; i < monsterAmount + 1; i++) {
            int yPos = new Integer(secondLine[i * 2]);
            int xPos = new Integer(secondLine[i * 2 + 1]);
            initialPositions.add(new Position(yPos, xPos));
        }

        //Read board data (walls and dots)
        for (int i = 0; i < rowAmount; i++) {
            String[] cellData = lines.get(i+2).split(",");
            for (int y = 0; y < colAmount; y++)
                boardData[i * colAmount + y] = Short.parseShort(cellData[y]);
        }

        state = new BoardState(colAmount, rowAmount, boardData, initialPositions);
        state.attachObserver(Board.getInstance());

        //Get monster actions (if there is any and if we run in deterministic mode)
        if (config.isMonstersDeterministic()) {
            List<Action[]> monsterActions = new ArrayList<>();
            for (int i = 0; i < monsterAmount; i++) {
                String[] moves = lines.get(i + rowAmount + 2).split(",");
                Action[] actions = new Action[moves.length];
                for (int j = 0; j < moves.length; j++)
                    actions[j] = Action.getByCode(Short.parseShort(moves[j]));
                monsterActions.add(actions);
            }
            state.setMonsterMoves(monsterActions);
        }

        if (config.isOnlinePlanning()) {
            try {
                state.pacman.getOnlinePlanner().train(state);
                config.markTrainingFinished();
                state.pacman.getOnlinePlanner().test(state);
            }
            catch (Exception e) {
                throw new RuntimeException("Exception on OnlinePlanner : " + e.toString());
            }
        }

        add(Board.getInstance());
        setTitle("Pacman Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //it was 380 * 420 for 15*15 board and 24px blockSize
        setSize(config.getBlockSize() * colAmount + 20, config.getBlockSize() * rowAmount + 60);
        setLocationRelativeTo(null);
        setVisible(true);

        thread = new Thread(this, "Game Cycle Thread");
        thread.start();
    }

    public static void main(String[] args) {

        Config.setByProgramArguments(args);

        EventQueue.invokeLater(() -> {
            Game ex = new Game();
            ex.setVisible(true);
        });
    }
}