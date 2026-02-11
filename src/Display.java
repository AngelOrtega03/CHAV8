import javax.swing.*;
import java.awt.*;

public class Display extends JPanel {
    private static final int WIDTH = 64;
    private static final int HEIGHT = 32;
    private static final int SCALE = 10;

    private boolean[][] display = new boolean[WIDTH][HEIGHT];

    public Display() {
        setPreferredSize(new Dimension(WIDTH * SCALE, HEIGHT * SCALE));
        setBackground(Color.BLACK);
        setFocusable(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.GREEN);

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
}
