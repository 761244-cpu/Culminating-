

    import javax.sound.sampled.AudioFormat;
    import javax.sound.sampled.AudioSystem;
    import javax.sound.sampled.LineUnavailableException;
    import javax.sound.sampled.SourceDataLine;
    import javax.swing.JFrame;
    import javax.swing.JPanel;
    import javax.swing.SwingUtilities;
    import javax.swing.Timer;
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
     
    public class SkyDefender {
     
        public static void main(String[] args) {
            SwingUtilities.invokeLater(() -> {
                JFrame frame = new JFrame("Sky Defender");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(false);
     
                GamePanel gamePanel = new GamePanel();
                frame.add(gamePanel);
     
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
     
                gamePanel.startGame();
            });
        }
    }
     
    class GamePanel extends JPanel implements ActionListener, KeyListener {
     
        public static final int WIDTH = 900;
        public static final int HEIGHT = 600;
     
        private static final int FPS = 60;
        private static final int PLAYER_FIRE_COOLDOWN = 12;
     
        private Timer gameTimer;
        private boolean running = true;
        private boolean gameOver = false;
        private boolean showStartScreen = true;
     
        private Player player;
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Bullet> bullets = new ArrayList<>();
        private final List<Bullet> enemyBullets = new ArrayList<>();
        private final List<Cloud> clouds = new ArrayList<>();
        private final List<Particle> particles = new ArrayList<>();
        private final List<PowerUp> powerUps = new ArrayList<>();
     
        private final Random random = new Random();
     
        private int score = 0;
        private int highScore = 0;
        private int level = 1;
        private int frameCount = 0;
        private int fireCooldown = 0;
        private int enemySpawnTimer = 0;
        private int powerUpSpawnTimer = 0;
        private double scrollOffset = 0;
     
        private final SoundManager sound = new SoundManager();
     
        public GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setBackground(new Color(135, 206, 250));
            setFocusable(true);
            addKeyListener(this);
        }
     
        public void startGame() {
            resetGame();
            gameTimer = new Timer(1000 / FPS, this);
            gameTimer.start();
        }
     
        private void resetGame() {
            player = new Player(80, HEIGHT / 2 - 25);
            enemies.clear();
            bullets.clear();
            enemyBullets.clear();
            powerUps.clear();
            particles.clear();
            if (clouds.isEmpty()) {
                for (int i = 0; i < 8; i++) {
                    clouds.add(new Cloud(random.nextInt(WIDTH), random.nextInt(HEIGHT), 0.2 + random.nextDouble() * 0.6));
                }
            }
            score = 0;
            level = 1;
            frameCount = 0;
            fireCooldown = 0;
            enemySpawnTimer = 0;
            powerUpSpawnTimer = 0;
            gameOver = false;
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
     
            frameCount++;
            scrollOffset += 2 + level * 0.3;
            if (scrollOffset > 2000) scrollOffset -= 2000;
     
            updateClouds();
            player.update();
            updateBullets();
            updateEnemies();
            updatePowerUps();
            updateParticles();
            checkCollisions();
            handleSpawning();
            updateLevel();
        }
     
        private void updateClouds() {
            for (Cloud c : clouds) {
                c.x -= c.speed;
                if (c.x + c.width < 0) {
                    c.x = WIDTH + random.nextInt(200);
                    c.y = random.nextInt(HEIGHT - 100);
                    c.width = 60 + random.nextInt(100);
                    c.height = 30 + random.nextInt(40);
                }
            }
        }
     
        private void updateBullets() {
            Iterator<Bullet> it = bullets.iterator();
            while (it.hasNext()) {
                Bullet b = it.next();
                b.x += b.speed;
                if (b.x > WIDTH) it.remove();
            }
            Iterator<Bullet> eit = enemyBullets.iterator();
            while (eit.hasNext()) {
                Bullet b = eit.next();
                b.x += b.speed;
                b.y += b.vy;
                if (b.x < -20 || b.x > WIDTH + 20 || b.y < -20 || b.y > HEIGHT + 20) eit.remove();
            }
            if (fireCooldown > 0) fireCooldown--;
        }
     
        private void updateEnemies() {
            for (Enemy en : enemies) {
                en.update();
                if (en.canShoot() && random.nextDouble() < 0.01 + level * 0.002) {
                    enemyBullets.add(new Bullet(en.x, en.y + en.height / 2 - 3, -5, 0, false));
                    sound.playShoot();
                }
            }
            Iterator<Enemy> it = enemies.iterator();
            while (it.hasNext()) {
                Enemy en = it.next();
                if (en.x + en.width < 0 || en.y > HEIGHT + 100) {
                    if (en.y > HEIGHT + 100) {
                        player.takeDamage(5);
                    }
                    it.remove();
                }
            }
        }
     
        private void updatePowerUps() {
            Iterator<PowerUp> it = powerUps.iterator();
            while (it.hasNext()) {
                PowerUp p = it.next();
                p.x -= 2;
                if (p.x + p.width < 0) it.remove();
            }
        }
     
        private void updateParticles() {
            Iterator<Particle> it = particles.iterator();
            while (it.hasNext()) {
                Particle p = it.next();
                p.x += p.vx;
                p.y += p.vy;
                p.vy += 0.15;
                p.life--;
                if (p.life <= 0) it.remove();
            }
        }
     
        private void checkCollisions() {
            Rectangle pr = player.getBounds();
     
            for (Bullet eb : enemyBullets) {
                if (pr.intersects(eb.getBounds())) {
                    player.takeDamage(10);
                    spawnExplosion(eb.x, eb.y, Color.ORANGE, 8);
                    eb.x = -1000;
                    if (player.lives <= 0) endGame();
                }
            }
            enemyBullets.removeIf(b -> b.x < -500);
     
            for (Enemy en : enemies) {
                if (pr.intersects(en.getBounds())) {
                    player.takeDamage(30);
                    spawnExplosion(en.x, en.y, Color.RED, 25);
                    en.x = -200;
                    if (player.lives <= 0) endGame();
                }
            }
     
            Iterator<Bullet> bit = bullets.iterator();
            while (bit.hasNext()) {
                Bullet b = bit.next();
                Rectangle br = b.getBounds();
                boolean hit = false;
                for (Enemy en : enemies) {
                    if (en.getBounds().intersects(br)) {
                        en.health--;
                        if (en.health <= 0) {
                            spawnExplosion(en.x + en.width / 2.0, en.y + en.height / 2.0, Color.ORANGE, 30);
                            sound.playExplosion();
                            score += en.points;
                            if (random.nextDouble() < 0.12) {
                                powerUps.add(new PowerUp(en.x, en.y));
                            }
                            en.x = -200;
                        } else {
                            spawnExplosion(b.x, b.y, Color.YELLOW, 5);
                        }
                        hit = true;
                        break;
                    }
                }
                if (hit) bit.remove();
            }
     
            Iterator<PowerUp> pit = powerUps.iterator();
            while (pit.hasNext()) {
                PowerUp p = pit.next();
                if (pr.intersects(p.getBounds())) {
                    sound.playPowerUp();
                    if (p.type == PowerUp.Type.HEALTH) {
                        player.lives = Math.min(player.maxLives, player.lives + 1);
                        spawnExplosion(p.x, p.y, Color.GREEN, 20);
                    } else {
                        player.activatePower();
                        spawnExplosion(p.x, p.y, Color.CYAN, 20);
                    }
                    pit.remove();
                }
            }
        }
     
        private void handleSpawning() {
            int spawnRate = Math.max(40, 90 - level * 5);
            enemySpawnTimer++;
            if (enemySpawnTimer >= spawnRate) {
                enemySpawnTimer = 0;
                spawnEnemy();
            }
     
            powerUpSpawnTimer++;
            if (powerUpSpawnTimer > 600 && random.nextDouble() < 0.005) {
                powerUpSpawnTimer = 0;
                double y = 50 + random.nextDouble() * (HEIGHT - 200);
                powerUps.add(new PowerUp(WIDTH + 30, (int) y));
            }
        }
     
        private void spawnEnemy() {
            int type = random.nextInt(100);
            int y = 40 + random.nextInt(HEIGHT - 150);
            if (type < 60) {
                enemies.add(new Enemy(WIDTH + 40, y, Enemy.Type.SCOUT, level));
            } else if (type < 90) {
                enemies.add(new Enemy(WIDTH + 40, y, Enemy.Type.FIGHTER, level));
            } else {
                enemies.add(new Enemy(WIDTH + 60, y, Enemy.Type.BOMBER, level));
            }
        }
     
        private void updateLevel() {
            int newLevel = 1 + score / 500;
            if (newLevel > level) {
                level = newLevel;
                sound.playLevelUp();
            }
        }
     
        private void spawnExplosion(double x, double y, Color color, int count) {
            for (int i = 0; i < count; i++) {
                double angle = random.nextDouble() * Math.PI * 2;
                double speed = 1 + random.nextDouble() * 4;
                particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color, 20 + random.nextInt(20)));
            }
        }
     
        private void endGame() {
            gameOver = true;
            if (score > highScore) highScore = score;
            sound.playGameOver();
        }
     
        private void playerShoot() {
            if (fireCooldown > 0 || gameOver || showStartScreen) return;
            fireCooldown = player.hasPower ? PLAYER_FIRE_COOLDOWN / 2 : PLAYER_FIRE_COOLDOWN;
            int bx = player.x + player.width - 5;
            int by = player.y + player.height / 2 - 4;
            bullets.add(new Bullet(bx, by, 12, 0, true));
            if (player.hasPower) {
                bullets.add(new Bullet(bx, by - 14, 12, 0, true));
                bullets.add(new Bullet(bx, by + 14, 12, 0, true));
            }
            sound.playShoot();
        }
     
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
     
            drawSky(g2);
            drawMountains(g2);
            drawClouds(g2);
     
            if (!showStartScreen) {
                for (PowerUp p : powerUps) p.draw(g2);
                for (Enemy en : enemies) en.draw(g2);
                for (Bullet b : bullets) b.draw(g2);
                for (Bullet b : enemyBullets) b.draw(g2);
                for (Particle p : particles) p.draw(g2);
                if (player != null) player.draw(g2);
     
                drawHUD(g2);
     
                if (gameOver) drawGameOver(g2);
            } else {
                drawStartScreen(g2);
            }
        }
     
        private void drawSky(Graphics2D g2) {
            GradientPaint gp = new GradientPaint(0, 0, new Color(100, 180, 255), 0, HEIGHT, new Color(220, 240, 255));
            g2.setPaint(gp);
            g2.fillRect(0, 0, WIDTH, HEIGHT);
            g2.setColor(new Color(255, 255, 200, 60));
            g2.fillOval(700, 60, 90, 90);
            g2.setColor(new Color(255, 255, 230, 100));
            g2.fillOval(720, 80, 50, 50);
        }
     
        private void drawMountains(Graphics2D g2) {
            int baseY = HEIGHT - 100;
            int offset = (int) (scrollOffset * 0.3) % 400;
            g2.setColor(new Color(70, 110, 140));
            for (int i = -1; i < 4; i++) {
                int x = i * 300 - offset;
                int[] xs = {x, x + 150, x + 300};
                int[] ys = {baseY, baseY - 120, baseY};
                g2.fillPolygon(xs, ys, 3);
            }
            g2.setColor(new Color(60, 140, 90));
            g2.fillRect(0, HEIGHT - 30, WIDTH, 30);
            g2.setColor(new Color(50, 120, 70));
            for (int i = 0; i < WIDTH; i += 18) {
                int h = 6 + (i * 37 % 10);
                g2.fillRect(i, HEIGHT - 30 - h, 8, h);
            }
        }
     
        private void drawClouds(Graphics2D g2) {
            g2.setColor(new Color(255, 255, 255, 220));
            for (Cloud c : clouds) c.draw(g2);
        }
     
        private void drawHUD(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(10, 10, 220, 90, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 18));
            g2.drawString("SCORE: " + score, 25, 35);
            g2.drawString("LEVEL: " + level, 25, 60);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("HI: " + highScore, 150, 60);
     
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRoundRect(WIDTH - 200, 10, 180, 50, 15, 15);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.drawString("LIVES:", WIDTH - 185, 40);
            for (int i = 0; i < player.lives; i++) {
                g2.setColor(new Color(255, 80, 80));
                g2.fillOval(WIDTH - 110 + i * 22, 25, 18, 18);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Arial", Font.BOLD, 12));
                g2.drawString("\u2665", WIDTH - 107 + i * 22, 38);
            }
            g2.setFont(new Font("Arial", Font.PLAIN, 12));
        }
     
        private void drawStartScreen(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 130));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
     
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 64));
            String title = "SKY DEFENDER";
            int tw = g2.getFontMetrics().stringWidth(title);
            g2.drawString(title, (WIDTH - tw) / 2, 180);
     
            g2.setFont(new Font("Arial", Font.PLAIN, 22));
            String[] lines = {
                    "Arrow Keys or WASD - Move",
                    "SPACE - Shoot",
                    "P - Pause / Resume",
                    "",
                    "Destroy enemies to score points!",
                    "Collect power-ups for upgrades!",
                    "",
                    "Press any key to start"
            };
            int y = 260;
            for (String line : lines) {
                int lw = g2.getFontMetrics().stringWidth(line);
                g2.drawString(line, (WIDTH - lw) / 2, y);
                y += 30;
            }
     
            g2.setFont(new Font("Arial", Font.BOLD, 16));
            g2.setColor(new Color(255, 220, 100));
            g2.drawString("High Score: " + highScore, WIDTH / 2 - 70, HEIGHT - 40);
        }
     
        private void drawGameOver(Graphics2D g2) {
            g2.setColor(new Color(0, 0, 0, 180));
            g2.fillRect(0, 0, WIDTH, HEIGHT);
     
            g2.setColor(new Color(255, 80, 80));
            g2.setFont(new Font("Arial", Font.BOLD, 72));
            String go = "GAME OVER";
            int gw = g2.getFontMetrics().stringWidth(go);
            g2.drawString(go, (WIDTH - gw) / 2, HEIGHT / 2 - 40);
     
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Arial", Font.BOLD, 28));
            String sc = "Final Score: " + score;
            int sw = g2.getFontMetrics().stringWidth(sc);
            g2.drawString(sc, (WIDTH - sw) / 2, HEIGHT / 2 + 10);
     
            g2.setFont(new Font("Arial", Font.PLAIN, 20));
            String hs = "High Score: " + highScore;
            int hw = g2.getFontMetrics().stringWidth(hs);
            g2.drawString(hs, (WIDTH - hw) / 2, HEIGHT / 2 + 45);
     
            g2.setColor(new Color(255, 220, 100));
            g2.setFont(new Font("Arial", Font.BOLD, 22));
            String rs = "Press R to Restart";
            int rw = g2.getFontMetrics().stringWidth(rs);
            g2.drawString(rs, (WIDTH - rw) / 2, HEIGHT / 2 + 100);
        }
     
        @Override
        public void keyPressed(KeyEvent e) {
            int k = e.getKeyCode();
            if (showStartScreen) {
                showStartScreen = false;
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
     
    class Player {
        public int x, y;
        public final int width = 50;
        public final int height = 30;
        public int lives = 3;
        public final int maxLives = 5;
        public boolean hasPower = false;
        private int powerTimer = 0;
     
        public boolean left, right, up, down;
        private final int speed = 5;
        private int animFrame = 0;
        private int invulnTimer = 0;
     
        public Player(int x, int y) {
            this.x = x;
            this.y = y;
        }
     
        public void update() {
            if (left) x -= speed;
            if (right) x += speed;
            if (up) y -= speed;
            if (down) y += speed;
            if (x < 10) x = 10;
            if (x > GamePanel.WIDTH - width - 10) x = GamePanel.WIDTH - width - 10;
            if (y < 40) y = 40;
            if (y > GamePanel.HEIGHT - height - 40) y = GamePanel.HEIGHT - height - 40;
            animFrame++;
            if (invulnTimer > 0) invulnTimer--;
            if (powerTimer > 0) {
                powerTimer--;
                if (powerTimer == 0) hasPower = false;
            }
        }
     
        public void takeDamage(int dmg) {
            if (invulnTimer > 0) return;
            lives -= (dmg >= 30 ? 1 : 0);
            if (dmg < 30) lives--;
            if (lives < 0) lives = 0;
            invulnTimer = 60;
            hasPower = false;
            powerTimer = 0;
        }
     
        public void activatePower() {
            hasPower = true;
            powerTimer = 60 * 15;
        }
     
        public Rectangle getBounds() {
            return new Rectangle(x + 5, y + 5, width - 10, height - 10);
        }
     
        public void draw(Graphics2D g) {
            if (invulnTimer > 0 && (animFrame / 4) % 2 == 0) return;
     
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
     
            int[] bodyX = {x, x + width - 5, x + width, x + width - 5, x};
            int[] bodyY = {y + height / 2, y, y + height / 2, y + height, y + height / 2};
            g.setColor(new Color(60, 100, 200));
            g.fillPolygon(bodyX, bodyY, 5);
     
            g.setColor(new Color(40, 70, 160));
            int[] wingX = {x + 10, x + 25, x + 25, x + 10};
            int[] wingY = {y + height + 2, y + height + 2, y + height - 6, y + height - 6};
            g.fillPolygon(wingX, wingY, 4);
     
            int[] wingX2 = {x + 10, x + 25, x + 25, x + 10};
            int[] wingY2 = {y - 2, y - 2, y + 6, y + 6};
            g.setColor(new Color(40, 70, 160));
            g.fillPolygon(wingX2, wingY2, 4);
     
            g.setColor(new Color(150, 220, 255));
            g.fillOval(x + width - 18, y + height / 2 - 5, 12, 10);
     
            g.setColor(Color.YELLOW);
            g.fillOval(x - 8, y + height / 2 - 3, 8, 6);
            int flicker = (animFrame / 3) % 3;
            g.setColor(new Color(255, 150, 0, 200 - flicker * 60));
            g.fillOval(x - 14 - flicker * 2, y + height / 2 - 2, 6 + flicker * 2, 4);
     
            if (hasPower) {
                g.setColor(new Color(100, 200, 255, 100));
                g.fillOval(x - 4, y - 2, width + 8, height + 4);
            }
        }
    }
     
    class Enemy {
        public enum Type { SCOUT, FIGHTER, BOMBER }
     
        public int x, y;
        public final int width, height;
        public int health;
        public int points;
        public Type type;
        private int animFrame = 0;
        private final Random random = new Random();
        private int shootCooldown;
        private final int baseSpeed;
        private final int level;
     
        public Enemy(int x, int y, Type type, int level) {
            this.x = x;
            this.y = y;
            this.type = type;
            this.level = level;
            switch (type) {
                case SCOUT:
                    width = 38; height = 22;
                    health = 1 + level / 3;
                    points = 50;
                    baseSpeed = 4;
                    shootCooldown = 100 + random.nextInt(80);
                    break;
                case FIGHTER:
                    width = 46; height = 28;
                    health = 2 + level / 2;
                    points = 100;
                    baseSpeed = 3;
                    shootCooldown = 70 + random.nextInt(60);
                    break;
                case BOMBER:
                    width = 60; height = 40;
                    health = 4 + level / 2;
                    points = 200;
                    baseSpeed = 2;
                    shootCooldown = 50 + random.nextInt(40);
                    break;
                default:
                    width = 38; height = 22;
                    health = 1; points = 50; baseSpeed = 4; shootCooldown = 100;
            }
        }
     
        public void update() {
            animFrame++;
            x -= baseSpeed;
            if (type == Type.SCOUT) {
                y += Math.sin((x + animFrame) * 0.05) * 1.2;
            } else if (type == Type.FIGHTER) {
                if (animFrame % 60 < 30) y += 0.5;
                else y -= 0.5;
            }
            if (shootCooldown > 0) shootCooldown--;
        }
     
        public boolean canShoot() {
            if (shootCooldown <= 0) {
                shootCooldown = 90 + random.nextInt(60);
                return true;
            }
            return false;
        }
     
        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
     
        public void draw(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            switch (type) {
                case SCOUT:
                    drawScout(g);
                    break;
                case FIGHTER:
                    drawFighter(g);
                    break;
                case BOMBER:
                    drawBomber(g);
                    break;
            }
        }
     
        private void drawScout(Graphics2D g) {
            int[] bx = {x + width, x + 5, x, x + 5};
            int[] by = {y + height / 2, y, y + height / 2, y + height};
            g.setColor(new Color(220, 60, 60));
            g.fillPolygon(bx, by, 4);
            g.setColor(new Color(160, 30, 30));
            g.fillRect(x + 8, y - 2, 12, 4);
            g.fillRect(x + 8, y + height - 2, 12, 4);
            g.setColor(new Color(255, 200, 100));
            g.fillOval(x + 5, y + height / 2 - 3, 6, 6);
        }
     
        private void drawFighter(Graphics2D g) {
            int[] bx = {x + width, x + 8, x, x + 8};
            int[] by = {y + height / 2, y, y + height / 2, y + height};
            g.setColor(new Color(180, 50, 80));
            g.fillPolygon(bx, by, 4);
            g.setColor(new Color(120, 30, 60));
            int[] wx = {x + 10, x + 30, x + 30, x + 10};
            int[] wy = {y + height + 2, y + height + 2, y + height - 8, y + height - 8};
            g.fillPolygon(wx, wy, 4);
            g.setColor(new Color(255, 200, 100));
            g.fillOval(x + 8, y + height / 2 - 3, 8, 6);
        }
     
        private void drawBomber(Graphics2D g) {
            g.setColor(new Color(90, 90, 90));
            g.fillRoundRect(x, y + 10, width, height - 20, 10, 10);
            g.setColor(new Color(60, 60, 60));
            g.fillOval(x + width - 15, y + height / 2 - 4, 12, 8);
            g.setColor(new Color(50, 50, 50));
            g.fillRect(x + 10, y + 4, 8, 6);
            g.fillRect(x + width - 18, y + 4, 8, 6);
            g.fillRect(x + 10, y + height - 10, 8, 6);
            g.fillRect(x + width - 18, y + height - 10, 8, 6);
            g.setColor(new Color(255, 200, 100));
            g.fillOval(x + 4, y + height / 2 - 3, 6, 6);
        }
    }
     
    class Bullet {
        public int x, y;
        public int speed;
        public int vy;
        public final int width = 12;
        public final int height = 6;
        public final boolean playerBullet;
     
        public Bullet(int x, int y, int speed, int vy, boolean playerBullet) {
            this.x = x;
            this.y = y;
            this.speed = speed;
            this.vy = vy;
            this.playerBullet = playerBullet;
        }
     
        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }
     
        public void draw(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (playerBullet) {
                g.setColor(new Color(255, 255, 100, 80));
                g.fillOval(x - 4, y - 2, width + 8, height + 4);
                g.setColor(Color.YELLOW);
                g.fillRoundRect(x, y, width, height, 4, 4);
                g.setColor(Color.WHITE);
                g.fillRect(x + 8, y + 1, 3, height - 2);
            } else {
                g.setColor(new Color(255, 80, 0, 80));
                g.fillOval(x - 4, y - 2, width + 8, height + 4);
                g.setColor(new Color(255, 140, 0));
                g.fillOval(x, y, width, height);
                g.setColor(Color.YELLOW);
                g.fillOval(x + 3, y + 1, width - 6, height - 2);
            }
        }
    }
     
    class Cloud {
        public int x, y;
        public int width, height;
        public double speed;
     
        public Cloud(int x, int y, double speed) {
            this.x = x;
            this.y = y;
            this.speed = speed * 1.5;
            this.width = 80;
            this.height = 30;
        }
     
        public void draw(Graphics2D g) {
            g.setColor(new Color(255, 255, 255, 230));
            g.fillOval(x, y, width, height);
            g.fillOval(x + 15, y - 10, width - 20, height);
            g.fillOval(x + 30, y - 5, width - 30, height - 5);
            g.fillOval(x + 10, y + 10, width - 20, height - 10);
        }
    }
     
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
     
    class PowerUp {
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
                g.setColor(new Color(0, 255, 0, 80));
                g.fillOval(x - 4 - pulse, y - 4 - pulse, width + 8 + pulse * 2, height + 8 + pulse * 2);
                g.setColor(new Color(0, 180, 0));
                g.fillRoundRect(x, y, width, height, 6, 6);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 16));
                g.drawString("+", x + 7, y + 18);
            } else {
                g.setColor(new Color(0, 200, 255, 80));
                g.fillOval(x - 4 - pulse, y - 4 - pulse, width + 8 + pulse * 2, height + 8 + pulse * 2);
                g.setColor(new Color(0, 150, 220));
                g.fillRoundRect(x, y, width, height, 6, 6);
                g.setColor(Color.WHITE);
                g.setFont(new Font("Arial", Font.BOLD, 14));
                g.drawString("S", x + 7, y + 18);
            }
        }
    }
     
    class SoundManager {
        private boolean enabled = true;
     
        public void playShoot() {
            if (!enabled) return;
            playTone(880, 50, 0.15);
        }
     
        public void playExplosion() {
            if (!enabled) return;
            playNoise(200, 0.3);
        }
     
        public void playPowerUp() {
            if (!enabled) return;
            playTone(660, 60, 0.2);
            try { Thread.sleep(40); } catch (InterruptedException ignored) {}
            playTone(880, 60, 0.2);
        }
     
        public void playLevelUp() {
            if (!enabled) return;
            int[] freqs = {523, 659, 784, 1047};
            for (int f : freqs) {
                playTone(f, 80, 0.2);
                try { Thread.sleep(60); } catch (InterruptedException ignored) {}
            }
        }
     
        public void playGameOver() {
            if (!enabled) return;
            int[] freqs = {440, 392, 349, 294};
            for (int f : freqs) {
                playTone(f, 150, 0.25);
                try { Thread.sleep(80); } catch (InterruptedException ignored) {}
            }
        }
     
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
                        double envelope = 1.0 - (double) i / numSamples;
                        buffer[i] = (byte) (Math.sin(angle) * volume * envelope * 100);
                    }
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.close();
                } catch (LineUnavailableException ignored) {}
            }).start();
        }
     
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
                        double envelope = 1.0 - (double) i / numSamples;
                        buffer[i] = (byte) ((random.nextDouble() * 2 - 1) * volume * envelope * 100);
                    }
                    line.write(buffer, 0, buffer.length);
                    line.drain();
                    line.close();
                } catch (LineUnavailableException ignored) {}
            }).start();
        }
    }
