package Main;

import java.awt.EventQueue;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import Game.*;
import AStarPlanner.AStarPlanner;
import SATSolver.SATPlanner;
import javaff.JavaFF;
import org.apache.commons.cli.*;

public class Game extends JFrame {

    public Game() throws Exception {

        Config config = Config.getInstance();

        //Load the maze from file
        Path path = Paths.get(getClass().getClassLoader().getResource(config.getMazeFile()).toURI());
        List<String> lines = Files.readAllLines(path);

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

        Board.setInstance(boardData, rowAmount, colAmount, initialPositions);

        //Get monster actions (if there is any)
        if (config.isMonstersDeterministic()) {
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

        if (config.isOnlinePlanning()) {
            Board.getState().pacman.getOnlinePlanner().train(Board.getState());
            config.markTrainingFinished();
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