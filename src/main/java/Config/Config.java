package Config;

import IPlanner.IPlanner;

public class Config {

    //Determines which file should be used as maze file
    public static String mazeFile = "maze.m";

    //If true, one of planners will play the game, otherwise, user
    //can control Pacman by keyboard
    public static boolean isAutomatic = false;

    //If true, Reinforcement Learning will be used as planner
    public static boolean isOnlinePlanning = false;

    public static boolean areMonstersDeterministic = false;
    public static boolean isTraining = false;

    //For reinforcement learning
    public static int trainingEpisodes = 50;
    public static int testEpisodes = 10;

    public static IPlanner planner;
}
