package SATSolver;

import Main.Config;
import Game.*;
import IPlanner.IPlanner;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SATPlanner implements IPlanner {

    private static final boolean testMode = false;
    private static final long threshold = 3000000;
    private boolean first = true;

    public IPlanner reset() {
        return this;
    }

    private void writeToFile(int timeLimit, BoardState state, List<SATClause> clauses) {
        try {
            Config config = Config.getInstance();
            PrintWriter w = new PrintWriter(new File(config.getMazeFile() + ".cnf"));

            w.println("p cnf " + timeLimit * state.colAmount * state.rowAmount + " " + clauses.size());
            for (SATClause clause : clauses) {
                int[] atoms = clause.atoms;
                for (int atom : atoms)
                    w.print(atom + " ");
                w.println("0");
            }

            w.close();
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Action> makePlan(BoardState state) {
        int lowerBound = state.remainingDotAmount;
        int upperBound = 2 * state.rowAmount * state.colAmount;
        Set<Integer> SATSolution = null;
        long startTime = System.currentTimeMillis();

        long totalTime = 0;

        while (upperBound  > lowerBound) {
            int candidate = (upperBound + lowerBound) / 2;
            Set<Integer> currentSol = notOptimalSATSolver(state, candidate);
            if (currentSol != null && currentSol.size() > 0) {
                upperBound = candidate;
                SATSolution = currentSol;
            }
            else
                lowerBound = candidate + 1;

            if (totalTime >= threshold && SATSolution != null)
                break;
        }

        if (SATSolution == null)
            return new ArrayList<>();

        int timeLimit = upperBound;
        int colAmount = state.colAmount;
        int rowAmount = state.rowAmount;

        boolean[] solBol = new boolean[timeLimit * colAmount * rowAmount];

        for (Integer a : SATSolution) {
            int index = Math.abs(a) - 1;
            boolean isTrue = a > 0;
            solBol[index] = isTrue;
        }

        boolean[][][] values = new boolean[timeLimit][colAmount][rowAmount];

        int curP = 0;

        for (int t = 0; t < timeLimit; t++)
            for (int c = 0; c < colAmount; c++)
                for (int r = 0; r < rowAmount; r++)
                    values[t][c][r] = solBol[curP++];

        Position[] pos = new Position[timeLimit];

        for (int t = 0; t < timeLimit; t++) {
            boolean posFound = false;
            for (int c = 0; c < colAmount && !posFound; c++)
                for (int r = 0; r < rowAmount && !posFound; r++)
                    if (values[t][c][r]) {
                        pos[t] = new Position(r, c);
                        posFound = true;
                    }
        }

        List<Action> result = new ArrayList<>();

        for (int t = 1; t < timeLimit; t++) {
            Position curPos = pos[t];
            Position oldPos = pos[t-1];

            if (curPos.x > oldPos.x)
                result.add(Action.RIGHT);
            else if (curPos.x < oldPos.x)
                result.add(Action.LEFT);
            else if (curPos.y > oldPos.y)
                result.add(Action.DOWN);
            else if (curPos.y < oldPos.y)
                result.add(Action.UP);
            else
                result.add(Action.STOP);
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Solved in " + elapsedTime + "ms amount of move is " + result.size());

        return result;
    }

    private Set<Integer> notOptimalSATSolver(BoardState state, int timeLimit)
    {
        short[] boardData = state.boardData;
        Position pacmanPosition = state.pacman.getCurrentPosition();
        List<Monster> monsters = state.monsters;
        int colAmount = state.colAmount;
        int rowAmount = state.rowAmount;

        int num = 0;
        int[][][] variables = new int[timeLimit][colAmount][rowAmount];

        for (int t = 0; t < timeLimit; t++)
            for (int c = 0; c < colAmount; c++)
                for (int r = 0; r < rowAmount; r++)
                    variables[t][c][r] = ++num;

        List<SATClause> clauses = new ArrayList<>();

        //Put constraints for each top wall (except boundaries)
        for (int r = 1 ; r < rowAmount; r++) {
            for (int c = 0; c < colAmount; c++) {
                int index = r * colAmount + c;
                if ((boardData[index] & 2) > 0) {
                    for (int t = 0; t < timeLimit - 1; t++) {
                        clauses.add(new SATClause(new int[] {variables[t][c][r] * -1, variables[t+1][c][r-1] * -1}));
                    }
                }
            }
        }

        //Put constraints for each bottom wall (except boundaries)
        for (int r = 0 ; r < rowAmount - 1; r++) {
            for (int c = 0; c < colAmount; c++) {
                int index = r * colAmount + c;
                if ((boardData[index] & 8) > 0) {
                    for (int t = 0; t < timeLimit - 1; t++) {
                        clauses.add(new SATClause(new int[] {variables[t][c][r] * -1, variables[t+1][c][r+1] * -1}));
                    }
                }
            }
        }

        //Put constraints for each left wall (except boundaries)
        for (int r = 0; r < rowAmount; r++) {
            for (int c = 1; c < colAmount; c++) {
                int index = r * colAmount + c;
                if ((boardData[index] & 1) > 0) {
                    for (int t = 0; t < timeLimit - 1; t++) {
                        clauses.add(new SATClause(new int[] {variables[t][c][r] * -1, variables[t+1][c-1][r] * -1}));
                    }
                }
            }
        }

        //Put constraints for each right wall (except boundaries)
        for (int r = 0; r < rowAmount; r++) {
            for (int c = 0; c < colAmount - 1; c++) {
                int index = r * colAmount + c;
                if ((boardData[index] & 4) > 0) {
                    for (int t = 0; t < timeLimit - 1; t++) {
                        clauses.add(new SATClause(new int[] {variables[t][c][r] * -1, variables[t+1][c+1][r] * -1}));
                    }
                }
            }
        }

        //Cannot be in more than one cell at a time
        int cellAmount = colAmount * rowAmount;
        for (int t = 0; t < timeLimit; t++) {
            for (int cell1 = 0; cell1 < cellAmount; cell1++) {
                for (int cell2 = cell1+1; cell2 < cellAmount; cell2++) {
                    int x1 = cell1 % colAmount;
                    int y1 = cell1 / colAmount;
                    int x2 = cell2 % colAmount;
                    int y2 = cell2 / colAmount;

                    clauses.add(new SATClause(new int[] {variables[t][x1][y1] * -1, variables[t][x2][y2] * -1}));
                }
            }
        }

        //Must be somewhere (cannot disappear)
        for (int t = 0; t < timeLimit; t++) {

            int[] arr = new int[cellAmount];

            for (int index = 0; index < cellAmount; index++) {
                int x1 = index % colAmount;
                int y1 = index / colAmount;
                arr[index] = variables[t][x1][y1];
            }

            clauses.add(new SATClause(arr));
        }

        //Can only move one cell horizontally or diagonally
        for (int t = 0; t < timeLimit - 1; t++)
            for (int r = 0; r < rowAmount; r++)
                for (int c = 0; c < colAmount; c++) {

                    List<Integer> elements = new ArrayList<>();
                    elements.add(variables[t][c][r] * -1);
                    elements.add(variables[t+1][c][r]);

                    if (r < rowAmount - 1)
                        elements.add(variables[t+1][c][r+1]);
                    if (r > 0)
                        elements.add(variables[t+1][c][r-1]);
                    if (c < colAmount - 1)
                        elements.add(variables[t+1][c+1][r]);
                    if (c > 0)
                        elements.add(variables[t+1][c-1][r]);

                    int[] arr = new int[elements.size()];
                    for (int p = 0; p < elements.size(); p++)
                        arr[p] = elements.get(p);

                    clauses.add(new SATClause(arr));
                }

        //Set initial position of pacman
        for (int c = 0; c < colAmount; c++)
            for (int r = 0; r < rowAmount; r++)
                if (c != pacmanPosition.x || r != pacmanPosition.y)
                    clauses.add(new SATClause(new int[] {variables[0][c][r] * -1}));

        clauses.add(new SATClause(new int[] {variables[0][pacmanPosition.x][pacmanPosition.y]}));

        //Each dot must be collected
        for (int index = 0; index < cellAmount; index++) {
            if ((boardData[index] & 16) > 0) {
                int[] arr = new int[timeLimit];
                int x = index % colAmount;
                int y = index / colAmount;

                for (int t = 0; t < timeLimit; t++)
                    arr[t] = variables[t][x][y];

                clauses.add(new SATClause(arr));
            }
        }

        try {
            //There must be no collisions (I'm not sure if this part is correct, I need to check it later)
            for (Monster currentMonster : monsters) {
                Position cPos = new Position(currentMonster.getCurrentPosition().y, currentMonster.getCurrentPosition().x);
                Action[] actions = currentMonster.giveActionsToDo(timeLimit);

                Position[] allPos = new Position[timeLimit];

                for (int t = 0; t < timeLimit; t++) {
                    allPos[t] = new Position(cPos.y, cPos.x);
                    cPos = Position.giveConsequence(cPos, actions[t]);
                }

                for (int t = 0; t < timeLimit - 1; t++) {
                    clauses.add(new SATClause(new int[]{variables[t][allPos[t].x][allPos[t].y] * -1}));
                    clauses.add(new SATClause(new int[]{variables[t][allPos[t + 1].x][allPos[t + 1].y] * -1, variables[t + 1][allPos[t].x][allPos[t].y] * -1}));
                }

                clauses.add(new SATClause(new int[]{variables[timeLimit - 1][allPos[timeLimit - 1].x][allPos[timeLimit - 1].y] * -1}));
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        Set<Integer> solution = null;

        try {
            if (testMode && first) {
                writeToFile(timeLimit, state, clauses);
                System.out.println("Model written to file");
                long startTime = System.currentTimeMillis();
                SATSolver mySolver = new SATSolver(clauses, timeLimit * colAmount * rowAmount);
                solution = mySolver.DPLL();
                long stopTime = System.currentTimeMillis();
                long elapsedTime = stopTime - startTime;
                System.out.println("Solved in : " + elapsedTime);
                first = false;
            }
            else {
                SATSolver mySolver = new SATSolver(clauses, timeLimit * colAmount * rowAmount);
                solution = mySolver.DPLL();
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        return solution;
    }
}