import javax.swing.*;
import java.awt.*;

public class Display extends JPanel {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 32;
    private static final int SCALE = 10;

    private Configuration config;

    private boolean[][] display = new boolean[WIDTH][HEIGHT];

    public Display(Configuration config) {
        this.config = config;
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setBackground(this.config.getBackgroundColor());
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(this.config.getPixelColor());

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                if (this.display[x][y]) {
                    g.fillRect(x * SCALE, y * SCALE, SCALE, SCALE);
                }
            }
        }
    }

    public void setPixel(int x, int y, boolean value) {
        this.display[x][y] = value;
        super.revalidate();
        super.repaint();
    }

    public void clear() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                this.display[x][y] = false;
            }
        }
        repaint();
    }

    public Configuration getConfig() {
        return this.config;
    }

    public void setConfig(Configuration newConfig) {
        this.config = newConfig;
    }
}
