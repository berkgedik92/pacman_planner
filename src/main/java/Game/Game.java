package Game;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFrame;

import AStarPlanner.AStarPlanner;
import Config.Config;
import SATSolver.SATPlanner;
import javaff.JavaFF;
import org.apache.commons.cli.*;

public class Game extends JFrame {

    public Game() throws Exception {

        //Load the maze from file
        Path path = Paths.get(getClass().getClassLoader().getResource(Config.mazeFile).toURI());
        List<String> lines = Files.readAllLines(path);

        //Get row amount, col amount and monster amount
        String[] firstLine = lines.get(0).split(",");
        int rowAmount = new Integer(firstLine[0]);
        int colAmount = new Integer(firstLine[1]);
        int monsterAmount = new Integer(firstLine[2]);
        short[] boardData = new short[rowAmount * colAmount];
        Config.areMonstersDeterministic = new Boolean(firstLine[3]);

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

        Board.setInstance(boardData, rowAmount, colAmount, initialPositions);

        //Get monster actions (if there is any)
        if (Config.areMonstersDeterministic) {
            List<Action[]> monsterActions = new ArrayList<>();
            for (int i = 0; i < monsterAmount; i++) {
                String[] moves = lines.get(i + rowAmount + 2).split(",");
                Action[] actions = new Action[moves.length];
                for (int j = 0; j < moves.length; j++)
                    actions[j] = Action.getByCode(Short.parseShort(moves[j]));
                monsterActions.add(actions);
            }
            Board.getInstance().setMonsterMoves(monsterActions);
        }

        if (Config.isOnlinePlanning) {
            Board.getState().pacman.getOnlinePlanner().train(Board.getState());
            Config.isTraining = false;
            Board.getState().pacman.getOnlinePlanner().test(Board.getState());
        }

        add(Board.getInstance());
        setTitle("Pacman Game");
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        //it was 380 * 420 for 15*15 board and 24px blockSize
        setSize(Board.blockSize * colAmount + 20, Board.blockSize * rowAmount + 60);
        setLocationRelativeTo(null);
        setVisible(true);

        Board.getInstance().startCycle();
    }

    public static void main(String[] args) {

        Options options = new Options();

        Option autoOption = new Option("a", "auto", false, "Use this argument to activate planner (so the game will be played automatically by the planner).");
        autoOption.setRequired(false);
        options.addOption(autoOption);

        Option mazeFileOption = new Option("m", "mazefile", true, "Path of maze file that will be used.");
        mazeFileOption.setRequired(true);
        options.addOption(mazeFileOption);

        Option plannerOption = new Option("p", "plannner", true, "The planner (astar/sat/ff/online)");
        plannerOption.setRequired(false);
        options.addOption(plannerOption);

        Option deterministicOption = new Option("d", "deterministic", false, "Decides whether the monsters are deterministic (can be used only with online planner)");
        deterministicOption.setRequired(false);
        options.addOption(deterministicOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = parser.parse(options, args);
            Config.isAutomatic = cmd.hasOption("auto");
            if (cmd.hasOption("mazefile")) {
                System.out.println("The following maze file will be used " + cmd.getOptionValue("mazefile"));
                Config.mazeFile = cmd.getOptionValue("mazefile");
            }
            if (cmd.hasOption("planner")) {
                String plannerStr = cmd.getOptionValue("planner");
                if (plannerStr.equals("astar")) {
                    System.out.println("A* algorithm will be used as planner");
                    Config.areMonstersDeterministic = true;
                    Config.planner = new AStarPlanner();
                }
                else if (plannerStr.equals("sat")) {
                    System.out.println("SAT solver algorithm will be used as planner");
                    Config.areMonstersDeterministic = true;
                    Config.planner = new SATPlanner();
                }
                else if (plannerStr.equals("ff")) {
                    System.out.println("JavaFF algorithm will be used as planner");
                    Config.areMonstersDeterministic = true;
                    Config.planner = new JavaFF();
                }
                else if (plannerStr.equals("online")) {
                    Config.isOnlinePlanning = true;
                    Config.areMonstersDeterministic = cmd.hasOption("deterministic");
                }
                else {
                    throw new ParseException("Invalid option for 'planner' parameter. Planner parameter must be astar, sat, ff or online");
                }
            }
        }
        catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }

        EventQueue.invokeLater(() -> {
            try {
                Game ex = new Game();
                ex.setVisible(true);
            }catch (Exception e) {
                System.err.println("cannot start the game, exiting");
                System.exit(-1);
            }
        });
    }
}