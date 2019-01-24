package Main;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import Game.*;
import OnlinePlanner.ApproximateQPlanner;
import org.apache.commons.cli.*;

public class Game extends JFrame implements Runnable, IBoardStateObserver {

    private boolean isFinished = false;
    private Thread thread;
    private Config config;
    private Board board;

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
                state.pacman.makeDecision(config.getBoolean("ai_enabled") ? null : Board.getInstance().moveAgentByKeyboard(state), state);

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

        config = Config.getInstance();
        List<String> lines;

        try {
            System.out.println(config.getString("maze_file"));
            lines = Files.readAllLines(Paths.get(config.getString("maze_file")));
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

        board = Board.getInstance();
        state = new BoardState(colAmount, rowAmount, boardData, initialPositions);
        state.attachObserver(board);
        state.attachObserver(this);

        //Get monster actions (if there is any and if we run in deterministic mode)
        if (config.getBoolean("deterministic_monsters")) {
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

        if (config.getBoolean("online_planning")) {
            try {
                ApproximateQPlanner planner = (ApproximateQPlanner)state.pacman.getPlanner();
                planner.train(state);
            }
            catch (Exception e) {
                throw new RuntimeException("Exception on OnlinePlanner : " + e.toString());
            }
        }

        add(board);
        setTitle("Pacman Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //it was 380 * 420 for 15*15 board and 24px blockSize
        int blockSize = config.getInt("block_size");
        setSize(blockSize * colAmount + 20, blockSize * rowAmount + 60);
        setLocationRelativeTo(null);
        setVisible(true);

        thread = new Thread(this, "Game Cycle Thread");
        thread.start();
    }

    public static CommandLine parseArgs(String[] args) throws ParseException {
        Options options = new Options();

        Option configOption = new Option("c", "config", true, "A configuration file (in YAML format)");
        configOption.setRequired(true);
        options.addOption(configOption);

        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }

    public static void main(String[] args) {
        try {
            CommandLine parsedArgs = parseArgs(args);
            Config.load(parsedArgs.getOptionValue("config"));
        } catch (ParseException e) {
            System.err.println("Couldn't parse command line arguments.");
            System.exit(1);
        }

        EventQueue.invokeLater(() -> {
            Game ex = new Game();
            ex.setVisible(true);
        });
    }
}