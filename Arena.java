import java.awt.*;
import java.awt.image.BufferStrategy;
import java.lang.*;
import java.util.ArrayList;
import java.util.Random;

public class Arena extends Canvas implements Runnable {
    private final static int SIZE = 64;

    public final static boolean SHOULD_DRAW = true;

    public Tron tron;
    private Random random = new Random();

    private BufferStrategy bs;

    private Thread conductor;
    public static ArrayList<Player> players = new ArrayList<>();
    public boolean board[][];
    public boolean startAgain = false;
    private int xmax, ymax;

    private static final int WAITING = 0;
    private static final int RUNNING = 1;
    private static final int RESTARTING = 2;
    private int state;

    private int playerSize = 5;

    public Arena(Tron t) {
        this.setBackground(Color.black);
        this.resize(playerSize * SIZE, playerSize * SIZE);

        this.conductor = null;
        this.board = null;
        this.state = WAITING;
        this.tron = t;

        this.xmax = SIZE;
        this.ymax = SIZE;

        players.add(new MaxMaxPlayer("MaxMax", Color.red, this, this.xmax, this.ymax, (byte) 1));
        players.add(new CirclePlayer("Circle", Color.white, this, this.xmax, this.ymax, (byte) 2));
        players.add(new MyOtherPlayer("Other", Color.green, this, this.xmax, this.ymax, (byte) 3));
    }

    public void start() {
        for (Player player : players) {
            player.crash = false;
        }
        this.repaint();

        if (this.board == null) {
            this.board = new boolean[xmax][ymax];
        }

        this.createBufferStrategy(120);
        this.bs = this.getBufferStrategy();

        if (this.conductor == null) {
            this.conductor = new Thread(this, "Arena");
            this.conductor.start();
        } else {
            this.conductor.resume();
        }
    }

    private void startPlayers() {
        this.tron.updateScore();

        this.clearBoard();

        for (Player player : players) {
            player.go(random.nextInt(xmax), random.nextInt(ymax));
        }

        this.state = RUNNING;
    }

    public void run() {
        while (true) {
            switch (state) {
                case RUNNING:
                    for (Player player : players) {
                        if (!player.crash) {
                            player.step();
                        }
                    }
                    break;
                case RESTARTING:
                    for (Player p1 : players) {
                        boolean crash = true;
                        for (Player p2 : players) {
                            if (p1 != p2 && !p2.crash) {
                                crash = false;
                            }
                        }
                        if (crash) {
                            p1.tallyWin();
                        }
                        p1.restart(crash);
                    }

                    this.state = WAITING;
                    this.tron.updateScore();
                    if (!SHOULD_DRAW) this.startAgain = true;
                    break;
                case WAITING:
                    if (this.startAgain) {
                        this.startAgain = false;
                        this.start();
                        this.startPlayers();
                    }
                    break;
            }

            this.repaint();

            if (SHOULD_DRAW) {
                try {
                    Thread.sleep(4);
                } catch (InterruptedException ignored) {

                }
            }

            for (Player player : players) {
                player.newPos();
            }
        }
    }

    public void killPlayer(byte number) {
        int playersCrashed = 0;
        for (Player player : players) {
            if (player.player_no == number) {
                player.crash = true;
            }
            if (player.crash) {
                playersCrashed++;
            }
        }
        if (playersCrashed == players.size() - 1) {
            this.state = RESTARTING;
        }
    }

    public void draw(DrawLambda callback) {
        Graphics2D graphics = (Graphics2D) this.bs.getDrawGraphics().create();

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        callback.draw(graphics);

        graphics.dispose();
        this.bs.show();
    }

    private void clearBoard() {
        int i, j;
        for (i = 0; i < this.xmax; i++) {
            for (j = 0; j < this.ymax; j++) {
                this.board[i][j] = false;
            }
        }
    }
}
