package Game;

import Main.Config;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JPanel;

public class Board extends JPanel {

    private Image ii;

    /*Colors that we use in the game*/
    private final Color dotColor    = new Color(192, 192, 0); //Color of dots
    private final Color mazeColor   = new Color(5, 100, 5);   //Color of borders
    private final Color cellColor   = new Color(0, 0, 0);     //Color of cells and background

    /*How px a cell should be (width and height), our image file has image with size
    22*22 and line thickness is 1 so this must be 22 + 1 + 1 = 24 */
    public static final int blockSize = 24;

    /*Variables about the screenSize (width = colAmount * blockSize and height = rowAmount * blockSize)*/
    private int screenWidth;
    private int screenHeight;

    /*Images*/
    private Image ghostImage;
    private Image pacmanImage;

    /*Necessary objects for turn-based game*/
    private final GameCycle cycle = GameCycle.getInstance();
    private final Object lock = new Object();

    private Action selectedAction = null;
    public BoardState state;
    private static Board instance = null;

    public static Board getInstance() {
        return instance;
    }

    public static BoardState getState() {
        return getInstance().state;
    }

    public static void setInstance(short[] boardData, int rowAmount, int colAmount, List<Position> initialPos) {
        instance = new Board(boardData, rowAmount, colAmount, initialPos);
    }

    private Board(short[] boardData, int rowAmount, int colAmount, List<Position> initialPos) {

        //Load Images
        ghostImage = new ImageIcon(getClass().getClassLoader().getResource("ghost.png")).getImage();
        pacmanImage = new ImageIcon(getClass().getClassLoader().getResource("pacman.png")).getImage();

        this.screenWidth = colAmount * blockSize;
        this.screenHeight = rowAmount * blockSize;

        this.state = new BoardState(colAmount, rowAmount, boardData, initialPos);

        addKeyListener(new TAdapter());
        setFocusable(true);
        setBackground(cellColor);
        setDoubleBuffered(true);
    }

    public void setMonsterMoves(List<Action[]> monsterActions) {
        state.setMonsterMoves(monsterActions);
    }


    //////////////////////////////////////////////
    // UI methods. Just DON'T TOUCH OR USE THEM //
    //////////////////////////////////////////////

    private void playGame() throws Exception {
        //Make pacman make a decision
        Config config = Config.getInstance();
        state.pacman.makeDecision(config.isAutomatic() ? null : moveAgentByKeyboard(state.pacman));

        //Make all monsters make a decision
        for (int i = 0; i < state.monsterAmount; i++)
            state.monsters.get(i).makeDecision(state.boardData);

        //Check the maze after to see what happened after all actions (collision, death, dot collection etc)
        state.checkMaze();
    }

    private void drawMaze(Graphics2D g2d) {

        short i = 0;
        int x, y;

        for (y = 0; y < screenHeight; y += blockSize) {
            for (x = 0; x < screenWidth; x += blockSize) {

                g2d.setColor(mazeColor);
                g2d.setStroke(new BasicStroke(2));

                if ((state.boardData[i] & 1) != 0) {
                    g2d.drawLine(x, y, x, y + blockSize - 1);
                }

                if ((state.boardData[i] & 2) != 0) {
                    g2d.drawLine(x, y, x + blockSize - 1, y);
                }

                if ((state.boardData[i] & 4) != 0) {
                    g2d.drawLine(x + blockSize - 1, y, x + blockSize - 1,
                            y + blockSize - 1);
                }

                if ((state.boardData[i] & 8) != 0) {
                    g2d.drawLine(x, y + blockSize - 1, x + blockSize - 1,
                            y + blockSize - 1);
                }

                if ((state.boardData[i] & 16) != 0) {
                    g2d.setColor(dotColor);
                    g2d.fillRect(x + 11, y + 11, 2, 2);
                }

                i++;
            }
        }
    }

    public void playTurn() throws Exception {
        playGame();
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        doDrawing(g);
    }

    private void doDrawing(Graphics g) {

        Graphics2D g2d = (Graphics2D) g;

        g2d.setColor(cellColor);
        g2d.fillRect(0, 0, this.screenWidth, this.screenHeight);

        drawMaze(g2d);
        g2d.drawImage(pacmanImage, state.pacman.getCurrentPosition().x * blockSize + 1, state.pacman.getCurrentPosition().y * blockSize + 1, this);

        for (int i = 0; i < state.monsterAmount; i++)
            g2d.drawImage(ghostImage, state.monsters.get(i).getCurrentPosition().x * blockSize + 1, state.monsters.get(i).getCurrentPosition().y * blockSize + 1, this);

        g2d.drawImage(ii, 5, 5, this);
        Toolkit.getDefaultToolkit().sync();
        g2d.dispose();
    }

    //This function waits until a keyboard button has been clicked
    //When a button is clicked, it wakes up and gets the clicked button
    private Action moveAgentByKeyboard(GameCreature agent) {
        try {
            synchronized (lock) {
                while (selectedAction == null)
                    lock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        Action decidedAction = selectedAction;

        synchronized (lock) {
            selectedAction = null;
        }

        if (state.checkActionValidity(agent.getCurrentPosition(), decidedAction))
            return decidedAction;

        return Action.STOP;
    }

    public void startCycle() {
        cycle.start();
    }

    class TAdapter extends KeyAdapter {

        @Override
        public void keyPressed(KeyEvent e) {

            Config config = Config.getInstance();

            /*If Pacman is not controlled by keyboard, we should not listen for keys*/
            if (config.isAutomatic())
                return;

            int key = e.getKeyCode();

            if (key == KeyEvent.VK_LEFT)
                selectedAction = Action.LEFT;
            else if (key == KeyEvent.VK_RIGHT)
                selectedAction = Action.RIGHT;
            else if (key == KeyEvent.VK_UP)
                selectedAction = Action.UP;
            else if (key == KeyEvent.VK_DOWN)
                selectedAction = Action.DOWN;
            else if (key == KeyEvent.VK_N)
                selectedAction = Action.STOP;

            synchronized (lock) {
                if (selectedAction != null)
                    lock.notify();
            }
        }
    }
}