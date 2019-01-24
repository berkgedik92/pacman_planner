package Main;

import AStarPlanner.AStarPlanner;
import IPlanner.IPlanner;
import SATSolver.SATPlanner;
import javaff.JavaFF;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.cli.*;

@Getter
@NoArgsConstructor
public class Config {

    private static Config instance = null;

    //Determines which file should be used as maze file
    private String mazeFile = "maze.m";

    //If true, one of planners will play the game, otherwise, user
    //can control Pacman by keyboard
    private boolean isAutomatic = false;

    //If true, Reinforcement Learning will be used as planner
    private boolean isOnlinePlanning = false;

    private boolean isMonstersDeterministic = false;
    private boolean isTraining = true;

    //For reinforcement learning
    private int trainingEpisodes = 50;
    private int testEpisodes = 10;

    private IPlanner planner;

    static void setByProgramArguments(String[] args) {
        Options options = new Options();

        //TODO: Add trainingEpisodes and testEpisodes to program arguments

        Option autoOption = new Option("a", "auto", false, "Use this argument to activate planner (so the game will be played automatically by the planner).");
        autoOption.setRequired(false);
        options.addOption(autoOption);

        Option mazeFileOption = new Option("m", "mazefile", true, "Path of maze file that will be used.");
        mazeFileOption.setRequired(true);
        options.addOption(mazeFileOption);

        Option plannerOption = new Option("p", "planner", true, "The planner (astar/sat/ff/online)");
        plannerOption.setRequired(false);
        options.addOption(plannerOption);

        Option deterministicOption = new Option("d", "deterministic", false, "Decides whether the monsters are deterministic (can be used only with online planner)");
        deterministicOption.setRequired(false);
        options.addOption(deterministicOption);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();

        try {
            Config parsedConfiguration = new Config();

            CommandLine cmd = parser.parse(options, args);
            parsedConfiguration.isAutomatic = cmd.hasOption("auto");

            if (cmd.hasOption("mazefile")) {
                System.out.println("The following maze file will be used " + cmd.getOptionValue("mazefile"));
                parsedConfiguration.mazeFile = cmd.getOptionValue("mazefile");
            }

            if (cmd.hasOption("planner")) {
                String plannerStr = cmd.getOptionValue("planner");
                switch (plannerStr) {
                    case "astar":
                        System.out.println("A* algorithm will be used as planner");
                        parsedConfiguration.isMonstersDeterministic = true;
                        parsedConfiguration.planner = new AStarPlanner();
                        break;
                    case "sat":
                        System.out.println("SAT solver algorithm will be used as planner");
                        parsedConfiguration.isMonstersDeterministic = true;
                        parsedConfiguration.planner = new SATPlanner();
                        break;
                    case "ff":
                        System.out.println("JavaFF algorithm will be used as planner");
                        parsedConfiguration.isMonstersDeterministic = true;
                        parsedConfiguration.planner = new JavaFF();
                        break;
                    case "online":
                        parsedConfiguration.isOnlinePlanning = true;
                        parsedConfiguration.isMonstersDeterministic = cmd.hasOption("deterministic");
                        break;
                    default:
                        throw new ParseException("Invalid option for 'planner' parameter. Planner parameter must be astar, sat, ff or online");
                }
            }

            Config.instance = parsedConfiguration;
        }
        catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        }
    }

    public static Config getInstance() {
        if (instance == null)
            throw new RuntimeException("The configuration is not set yet. A setter method must be called before to set properties before getInstance function is called.");
        return instance;
    }

    /*
        TODO: Change the implementation so this function will not be needed (isTraining is not part of
         configuration object it should be moved to OnlinePlanner class)
     */

    public void markTrainingFinished() {
        this.isTraining = false;
    }

    // TODO: Remove this
    public IPlanner getPlanner() {
        return planner;
    }

    // TODO: Remove this
    public void setPlanner(IPlanner planner) {
        this.planner = planner;
    }
}
