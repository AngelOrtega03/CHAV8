import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class Chip8Emulator {
    private CPU cpu;
    private Display display;
    private JFrame frame;

    public Chip8Emulator() {
        this.cpu = new CPU();
        this.display = new Display();

        this.cpu.loadRom("pong.ch8");

        this.frame = new JFrame("CHAV8");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLayout(new BorderLayout());
        this.frame.setResizable(false);
        this.frame.add(this.display, BorderLayout.CENTER);
        this.frame.pack();
        this.frame.setLocationRelativeTo(null);
        this.frame.setVisible(true);

        this.frame.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
               cpu.keyPressed(mapKey(e.getKeyCode()));
            }

            public void keyReleased(KeyEvent e) {
                cpu.keyReleased(mapKey(e.getKeyCode()));
            }
        });
        this.frame.setFocusable(true);


//        setStartScreen();
    }

    private int mapKey(int keyCode) {
        // Mapear teclado PC a CHIP8 (0x0-0xF)
        return switch (keyCode) {
            case KeyEvent.VK_1 -> 0x1;
            case KeyEvent.VK_2 -> 0x2;
            case KeyEvent.VK_3 -> 0x3;
            case KeyEvent.VK_4 -> 0xC;
            case KeyEvent.VK_Q -> 0x4;
            case KeyEvent.VK_W -> 0x5;
            case KeyEvent.VK_E -> 0x6;
            case KeyEvent.VK_R -> 0xD;
            case KeyEvent.VK_A -> 0x7;
            case KeyEvent.VK_S -> 0x8;
            case KeyEvent.VK_D -> 0x9;
            case KeyEvent.VK_F -> 0xE;
            case KeyEvent.VK_Z -> 0xA;
            case KeyEvent.VK_X -> 0x0;
            case KeyEvent.VK_C -> 0xB;
            case KeyEvent.VK_V -> 0xF;
            default -> -1;
        };
    }

    public void setStartScreen() {
        try (BufferedReader reader = new BufferedReader(new FileReader("pan.txt"))){
            String line;
            int y = 0;
            while ((line = reader.readLine()) != null) {
                for (int x = 0; x < this.display.getWidth() / 10; x++) {
                    if(line.charAt(x) == '1') {
                        this.display.setPixel(x, y, true);
                    }
                }
                y++;
            }
            System.out.println("START IMAGE FINISHED!");
        } catch(IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void run() {
        try {
            setStartScreen();
            TimeUnit.SECONDS.sleep(4);
            display.clear();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        while(true) {
            for (int i = 0; i < 10; i++) {
                this.cpu.cycle();
            }

            boolean[][] videoMatrix = this.cpu.getVideoMatrix();
            for(int y=0; y<32; y++) {
                for (int x=0; x<64; x++) {
                    this.display.setPixel(x, y, videoMatrix[x][y]);
                }
            }

            try {
                Thread.sleep(16);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
