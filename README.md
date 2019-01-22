# pacman_planner
Intelligent agents playing Pacman game. Implemented in Java

Running the Game
----------------
The main class is called Game and can be found in the Game folder under src. If testing a planning algorithm, 
run the program with the argument: auto. Apart from that, here are the other program arguments:

-m/mazefile <"Path of maze file that will be used">
-p/plannner <"The planner (astar/sat/ff/online)")>
-d/deterministic" (Decides whether the monsters are deterministic (can be used only with online planner)");

Maze definement
----------------

The maze that the application will use is defined in the file "maze.m" (in src folder). 
The first line defines the row (r), column (c), monster (m) amount in the game and whether monsters moves are deterministic (true or false),
The second line gives the initial position of creatures (pacmanY, pacmanX, monster1Y, monster1X, ...)
The next r lines define the cells of a row (from top to bottom). Here is the information about meaning of numbers:

1 = the cell has a left wall
2 = the cell has a top wall
4 = the cell has a right wall
8 = the cell has a bottom wall
16 = the cell has a dot

Those numbers can be combined in order to make cells having more than one property at the same time. For example, if we define a cell with number 26 (16+8+2), this cell will have bottom wall, top wall and a dot at the same time.

Important remark: Let's say A and B are two neighboring cells (Let's assume A is at the left of B). Let's say B has a left wall. In this case, a creature cannot pass from B to A. However if A does not have a right wall, then a monster can pass from A to B. So B having a left wall does not imply that A having a right wall. When constructing valid mazes, one should pay attention to this fact.

The next m lines define monsters deterministic behaviour using the same codes as defined above, i.e.:
0 = stop
1 = move left
2 = move up
4 = move right
8 = move bottom

Keyboard controlled game: To make stop move press N, to make pacman move any direction use arrow keys

Running the game with automatic mode: If the program is run with "auto" argument, the pacman will not listen keyboard commands and move according to its "intelligent planner."