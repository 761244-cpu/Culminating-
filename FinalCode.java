import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JButton;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class MyProgram {

    private static final String DATA_FILE = "file.txt";

    private static JFrame frame;
    private static JPanel loginpanel;
    private static JTextField userText;
    private static JPasswordField passwordText;
    private static JLabel statusLabel;
    private static GamePanel gamePanel;

    public static void main(String[] args) {
        // Starts the program and builds the login screen
        SwingUtilities.invokeLater(() -> {
            frame = new JFrame("Sky Defender");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);

            gamePanel = new GamePanel();

            loginpanel = new JPanel();
            loginpanel.setLayout(null);
            loginpanel.setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));

            // label for user
            JLabel label = new JLabel("User");
            label.setBounds(310, 210, 80, 25);
            loginpanel.add(label);

            // text for user
            userText = new JTextField(20);
            userText.setBounds(400, 210, 165, 25);
            loginpanel.add(userText);

            // label for password
            JLabel passwordlabel = new JLabel("Password");
            passwordlabel.setBounds(310, 245, 80, 25);
            loginpanel.add(passwordlabel);

            // password text
            passwordText = new JPasswordField();
            passwordText.setBounds(400, 245, 165, 25);
            loginpanel.add(passwordText);

            // login button
            JButton loginbutton = new JButton("Login");
            loginbutton.setBounds(350, 290, 90, 25);
            loginpanel.add(loginbutton);

            // register button
            JButton registerbutton = new JButton("Register");
            registerbutton.setBounds(455, 290, 110, 25);
            loginpanel.add(registerbutton);

            statusLabel = new JLabel("");
            statusLabel.setBounds(310, 330, 330, 25);
            loginpanel.add(statusLabel);

            loginbutton.addActionListener(e -> checkpassword());
            registerbutton.addActionListener(e -> register());

            frame.add(loginpanel);
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    public static void checkpassword() {
        String user = userText.getText().trim();
        String password = new String(passwordText.getPassword()).trim();

        if (user.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter both a username and password.");
            return;
        }

        try {
            ensureFileExists();
            Scanner sc = new Scanner(new File(DATA_FILE));
            // Reads the lines in the file 
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
              // gets rid of the space at the end and start
              // Splits line into parts array to compare with the username and password the user inputs
                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && parts[0].equals(user) && parts[1].equals(password)) {
                    int savedHighScore = 0;
                     // checks if the length of the array is greater than 3 
                    if (parts.length >= 3) {
                        try {
                             // stores the highscore in this variable
                            savedHighScore = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ignored) {}
                    }

                    sc.close();
                     // starts the game for the respective user
                    startGameFor(user, password, savedHighScore);
                    return;
                }
            }

            sc.close();
            statusLabel.setText("Incorrect username or password.");
        } catch (IOException e) {
            statusLabel.setText("Could not read file.txt.");
            e.printStackTrace();
        }
    }

    public static void register() {
         // same concept as password checker
        String user = userText.getText().trim();
        String password = new String(passwordText.getPassword()).trim();
        // As long as either is empty it will ask the user to enter their password or username again    
        if (user.isEmpty() || password.isEmpty()) {
            statusLabel.setText("Enter both a username and password.");
            return;
        }

         // prevents username or password from having spaces 
        if (user.contains(" ") || password.contains(" ")) {
            statusLabel.setText("Username and password cannot contain spaces.");
            return;
        }

        try {
            ensureFileExists();

            Scanner sc = new Scanner(new File(DATA_FILE));
            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
                // ensures that a person does not register as someone else
                String[] parts = line.split("\\s+");
                if (parts.length >= 1 && parts[0].equals(user)) {
                    sc.close();
                    statusLabel.setText("That username already exists.");
                    return;
                }
            }
            sc.close();

            FileWriter writer = new FileWriter(DATA_FILE, true);
             // Writes the username password and default score the file 
            writer.write(user + " " + password + " 0" + System.lineSeparator());
            writer.close();

            startGameFor(user, password, 0);
        } catch (IOException e) {
            statusLabel.setText("Could not write to file.txt.");
            e.printStackTrace();
        }
    }

    private static void startGameFor(String user, String password, int highScore) {
        // stores the user's password and high score 
        gamePanel.setUser(user, password, highScore);
         
        // creates difficulty panel  
        difficultypanel difficultyPanel = new difficultypanel();
        frame.setContentPane(difficultyPanel);
        frame.revalidate();
        frame.repaint();
    }

    public static void startGameWithDifficulty(String difficulty) {
        // sets the difficulty depending on the difficulty selected
        gamePanel.setDifficulty(difficulty);
        frame.setContentPane(gamePanel);
        frame.revalidate();
        frame.repaint();
        gamePanel.requestFocusInWindow();
        gamePanel.startGame();
    }

    // Makes sure the save file exists before reading or writing to it
    private static void ensureFileExists() throws IOException {
        File file = new File(DATA_FILE);
        if (!file.exists()) {
            file.createNewFile();
        }
    }

    public static void updateHighScore(String user, String password, int score) {
        try {
            ensureFileExists();

            ArrayList<String> lines = new ArrayList<>();
            Scanner sc = new Scanner(new File(DATA_FILE));
            boolean updated = false;

            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
            // updates the highscore for the respective user 
                String[] parts = line.split("\\s+");
                if (parts.length >= 2 && parts[0].equals(user) && parts[1].equals(password)) {
                    int oldScore = 0;
                    if (parts.length >= 3) {
                        try {
                            oldScore = Integer.parseInt(parts[2]);
                        } catch (NumberFormatException ignored) {}
                    }
                    int bestScore = Math.max(oldScore, score);
                    lines.add(user + " " + password + " " + bestScore);
                    updated = true;
                } else {
                    lines.add(line);
                }
            }
            sc.close();

            if (!updated) {
                lines.add(user + " " + password + " " + score);
            }

            PrintWriter writer = new PrintWriter(new FileWriter(DATA_FILE));
            // prints out the new line for the user including the new high score 
            for (String line : lines) {
                writer.println(line);
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

// keeps the leaderboard as an array of strings 
    public static ArrayList<String> getLeaderboardText(int maxPlayers) {
        ArrayList<UserScore> scores = new ArrayList<>();
        ArrayList<String> result = new ArrayList<>();

        try {
            ensureFileExists();
            Scanner sc = new Scanner(new File(DATA_FILE));

            while (sc.hasNextLine()) {
                String line = sc.nextLine().trim();
                if (line.isEmpty()) continue;
 // adds the name of the user alongside their score 
                String[] parts = line.split("\\s+");
                if (parts.length >= 3) {
                    try {
                        scores.add(new UserScore(parts[0], Integer.parseInt(parts[2])));
                    } catch (NumberFormatException ignored) {}
                }
            }
            sc.close();
        } catch (IOException e) {
            result.add("Could not load leaderboard.");
            return result;
        }

        scores.sort((a, b) -> Integer.compare(b.score, a.score));
// prints out the names of players and the scores 
        for (int i = 0; i < scores.size() && i < maxPlayers; i++) {
            UserScore s = scores.get(i);
            result.add((i + 1) + ". " + s.user + " - " + s.score);
        }

        if (result.isEmpty()) {
            result.add("No scores yet.");
        }

        return result;
    }

// keeps track of user score 
    private static class UserScore {
        String user;
        int score;

        UserScore(String user, int score) {
            this.user = user;
            this.score = score;
        }
    }
}

// Panel that appears after login/register so the player can choose a difficulty
class difficultypanel extends JPanel {
    public difficultypanel() {
        // Sets up the difficulty selection screen
        setPreferredSize(new Dimension(GamePanel.WIDTH, GamePanel.HEIGHT));
        setBackground(new Color(40, 20, 60));
        setLayout(null);

        JLabel title = new JLabel("Choose Difficulty");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Serif", Font.BOLD, 42));
        title.setBounds(290, 150, 350, 60);
        add(title);

        JButton easy = new JButton("Easy");
        easy.setBounds(350, 250, 200, 40);
        add(easy);

        JButton medium = new JButton("Medium");
        medium.setBounds(350, 310, 200, 40);
        add(medium);

        JButton hard = new JButton("Hard");
        hard.setBounds(350, 370, 200, 40);
        add(hard);

// basic instructions 
        JLabel info = new JLabel("Easy = slow enemies, Medium = normal enemies, Hard = fastest enemies");
        info.setForeground(Color.WHITE);
        info.setFont(new Font("Serif", Font.PLAIN, 16));
        info.setBounds(240, 440, 500, 25);
        add(info);
// switches to the difficulty the user selects 
        easy.addActionListener(e -> MyProgram.startGameWithDifficulty("Easy"));
        medium.addActionListener(e -> MyProgram.startGameWithDifficulty("Medium"));
        hard.addActionListener(e -> MyProgram.startGameWithDifficulty("Hard"));
    }
}

class GamePanel extends JPanel implements ActionListener, KeyListener {
// All required variables 
    public static final int WIDTH = 900;
    public static final int HEIGHT = 600;

    private static final int FPS = 60;
    private static final int PLAYER_FIRE_COOLDOWN = 8;

    private Timer gameTimer;
    private boolean running = true;
    private boolean gameOver = false;
    private boolean victory = false;
    private boolean showStartScreen = true;
    private String message = "";
    private int messageTimer = 0;

    private Dragon player;
    private Castle castle;
    
// arraylists for objects 
    private final List<EnemyDragon> enemies = new ArrayList<>();
    private final List<Fireball> fireballs = new ArrayList<>();
    private final List<Cloud> clouds = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<PowerUp> powerUps = new ArrayList<>();

    private final Random random = new Random();

    private int score = 0;
    private int highScore = 0;
    private String currentUser = "";
    private String currentPassword = "";
    private String difficulty = "Medium";
    private int wave = 1;
    private int frameCount = 0;
    private int fireCooldown = 0;
    private int enemySpawnTimer = 0;
    private int enemiesKilledThisWave = 0;
    private int enemiesPerWave = 8;
    private int waveMessageTimer = 0;
    private double scrollOffset = 0;

    private final SoundManager sound = new SoundManager();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(new Color(40, 20, 60));
        setFocusable(true);
        addKeyListener(this);
    }

// stores user password and highscore for further use 
    public void setUser(String user, String password, int savedHighScore) {
        currentUser = user;
        currentPassword = password;
        highScore = savedHighScore;
    }

// changes the difficulty 
    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public void startGame() {
        running = true;
        if (gameTimer != null) {
            gameTimer.stop();
        }
        resetGame();
        gameTimer = new Timer(1000 / FPS, this);
        gameTimer.start();
    }

// clears objects and resets to default start screen 
    private void resetGame() {
        player = new Dragon(120, HEIGHT / 2 - 30);
        castle = new Castle(WIDTH / 2 - 110, HEIGHT - 200);
        enemies.clear();
        fireballs.clear();
        powerUps.clear();
        particles.clear();
        if (clouds.isEmpty()) {
            for (int i = 0; i < 6; i++) {
                clouds.add(new Cloud(random.nextInt(WIDTH), random.nextInt(HEIGHT / 2), 0.3 + random.nextDouble() * 0.4));
            }
        }
        score = 0;
        wave = 1;
        frameCount = 0;
        fireCooldown = 0;
        enemySpawnTimer = 0;
        enemiesKilledThisWave = 0;
        enemiesPerWave = 8;
        waveMessageTimer = 120;
        gameOver = false;
        victory = false;
        showStartScreen = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!running) return;
        update();
        repaint();
    }

    private void update() {
        if (showStartScreen) {
            updateClouds();
            return;
        }

        if (gameOver) {
            updateClouds();
            updateParticles();
            return;
        }

// second wave appears only after a certain amount of time has passed 
        frameCount++;
        scrollOffset += 0.5 + wave * 0.1;
        if (scrollOffset > 2000) scrollOffset -= 2000;

        if (waveMessageTimer > 0) waveMessageTimer--;
        if (messageTimer > 0) messageTimer--;

        updateClouds();
        player.update();
        updateFireballs();
        updateEnemies();
        updatePowerUps();
        updateParticles();
        checkCollisions();
        handleSpawning();
        checkWaveProgress();

// when castle loses all its health game ends 
        if (castle.health <= 0) {
            castle.health = 0;
            endGame(false);
        }
    }

// spawns clouds repeatedly 
    private void updateClouds() {
        for (Cloud c : clouds) {
            c.x -= c.speed;
            if (c.x + c.width < 0) {
                c.x = WIDTH + random.nextInt(200);
                c.y = random.nextInt(HEIGHT / 2);
                c.width = 80 + random.nextInt(120);
                c.height = 25 + random.nextInt(30);
            }
        }
    }

// moves player fireballs and removes them after they leave the screen 
    private void updateFireballs() {
        Iterator<Fireball> it = fireballs.iterator();
        while (it.hasNext()) {
            Fireball b = it.next();
            b.x += b.speed;
            if (b.x > WIDTH) it.remove();
        }
        if (fireCooldown > 0) fireCooldown--;
    }

// creates new enemies 
    private void updateEnemies() {
        for (EnemyDragon en : enemies) {
            en.update();
        }
        Iterator<EnemyDragon> it = enemies.iterator();
        while (it.hasNext()) {
            EnemyDragon en = it.next();
            
            // Checks if the enemy is dead and then gets rid of it so that the castle doesnt take damage
            if (en.dead) {
                it.remove();
                continue;
            }
            // if the enemy gets past a certain point the castle takes damage 
            if (en.x + en.width < 0) {
                castle.takeDamage(en.attackDamage);
                spawnParticles(en.x + en.width / 2.0, en.y + en.height / 2.0, Color.RED, 15);
                sound.playCastleHit();
                it.remove();
            } else if (en.y > HEIGHT + 100) {
                it.remove();
            }
        }
    }

// spawns power ups 
    private void updatePowerUps() {
        Iterator<PowerUp> it = powerUps.iterator();
        while (it.hasNext()) {
            PowerUp p = it.next();
            p.x -= 1.5;
            if (p.x + p.width < 0) it.remove();
        }
    }

    // Moves particles and removes them when their life runs out
    private void updateParticles() {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.x += p.vx;
            p.y += p.vy;
            p.vy += 0.1;
            p.life--;
            if (p.life <= 0) it.remove();
        }
    }

    // Checks collisions between the player, enemies, fireballs, and powerups
    private void checkCollisions() {
        Rectangle pr = player.getBounds();

// when the fireball it hits the enemies it removes the enemy some enemies might take more damage 
        for (Fireball fb : fireballs) {
            Rectangle br = fb.getBounds();
            for (EnemyDragon en : enemies) {
                if (en.getBounds().intersects(br)) {
                    en.health--;
                    if (en.health <= 0) {
                        spawnParticles(en.x + en.width / 2.0, en.y + en.height / 2.0, new Color(255, 100, 0), 35);
                        spawnParticles(en.x + en.width / 2.0, en.y + en.height / 2.0, new Color(80, 0, 0), 20);
                        sound.playExplosion();
                        // points are accumulated 
                        score += en.points;
                        enemiesKilledThisWave++;
                        if (random.nextDouble() < 0.18) {
                            powerUps.add(new PowerUp(en.x, en.y));
                        }
                        en.dead = true;
                    } else {
                        spawnParticles(fb.x, fb.y, new Color(255, 200, 0), 5);
                    }
                    fb.x = WIDTH + 100;
                    break;
                }
            }
        }
        fireballs.removeIf(b -> b.x > WIDTH + 50);

        for (EnemyDragon en : enemies) {
            if (pr.intersects(en.getBounds())) {
                player.takeDamage(30);
                spawnParticles(en.x, en.y, new Color(180, 0, 0), 25);
                en.health -= 2;
                if (en.health <= 0) {
                    score += en.points;
                    enemiesKilledThisWave++;
                    spawnParticles(en.x + en.width / 2.0, en.y + en.height / 2.0, new Color(255, 100, 0), 30);
                    sound.playExplosion();
                    en.dead = true;
                } else {
                    en.x -= 40;
                }
                if (player.lives <= 0) endGame(false);
            }
        }

        Iterator<PowerUp> pit = powerUps.iterator();
        while (pit.hasNext()) {
            PowerUp p = pit.next();
            if (pr.intersects(p.getBounds())) {
                sound.playPowerUp();
                if (p.type == PowerUp.Type.HEALTH) {
                    player.lives = Math.min(player.maxLives, player.lives + 1);
                    spawnParticles(p.x, p.y, new Color(0, 255, 100), 20);
                    showMessage("+1 LIFE");
                } else {
                    player.activatePower();
                    spawnParticles(p.x, p.y, new Color(255, 180, 0), 20);
                    showMessage("FIRE BREATH UP!");
                }
                pit.remove();
            }
        }
    }

    // Controls how often enemies appear
    private void handleSpawning() {
        if (waveMessageTimer > 0) return;
        int spawnRate = Math.max(50, 100 - wave * 4);
        enemySpawnTimer++;
        if (enemySpawnTimer >= spawnRate) {
            enemySpawnTimer = 0;
            spawnEnemy();
        }
    }

    // Creates enemies depending on the current wave and selected difficulty
    private void spawnEnemy() {
        int typeRoll = random.nextInt(100);
        int y = 50 + random.nextInt(HEIGHT - 250);
        if (wave == 1) {
            enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.SCOUT, wave, difficulty));
        } else if (wave == 2) {
            if (typeRoll < 70) enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.SCOUT, wave, difficulty));
            else enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.WARRIOR, wave, difficulty));
        } else if (wave < 5) {
            if (typeRoll < 50) enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.SCOUT, wave, difficulty));
            else if (typeRoll < 85) enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.WARRIOR, wave, difficulty));
            else enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.WARLORD, wave, difficulty));
        } else {
            if (typeRoll < 35) enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.SCOUT, wave, difficulty));
            else if (typeRoll < 75) enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.WARRIOR, wave, difficulty));
            else enemies.add(new EnemyDragon(WIDTH + 40, y, EnemyDragon.Type.WARLORD, wave, difficulty));
        }
    }

    // Moves to the next wave after enough enemies are defeated
    private void checkWaveProgress() {
        if (enemiesKilledThisWave >= enemiesPerWave && enemies.isEmpty()) {
            wave++;
            enemiesKilledThisWave = 0;
            enemiesPerWave = 8 + wave * 2;
            waveMessageTimer = 150;
            if (wave > 10) {
                endGame(true);
            } else {
                sound.playLevelUp();
                showMessage("WAVE " + wave + " - THE HORDE APPROACHES!");
            }
        }
    }

    // Shows short messages in the center of the screen
    private void showMessage(String msg) {
        message = msg;
        messageTimer = 120;
    }

    // Creates small particles for explosions and effects
    private void spawnParticles(double x, double y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double speed = 1 + random.nextDouble() * 4;
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color, 20 + random.nextInt(20)));
        }
    }

    // Ends the game and updates the high score if needed
    private void endGame(boolean won) {
        gameOver = true;
        victory = won;
        if (score > highScore) {
            highScore = score;
            MyProgram.updateHighScore(currentUser, currentPassword, highScore);
        }
        sound.playGameOver();
    }

    // Creates the player's fireball attack
    private void playerShoot() {
        if (fireCooldown > 0 || gameOver || showStartScreen) return;
        fireCooldown = player.hasPower ? PLAYER_FIRE_COOLDOWN / 2 : PLAYER_FIRE_COOLDOWN;
        int bx = player.x + player.width - 5;
        int by = player.y + player.height / 2 - 4;
        fireballs.add(new Fireball(bx, by, 14, 0, true));
        if (player.hasPower) {
            fireballs.add(new Fireball(bx, by - 12, 13, -0.3, true));
            fireballs.add(new Fireball(bx, by + 12, 13, 0.3, true));
        }
        sound.playShoot();
    }

    @Override
    // Draws the whole game screen
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawSky(g2);
        drawDistantMountains(g2);
        drawClouds(g2);
        drawGround(g2);

        if (!showStartScreen) {
            if (castle != null) castle.draw(g2);
            for (PowerUp p : powerUps) p.draw(g2);
            for (EnemyDragon en : enemies) en.draw(g2);
            for (Fireball b : fireballs) b.draw(g2);
            for (Particle p : particles) p.draw(g2);
            if (player != null) player.draw(g2);

            drawHUD(g2);
            drawWaveMessage(g2);
            if (messageTimer > 0) drawCenterMessage(g2);

            if (gameOver) drawGameOver(g2);
        } else {
            drawStartScreen(g2);
        }
    }

    // Draws the background sky
    private void drawSky(Graphics2D g2) {
        GradientPaint gp = new GradientPaint(0, 0, new Color(30, 10, 50), 0, HEIGHT, new Color(180, 80, 50));
        g2.setPaint(gp);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setColor(new Color(255, 180, 80, 30));
        g2.fillOval(620, 80, 120, 120);
        g2.setColor(new Color(255, 200, 120, 60));
        g2.fillOval(640, 100, 80, 80);

        g2.setColor(new Color(255, 255, 255, 200));
        for (int i = 0; i < 40; i++) {
            int sx = (i * 137 + (int) (scrollOffset * 0.5)) % WIDTH;
            int sy = (i * 73) % (HEIGHT - 150);
            g2.fillOval(sx, sy, 2, 2);
        }
    }

    // Draws the mountains in the background
    private void drawDistantMountains(Graphics2D g2) {
        int baseY = HEIGHT - 130;
        int offset = (int) (scrollOffset * 0.3) % 400;
        g2.setColor(new Color(50, 30, 60));
        for (int i = -1; i < 4; i++) {
            int x = i * 300 - offset;
            int[] xs = {x, x + 150, x + 300};
            int[] ys = {baseY, baseY - 100, baseY};
            g2.fillPolygon(xs, ys, 3);
        }
        g2.setColor(new Color(30, 20, 40));
        for (int i = -1; i < 4; i++) {
            int x = i * 250 - (int) (scrollOffset * 0.5) % 500;
            int[] xs = {x, x + 125, x + 250};
            int[] ys = {baseY + 20, baseY - 60, baseY + 20};
            g2.fillPolygon(xs, ys, 3);
        }
    }

    // Draws the ground at the bottom of the screen
    private void drawGround(Graphics2D g2) {
        g2.setColor(new Color(30, 20, 25));
        g2.fillRect(0, HEIGHT - 100, WIDTH, 100);
        g2.setColor(new Color(45, 30, 35));
        int offset = (int) (scrollOffset * 0.8) % 80;
        for (int i = -1; i < 14; i++) {
            int x = i * 80 - offset;
            int[] xs = {x, x + 40, x + 80};
            int[] ys = {HEIGHT - 100, HEIGHT - 130, HEIGHT - 100};
            g2.fillPolygon(xs, ys, 3);
        }
        g2.setColor(new Color(50, 35, 30));
        g2.fillRect(0, HEIGHT - 100, WIDTH, 4);
    }

    // Draws all clouds
    private void drawClouds(Graphics2D g2) {
        for (Cloud c : clouds) c.draw(g2);
    }

    // Draws score, wave, high score, difficulty, lives, and castle health
    private void drawHUD(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(10, 10, 240, 120, 15, 15);
        g2.setColor(new Color(255, 220, 150));
        g2.setFont(new Font("Serif", Font.BOLD, 20));
        g2.drawString("SCORE: " + score, 25, 38);
        g2.drawString("WAVE: " + wave, 25, 65);
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        g2.drawString("Best: " + highScore, 25, 88);
        g2.drawString("Difficulty: " + difficulty, 25, 108);

        if (castle != null) {
            int chx = WIDTH - 250;
            int chy = 10;
            g2.setColor(new Color(0, 0, 0, 170));
            g2.fillRoundRect(chx, chy, 230, 70, 15, 15);
            g2.setColor(new Color(220, 200, 150));
            g2.setFont(new Font("Serif", Font.BOLD, 16));
            g2.drawString("CASTLE", chx + 15, chy + 22);
            int barW = 200;
            int barH = 16;
            int barX = chx + 15;
            int barY = chy + 32;
            g2.setColor(new Color(60, 20, 20));
            g2.fillRoundRect(barX, barY, barW, barH, 6, 6);
            float pct = (float) castle.health / castle.maxHealth;
            Color barColor = pct > 0.6 ? new Color(80, 200, 100) : pct > 0.3 ? new Color(220, 180, 60) : new Color(220, 60, 60);
            g2.setColor(barColor);
            g2.fillRoundRect(barX, barY, (int) (barW * pct), barH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Serif", Font.BOLD, 12));
            g2.drawString(castle.health + " / " + castle.maxHealth, barX + 65, barY + 13);
        }

        g2.setColor(new Color(0, 0, 0, 170));
        g2.fillRoundRect(WIDTH - 250, 90, 230, 50, 15, 15);
        g2.setColor(new Color(220, 200, 150));
        g2.setFont(new Font("Serif", Font.BOLD, 16));
        g2.drawString("DRAGON LIVES:", WIDTH - 235, 110);
        for (int i = 0; i < player.lives; i++) {
            g2.setColor(new Color(100, 200, 100));
            g2.fillOval(WIDTH - 90 + i * 22, 100, 18, 18);
            g2.setColor(new Color(40, 100, 40));
            g2.fillOval(WIDTH - 87 + i * 22, 103, 12, 12);
        }
    }

    // Draws the wave message when a new wave starts
    private void drawWaveMessage(Graphics2D g2) {
        if (waveMessageTimer <= 0) return;
        float alpha = Math.min(1f, waveMessageTimer / 30f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, HEIGHT / 2 - 60, WIDTH, 80);
        g2.setColor(new Color(255, 100, 50));
        g2.setFont(new Font("Serif", Font.BOLD, 48));
        String w = "WAVE " + wave;
        int ww = g2.getFontMetrics().stringWidth(w);
        g2.drawString(w, (WIDTH - ww) / 2, HEIGHT / 2);
        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        String sub = "The dragon horde draws near...";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, (WIDTH - sw) / 2, HEIGHT / 2 + 28);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // Draws temporary center-screen messages
    private void drawCenterMessage(Graphics2D g2) {
        float alpha = Math.min(1f, messageTimer / 30f);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2.setColor(new Color(255, 220, 100));
        g2.setFont(new Font("Serif", Font.BOLD, 28));
        int mw = g2.getFontMetrics().stringWidth(message);
        g2.drawString(message, (WIDTH - mw) / 2, HEIGHT / 2 + 100);
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    }

    // Draws the instructions before the game begins
    private void drawStartScreen(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 150));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(new Color(255, 80, 40));
        g2.setFont(new Font("Serif", Font.BOLD, 72));
        String title = "SKY DEFENDER";
        int tw = g2.getFontMetrics().stringWidth(title);
        g2.drawString(title, (WIDTH - tw) / 2, 160);

        g2.setColor(new Color(255, 200, 100));
        g2.setFont(new Font("Serif", Font.ITALIC, 22));
        String sub = "~ Defend the castle from the dragon horde ~";
        int sw = g2.getFontMetrics().stringWidth(sub);
        g2.drawString(sub, (WIDTH - sw) / 2, 200);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Serif", Font.PLAIN, 22));
        String[] lines = {
                "Arrow Keys or WASD - Fly",
                "SPACE - Breathe Fire",
                "P - Pause / Resume",
                "",
                "Stop the dragons before they raze your castle!",
                "Collect orbs to heal and empower your fire breath."
        };
        int y = 280;
        for (String line : lines) {
            int lw = g2.getFontMetrics().stringWidth(line);
            g2.drawString(line, (WIDTH - lw) / 2, y);
            y += 32;
        }

        g2.setColor(new Color(255, 220, 100));
        g2.setFont(new Font("Serif", Font.BOLD, 24));
        String start = "Press any key to begin";
        int stw = g2.getFontMetrics().stringWidth(start);
        int pulse = (int) (Math.sin(frameCount * 0.1) * 20);
        g2.setColor(new Color(255, 220 + pulse, 100));
        g2.drawString(start, (WIDTH - stw) / 2, HEIGHT - 60);
    }

    // Draws the game over screen and leaderboard
    private void drawGameOver(Graphics2D g2) {
        g2.setColor(new Color(0, 0, 0, 200));
        g2.fillRect(0, 0, WIDTH, HEIGHT);

        g2.setColor(victory ? new Color(255, 220, 100) : new Color(255, 60, 60));
        g2.setFont(new Font("Serif", Font.BOLD, 72));
        String go = victory ? "VICTORY!" : "THE KEEP HAS FALLEN";
        int gw = g2.getFontMetrics().stringWidth(go);
        g2.drawString(go, (WIDTH - gw) / 2, HEIGHT / 2 - 50);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Serif", Font.BOLD, 30));
        String sc = "Final Score: " + score;
        int sw = g2.getFontMetrics().stringWidth(sc);
        g2.drawString(sc, (WIDTH - sw) / 2, HEIGHT / 2 + 5);

        g2.setFont(new Font("Serif", Font.PLAIN, 22));
        String hs = "High Score: " + highScore;
        int hw = g2.getFontMetrics().stringWidth(hs);
        g2.drawString(hs, (WIDTH - hw) / 2, HEIGHT / 2 + 40);

        g2.setFont(new Font("Serif", Font.PLAIN, 18));
        g2.drawString("Leaderboard", 40, HEIGHT / 2 - 40);
        ArrayList<String> leaders = MyProgram.getLeaderboardText(5);
        for (int i = 0; i < leaders.size(); i++) {
            g2.drawString(leaders.get(i), 40, HEIGHT / 2 - 10 + i * 25);
        }

        g2.setFont(new Font("Serif", Font.PLAIN, 20));
        String w = "Waves Survived: " + wave;
        int ww = g2.getFontMetrics().stringWidth(w);
        g2.drawString(w, (WIDTH - ww) / 2, HEIGHT / 2 + 70);

        g2.setColor(new Color(255, 220, 100));
        g2.setFont(new Font("Serif", Font.BOLD, 22));
        String rs = "Press R to play again";
        int rw = g2.getFontMetrics().stringWidth(rs);
        g2.drawString(rs, (WIDTH - rw) / 2, HEIGHT / 2 + 120);
    }

    @Override
    // Handles keyboard input when keys are pressed
    public void keyPressed(KeyEvent e) {
        int k = e.getKeyCode();
        if (showStartScreen) {
            showStartScreen = false;
            sound.playRoar();
            return;
        }
        if (gameOver && k == KeyEvent.VK_R) {
            resetGame();
            return;
        }
        if (k == KeyEvent.VK_P) {
            running = !running;
            return;
        }
        if (k == KeyEvent.VK_SPACE) playerShoot();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) player.left = true;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) player.right = true;
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) player.up = true;
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) player.down = true;
    }

    @Override
    // Stops movement when keys are released
    public void keyReleased(KeyEvent e) {
        int k = e.getKeyCode();
        if (k == KeyEvent.VK_LEFT || k == KeyEvent.VK_A) player.left = false;
        if (k == KeyEvent.VK_RIGHT || k == KeyEvent.VK_D) player.right = false;
        if (k == KeyEvent.VK_UP || k == KeyEvent.VK_W) player.up = false;
        if (k == KeyEvent.VK_DOWN || k == KeyEvent.VK_S) player.down = false;
    }

    @Override
    public void keyTyped(KeyEvent e) {}
}

// abstract class 
// Parent class for game objects that can be drawn on the screen
abstract class objects {
    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected int speed;

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void update() {
        // Default empty update. Subclasses can override this.
    }

    public void takeDamage(int dmg) {
        // Removes a life when the dragon is hit
        // Default empty damage method. Subclasses can override this.
    }

    public abstract void draw(Graphics2D g);
}

// Player-controlled dragon
class Dragon extends objects {
    public int x, y;
    public final int width = 70;
    public final int height = 40;
    public int lives = 3;
    public final int maxLives = 5;
    public boolean hasPower = false;
    private int powerTimer = 0;

    public boolean left, right, up, down;
    private final int speed = 5;
    private int animFrame = 0;
    private int invulnTimer = 0;

    public Dragon(int x, int y) {
        // Sets the starting position of the dragon
        this.x = x;
        this.y = y;
    }

    public void update() {
        // Moves the dragon based on keyboard input
        if (left) x -= speed;
        if (right) x += speed;
        if (up) y -= speed;
        if (down) y += speed;
        if (x < 10) x = 10;
        if (x > GamePanel.WIDTH - width - 10) x = GamePanel.WIDTH - width - 10;
        if (y < 40) y = 40;
        if (y > GamePanel.HEIGHT - height - 50) y = GamePanel.HEIGHT - height - 50;
        animFrame++;
        if (invulnTimer > 0) invulnTimer--;
        if (powerTimer > 0) {
            powerTimer--;
            if (powerTimer == 0) hasPower = false;
        }
    }

    public void takeDamage(int dmg) {
        if (invulnTimer > 0) return;
        if (dmg >= 30) lives -= 1;
        else lives -= 1;
        if (lives < 0) lives = 0;
        invulnTimer = 60;
    }

    public void activatePower() {
        // Turns on the temporary fire powerup
        hasPower = true;
        powerTimer = 60 * 15;
    }

    public Rectangle getBounds() {
        return new Rectangle(x + 10, y + 8, width - 25, height - 16);
    }

    public void draw(Graphics2D g) {
        if (invulnTimer > 0 && (animFrame / 4) % 2 == 0) return;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int wingFlap = (int) (Math.sin(animFrame * 0.25) * 8);

        g.setColor(new Color(60, 30, 80));
        int[] tailX = {x, x - 18, x - 10, x - 4};
        int[] tailY = {y + height / 2, y + height / 2 - 4, y + height / 2, y + height / 2 + 6};
        g.fillPolygon(tailX, tailY, 4);
        g.setColor(new Color(120, 60, 30));
        int[] tailSpikeX = {x - 14, x - 20, x - 16};
        int[] tailSpikeY = {y + height / 2 - 3, y + height / 2 - 8, y + height / 2 + 1};
        g.fillPolygon(tailSpikeX, tailSpikeY, 3);

        g.setColor(new Color(50, 110, 60));
        g.fillOval(x + 5, y + 8, width - 25, height - 16);

        g.setColor(new Color(120, 180, 90));
        g.fillOval(x + 10, y + 18, width - 35, 12);

        g.setColor(new Color(50, 110, 60));
        int[] neckX = {x + width - 25, x + width - 10, x + width - 8, x + width - 25};
        int[] neckY = {y + height / 2 - 2, y + height / 2 - 6, y + height / 2 + 8, y + height / 2 + 6};
        g.fillPolygon(neckX, neckY, 4);

        g.setColor(new Color(60, 130, 70));
        int[] headX = {x + width - 25, x + width, x + width - 8, x + width - 22};
        int[] headY = {y + height / 2 - 10, y + height / 2, y + height / 2 + 12, y + height / 2 + 8};
        g.fillPolygon(headX, headY, 4);

        g.setColor(new Color(40, 80, 50));
        int[] hornX = {x + width - 18, x + width - 12, x + width - 15};
        int[] hornY = {y + height / 2 - 10, y + height / 2 - 18, y + height / 2 - 8};
        g.fillPolygon(hornX, hornY, 3);
        int[] hornX2 = {x + width - 22, x + width - 18, x + width - 22};
        int[] hornY2 = {y + height / 2 - 6, y + height / 2 - 12, y + height / 2 - 2};
        g.fillPolygon(hornX2, hornY2, 3);

        g.setColor(Color.YELLOW);
        g.fillOval(x + width - 12, y + height / 2 - 2, 5, 5);
        g.setColor(new Color(255, 100, 0));
        g.fillOval(x + width - 11, y + height / 2 - 1, 2, 2);

        g.setColor(new Color(40, 20, 30));
        g.drawLine(x + width - 5, y + height / 2 + 4, x + width - 1, y + height / 2 + 3);

        int[] wingTopX = {x + 12, x + 32, x + 28};
        int[] wingTopY = {y + 8 + wingFlap, y - 12 + wingFlap, y + 8 + wingFlap};
        g.setColor(new Color(40, 90, 50));
        g.fillPolygon(wingTopX, wingTopY, 3);

        g.setColor(new Color(120, 60, 100, 180));
        int[] wingMembraneX = {x + 5, x + 30, x + 30};
        int[] wingMembraneY = {y + 8, y - 8 + wingFlap, y + 14};
        g.fillPolygon(wingMembraneX, wingMembraneY, 3);

        g.setColor(new Color(40, 90, 50));
        int[] wingBotX = {x + 12, x + 32, x + 28};
        int[] wingBotY = {y + height - 8 - wingFlap, y + height + 12 - wingFlap, y + height - 8 - wingFlap};
        g.fillPolygon(wingBotX, wingBotY, 3);

        g.setColor(new Color(120, 60, 100, 180));
        int[] wingMembraneX2 = {x + 5, x + 30, x + 30};
        int[] wingMembraneY2 = {y + height - 8, y + 8 - wingFlap, y + height - 14};
        g.fillPolygon(wingMembraneX2, wingMembraneY2, 3);

        g.setColor(new Color(200, 100, 50));
        g.fillOval(x - 4, y + height / 2 - 6, 10, 5);
        int flicker = (animFrame / 3) % 3;
        g.setColor(new Color(255, 200, 50, 180 - flicker * 50));
        g.fillOval(x - 10 - flicker * 2, y + height / 2 - 4, 6 + flicker * 2, 4);

        if (hasPower) {
            g.setColor(new Color(255, 200, 50, 80));
            g.fillOval(x - 4, y - 4, width + 8, height + 8);
        }
    }
}

// Enemy dragon class
class EnemyDragon extends objects {
    public enum Type { SCOUT, WARRIOR, WARLORD }

    public int x, y;
    public final int width, height;
    public int health;
    public int points;
    public int attackDamage;
    public Type type;
    public boolean dead = false;
    private int animFrame = 0;
    private final Random random = new Random();
    private int shootCooldown;
    private final int baseSpeed;
    private final int level;

    public EnemyDragon(int x, int y, Type type, int level, String difficulty) {
        // Sets enemy stats based on type and difficulty
        this.x = x;
        this.y = y;
        this.type = type;
        this.level = level;
        switch (type) {
            case SCOUT:
                width = 50; height = 28;
                health = 1 + level / 4;
                points = 50;
                attackDamage = 5;
                baseSpeed = getSpeedForDifficulty(type, difficulty);
                shootCooldown = 9999;
                break;
            case WARRIOR:
                width = 62; height = 36;
                health = 2 + level / 3;
                points = 120;
                attackDamage = 10;
                baseSpeed = getSpeedForDifficulty(type, difficulty);
                shootCooldown = 80 + random.nextInt(60);
                break;
            case WARLORD:
                width = 80; height = 50;
                health = 5 + level / 2;
                points = 300;
                attackDamage = 20;
                baseSpeed = getSpeedForDifficulty(type, difficulty);
                shootCooldown = 60 + random.nextInt(40);
                break;
            default:
                width = 50; height = 28; health = 1; points = 50; attackDamage = 5; baseSpeed = getSpeedForDifficulty(type, difficulty); shootCooldown = 9999;
        }
    }

    // Returns different enemy speeds based on the selected difficulty
    private int getSpeedForDifficulty(Type type, String difficulty) {
        if (difficulty.equals("Easy")) {
            if (type == Type.SCOUT) return 2;
            if (type == Type.WARRIOR) return 2;
            return 1;
        }

        if (difficulty.equals("Hard")) {
            if (type == Type.SCOUT) return 6;
            if (type == Type.WARRIOR) return 5;
            return 4;
        }

        // Medium difficulty
        if (type == Type.SCOUT) return 4;
        if (type == Type.WARRIOR) return 3;
        return 2;
    }

    public void update() {
        animFrame++;
        x -= baseSpeed;
        if (type == Type.SCOUT) {
            y += Math.sin((x + animFrame) * 0.06) * 1.5;
        } else if (type == Type.WARRIOR) {
            if (animFrame % 80 < 40) y += 0.4;
            else y -= 0.4;
        } else {
            if (animFrame % 100 < 50) y += 0.3;
            else y -= 0.3;
        }
        if (shootCooldown > 0 && type != Type.SCOUT) shootCooldown--;
    }

    // Checks if this enemy is ready to shoot
    public boolean canShoot() {
        if (type == Type.SCOUT) return false;
        if (shootCooldown <= 0) {
            shootCooldown = 90 + random.nextInt(60);
            return true;
        }
        return false;
    }

    public Rectangle getBounds() {
        return new Rectangle(x + 10, y + 6, width - 25, height - 12);
    }

    public void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color bodyColor, bellyColor, wingColor, membraneColor;
        switch (type) {
            case SCOUT:
                bodyColor = new Color(160, 40, 40);
                bellyColor = new Color(220, 100, 80);
                wingColor = new Color(120, 30, 30);
                membraneColor = new Color(60, 0, 0, 180);
                break;
            case WARRIOR:
                bodyColor = new Color(80, 40, 120);
                bellyColor = new Color(140, 100, 180);
                wingColor = new Color(50, 20, 80);
                membraneColor = new Color(30, 0, 50, 180);
                break;
            case WARLORD:
                bodyColor = new Color(40, 40, 50);
                bellyColor = new Color(90, 60, 50);
                wingColor = new Color(20, 20, 30);
                membraneColor = new Color(0, 0, 0, 200);
                break;
            default:
                bodyColor = Color.RED; bellyColor = Color.PINK; wingColor = Color.DARK_GRAY; membraneColor = Color.BLACK;
        }
        int wingFlap = (int) (Math.sin((animFrame + 7) * 0.25) * 8);

        g.setColor(wingColor);
        int[] wingTopX = {x + width - 30, x + width - 55, x + width - 45};
        int[] wingTopY = {y + 6 + wingFlap, y - 14 + wingFlap, y + 6 + wingFlap};
        g.fillPolygon(wingTopX, wingTopY, 3);

        g.setColor(membraneColor);
        int[] wingMembraneX = {x + width - 25, x + width - 55, x + width - 55};
        int[] wingMembraneY = {y + 6, y - 10 + wingFlap, y + 14};
        g.fillPolygon(wingMembraneX, wingMembraneY, 3);

        g.setColor(wingColor);
        int[] wingBotX = {x + width - 30, x + width - 55, x + width - 45};
        int[] wingBotY = {y + height - 6 - wingFlap, y + height + 14 - wingFlap, y + height - 6 - wingFlap};
        g.fillPolygon(wingBotX, wingBotY, 3);

        g.setColor(membraneColor);
        int[] wingMembraneX2 = {x + width - 25, x + width - 55, x + width - 55};
        int[] wingMembraneY2 = {y + height - 6, y + height + 10 - wingFlap, y + height - 14};
        g.fillPolygon(wingMembraneX2, wingMembraneY2, 3);

        g.setColor(bodyColor);
        g.fillOval(x + 10, y + 6, width - 25, height - 12);
        g.setColor(bellyColor);
        g.fillOval(x + 15, y + 14, width - 40, 10);

        g.setColor(bodyColor);
        int[] headX = {x + 10, x - 5, x + 5, x + 12};
        int[] headY = {y + height / 2 - 4, y + height / 2, y + height / 2 + 8, y + height / 2 + 6};
        g.fillPolygon(headX, headY, 4);

        g.setColor(new Color(20, 20, 20));
        int[] hornX = {x + 2, x + 8, x + 4};
        int[] hornY = {y + height / 2 - 4, y + height / 2 - 12, y + height / 2 - 2};
        g.fillPolygon(hornX, hornY, 3);

        g.setColor(new Color(255, 200, 0));
        g.fillOval(x - 2, y + height / 2 + 1, 5, 5);
        g.setColor(new Color(255, 50, 0));
        g.fillOval(x - 1, y + height / 2 + 2, 2, 2);

        g.setColor(new Color(200, 60, 30));
        g.fillOval(x + width, y + height / 2 - 4, 10, 5);
        int flicker = (animFrame / 3) % 3;
        g.setColor(new Color(255, 150, 0, 180 - flicker * 50));
        g.fillOval(x + width + 6 + flicker * 2, y + height / 2 - 2, 6 + flicker * 2, 4);

        int hbW = width - 10;
        g.setColor(new Color(0, 0, 0, 150));
        g.fillRect(x + 5, y - 8, hbW, 4);
        g.setColor(new Color(200, 60, 60));
        g.fillRect(x + 5, y - 8, (int) (hbW * ((float) health / (5 + level / 2))), 4);
    }
}

// Fireball class for both player and enemy attacks
class Fireball extends objects {
    public int x, y;
    public int speed;
    public double vy;
    public final int width = 18;
    public final int height = 10;
    public final boolean playerFireball;

    public Fireball(int x, int y, int speed, double vy, boolean playerFireball) {
        this.x = x;
        this.y = y;
        this.speed = speed;
        this.vy = vy;
        this.playerFireball = playerFireball;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int flicker = (int) (Math.random() * 3);
        if (playerFireball) {
            g.setColor(new Color(255, 200, 50, 70));
            g.fillOval(x - 6, y - 4, width + 12, height + 8);
            g.setColor(new Color(255, 150, 0));
            g.fillOval(x, y, width, height);
            g.setColor(new Color(255, 230, 80));
            g.fillOval(x + 3 + flicker, y + 2, width - 8, height - 4);
            g.setColor(Color.WHITE);
            g.fillOval(x + 6 + flicker, y + 3, 3, 3);
        } else {
            g.setColor(new Color(180, 0, 0, 70));
            g.fillOval(x - 6, y - 4, width + 12, height + 8);
            g.setColor(new Color(180, 40, 0));
            g.fillOval(x, y, width, height);
            g.setColor(new Color(255, 100, 0));
            g.fillOval(x + 3 + flicker, y + 2, width - 8, height - 4);
            g.setColor(new Color(255, 200, 50));
            g.fillOval(x + 6 + flicker, y + 3, 3, 3);
        }
    }
}

// Castle that the player must defend
class Castle extends objects {
    public int x, y;
    public int health;
    public int maxHealth = 100;
    public final int width = 220;
    public final int height = 200;
    private int animFrame = 0;
    private int hitFlash = 0;

    public Castle(int x, int y) {
        this.x = x;
        this.y = y;
        this.health = maxHealth;
    }

    public void takeDamage(int dmg) {
        health -= dmg;
        if (health < 0) health = 0;
        hitFlash = 15;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        animFrame++;
        if (hitFlash > 0) hitFlash--;

        int baseY = GamePanel.HEIGHT - 100;
        int topY = y;
        int wallH = baseY - topY;
        int wallX = x + 30;
        int wallW = width - 60;

        g.setColor(new Color(30, 20, 25));
        g.fillRect(x + 20, baseY - 4, width - 40, 8);

        g.setColor(new Color(95, 85, 75));
        g.fillRect(wallX, baseY - wallH, wallW, wallH);
        g.setColor(new Color(70, 60, 55));
        g.fillRect(wallX, baseY - 12, wallW, 12);

        g.setColor(new Color(110, 100, 90));
        for (int i = 0; i < wallW; i += 24) {
            int bx = wallX + i;
            int bh = 8 + (i * 17) % 6;
            g.fillRect(bx, baseY - wallH - bh, 20, bh);
        }

        g.setColor(new Color(50, 30, 25));
        int doorW = 28;
        int doorH = 50;
        g.fillRect(x + width / 2 - doorW / 2, baseY - doorH, doorW, doorH);
        g.setColor(new Color(80, 50, 30));
        g.fillOval(x + width / 2 + 4, baseY - doorH / 2, 4, 4);

        g.setColor(new Color(255, 220, 100));
        for (int i = 0; i < 3; i++) {
            int wy = baseY - wallH + 25 + i * 30;
            g.fillRect(wallX + 20 + i * 50, wy, 10, 14);
            g.fillRect(wallX + wallW - 30 - i * 50, wy, 10, 14);
        }

        int towerW = 36;
        int towerH = wallH + 30;
        int[] towerXs = {x + 5, x + width - towerW - 5, x + width / 2 - towerW / 2};
        for (int tx : towerXs) {
            g.setColor(new Color(95, 85, 75));
            g.fillRect(tx, baseY - towerH, towerW, towerH);
            g.setColor(new Color(75, 65, 60));
            g.fillRect(tx, baseY - 12, towerW, 12);
            g.setColor(new Color(110, 100, 90));
            for (int i = 0; i < towerW; i += 14) {
                int bh = 6 + (i * 11) % 5;
                g.fillRect(tx + i, baseY - towerH - bh, 12, bh);
            }
            g.setColor(new Color(70, 30, 30));
            int[] roofX = {tx - 4, tx + towerW + 4, tx + towerW / 2};
            int[] roofY = {baseY - towerH, baseY - towerH, baseY - towerH - 28};
            g.fillPolygon(roofX, roofY, 3);
            g.setColor(new Color(50, 20, 20));
            g.fillRect(tx + towerW / 2 - 2, baseY - towerH - 28, 4, 12);
            g.setColor(new Color(255, 220, 100));
            g.fillRect(tx + 8, baseY - towerH + 20, 8, 12);
            g.fillRect(tx + towerW - 16, baseY - towerH + 20, 8, 12);
            g.fillRect(tx + towerW / 2 - 4, baseY - towerH + 50, 8, 12);
        }

        int flagX = x + width / 2;
        int flagY = baseY - towerH - 40;
        g.setColor(new Color(40, 25, 15));
        g.fillRect(flagX - 1, flagY, 2, 40);
        g.setColor(new Color(180, 30, 30));
        int wave = (int) (Math.sin(animFrame * 0.1) * 3);
        int[] flagPtsX = {flagX + 1, flagX + 22, flagX + 18, flagX + 1};
        int[] flagPtsY = {flagY, flagY + 2, flagY + 10 + wave, flagY + 12};
        g.fillPolygon(flagPtsX, flagPtsY, 4);

        g.setColor(new Color(255, 100, 50, 100 + hitFlash * 10));
        g.fillRect(x, y, width, height);
    }
}

// Moving background cloud
class Cloud extends objects {
    public int x, y;
    public int width, height;
    public double speed;

    public Cloud(int x, int y, double speed) {
        this.x = x;
        this.y = y;
        this.speed = speed * 0.8;
        this.width = 100;
        this.height = 30;
    }

    public void draw(Graphics2D g) {
        g.setColor(new Color(60, 40, 80, 200));
        g.fillOval(x, y, width, height);
        g.fillOval(x + 20, y - 12, width - 30, height);
        g.fillOval(x + 40, y - 6, width - 50, height - 4);
        g.fillOval(x + 15, y + 12, width - 30, height - 10);
        g.setColor(new Color(120, 80, 140, 80));
        g.fillOval(x + 25, y - 4, width - 40, height - 8);
    }
}

// Small visual effects used for hits and explosions
class Particle {
    public double x, y;
    public double vx, vy;
    public Color color;
    public int life;
    public int size;

    public Particle(double x, double y, double vx, double vy, Color color, int life) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.color = color;
        this.life = life;
        this.size = 3 + (life / 8);
    }

    public void draw(Graphics2D g) {
        float alpha = Math.min(1f, life / 30f);
        Color c = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (alpha * 255));
        g.setColor(c);
        g.fillOval((int) x, (int) y, size, size);
    }
}

// Collectible powerups for health or stronger fire
class PowerUp extends objects {
    public enum Type { HEALTH, POWER }

    public int x, y;
    public final int width = 24;
    public final int height = 24;
    public Type type;
    private int animFrame = 0;
    private static final Random random = new Random();

    public PowerUp(int x, int y) {
        this.x = x;
        this.y = y;
        this.type = random.nextBoolean() ? Type.HEALTH : Type.POWER;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, width, height);
    }

    public void draw(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        animFrame++;
        int pulse = (int) (Math.sin(animFrame * 0.1) * 2);
        if (type == Type.HEALTH) {
            g.setColor(new Color(0, 255, 100, 80));
            g.fillOval(x - 4 - pulse, y - 4 - pulse, width + 8 + pulse * 2, height + 8 + pulse * 2);
            g.setColor(new Color(30, 120, 60));
            g.fillOval(x, y, width, height);
            g.setColor(new Color(80, 200, 120));
            g.fillOval(x + 2, y + 2, width - 4, height - 4);
            g.setColor(Color.WHITE);
            g.setFont(new Font("Serif", Font.BOLD, 18));
            g.drawString("+", x + 7, y + 19);
        } else {
            g.setColor(new Color(255, 180, 0, 80));
            g.fillOval(x - 4 - pulse, y - 4 - pulse, width + 8 + pulse * 2, height + 8 + pulse * 2);
            g.setColor(new Color(180, 80, 0));
            g.fillOval(x, y, width, height);
            g.setColor(new Color(255, 150, 0));
            g.fillOval(x + 2, y + 2, width - 4, height - 4);
            g.setColor(Color.YELLOW);
            g.setFont(new Font("Serif", Font.BOLD, 14));
            g.drawString("F", x + 7, y + 18);
        }
    }
}

// Handles simple sound effects
class SoundManager {
    private boolean enabled = true;

    // Plays shooting sound
    public void playShoot() {
        if (!enabled) return;
        playTone(180, 30, 0.2);
    }

    // Plays explosion sound
    public void playExplosion() {
        if (!enabled) return;
        playNoise(250, 0.4);
        new Thread(() -> {
            try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            playNoise(150, 0.25);
        }).start();
    }

    // Plays powerup sound
    public void playPowerUp() {
        if (!enabled) return;
        playTone(440, 50, 0.2);
        new Thread(() -> {
            try { Thread.sleep(60); } catch (InterruptedException ignored) {}
            playTone(660, 80, 0.2);
        }).start();
    }

    // Plays level-up sound
    public void playLevelUp() {
        if (!enabled) return;
        int[] freqs = {330, 415, 494, 660, 830};
        for (int f : freqs) {
            playTone(f, 70, 0.25);
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
        }
    }

    // Plays game-over sound
    public void playGameOver() {
        if (!enabled) return;
        int[] freqs = {330, 294, 262, 220, 165};
        for (int f : freqs) {
            playTone(f, 200, 0.3);
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    // Plays sound when the castle is hit
    public void playCastleHit() {
        if (!enabled) return;
        playNoise(180, 0.35);
    }

    // Plays roar sound at the start of the game
    public void playRoar() {
        if (!enabled) return;
        for (int i = 0; i < 3; i++) {
            playTone(120 - i * 15, 100, 0.4);
            try { Thread.sleep(40); } catch (InterruptedException ignored) {}
        }
    }

    // Creates a tone using Java audio
    private void playTone(int frequency, int durationMs, double volume) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, (int) sampleRate);
                line.start();
                int numSamples = (int) (sampleRate * durationMs / 1000.0);
                byte[] buffer = new byte[numSamples];
                for (int i = 0; i < numSamples; i++) {
                    double angle = i / (sampleRate / frequency) * 2.0 * Math.PI;
                    double envelope = Math.sin(Math.PI * i / numSamples);
                    buffer[i] = (byte) (Math.sin(angle) * volume * envelope * 100);
                }
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (LineUnavailableException ignored) {}
        }).start();
    }

    // Creates noise for explosion-style effects
    private void playNoise(int durationMs, double volume) {
        new Thread(() -> {
            try {
                float sampleRate = 44100;
                AudioFormat format = new AudioFormat(sampleRate, 8, 1, true, false);
                SourceDataLine line = AudioSystem.getSourceDataLine(format);
                line.open(format, (int) sampleRate);
                line.start();
                int numSamples = (int) (sampleRate * durationMs / 1000.0);
                byte[] buffer = new byte[numSamples];
                Random random = new Random();
                for (int i = 0; i < numSamples; i++) {
                    double envelope = Math.sin(Math.PI * i / numSamples);
                    buffer[i] = (byte) ((random.nextDouble() * 2 - 1) * volume * envelope * 100);
                }
                line.write(buffer, 0, buffer.length);
                line.drain();
                line.close();
            } catch (LineUnavailableException ignored) {}
        }).start();
    }
}
