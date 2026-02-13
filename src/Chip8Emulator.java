import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Time;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Chip8Emulator {
    private CPU cpu;
    private Display display;
    private JFrame frame;
//
    private SwingWorker<Void, Void> emulatorWorker;

    private File fileSelected;
    private boolean gameClosed;

    JMenuItem pauseItem;
    JMenuItem closeGameItem;
    JMenuItem resetItem;

    public Chip8Emulator() {
        this.cpu = new CPU();
        this.display = new Display();

        this.frame = new JFrame("CHAV8");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLayout(new java.awt.FlowLayout());
        this.frame.setResizable(false);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu configMenu = new JMenu("Config");

        //Pause Item
        this.pauseItem = new JMenuItem("Pause");
        this.pauseItem.addActionListener(e -> stop());
        this.pauseItem.setVisible(false);
        //Close Item
        this.closeGameItem = new JMenuItem("Close game");
        this.closeGameItem.addActionListener(e -> closeGame());
        this.closeGameItem.setVisible(false);
        //Reset Item
        this.resetItem = new JMenuItem("Reset");
        this.resetItem.addActionListener(e -> resetGame());
        this.resetItem.setVisible(false);
        //Run Item
        JMenuItem runItem = new JMenuItem("Run");
        Action runItemAction = new AbstractAction("Run") {
            @Override
            public void actionPerformed(ActionEvent e) {
                resetItem.setVisible(true);
                pauseItem.setVisible(true);
                closeGameItem.setVisible(true);
                run();
            }
        };
        runItem.setAction(runItemAction);
        runItem.setVisible(false);
        //Open Item
        JMenuItem openItem = new JMenuItem("Open");
        Action openItemAction = new AbstractAction("Open") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                try {
                    String currentDir = System.getProperty("user.dir");
                    fileChooser.setCurrentDirectory(new File(currentDir,"roms").getCanonicalFile());
                } catch (IOException ex) {
                    throw new RuntimeException(ex.getMessage());
                }
                int result = fileChooser.showOpenDialog(null);
                if (result == JFileChooser.APPROVE_OPTION) {
                    fileSelected = fileChooser.getSelectedFile();
                    cpu.loadRom(fileSelected.getAbsolutePath());
                    runItem.setText("Run - "+fileSelected.getAbsolutePath());
                    runItem.setVisible(true);
                }
            }
        };
        openItem.setAction(openItemAction);

        fileMenu.add(openItem);
        fileMenu.add(runItem);
        fileMenu.add(resetItem);
        fileMenu.add(closeGameItem);
        fileMenu.add(pauseItem);

        menuBar.add(fileMenu);
        menuBar.add(configMenu);
        this.frame.setJMenuBar(menuBar);

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

        setStartScreen();
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
            this.display.clear();
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
            //System.out.println("START IMAGE FINISHED!");
        } catch(IOException e) {
            System.err.println(e.getMessage());
        }
    }

    public void setRom(String filename) {
        this.cpu.loadRom(filename);
    }

    public void run() {
        if (this.gameClosed) {
            this.gameClosed = false;
            resetGame();
        }

        display.clear();

        if (emulatorWorker != null && !emulatorWorker.isDone()) {
            return;
        }

        emulatorWorker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                while (!isCancelled()) {
                    for (int i = 0; i < 10; i++) {
                        cpu.cycle();
                    }

                    publish();

                    Thread.sleep(16);
                }
                return null;
            }

            @Override
            protected void process(List<Void> chunks) {
                boolean[][] videoMatrix = cpu.getVideoMatrix();
                for (int y = 0; y < 32; y++) {
                    for (int x = 0; x < 64; x++) {
                        display.setPixel(x, y, videoMatrix[x][y]);
                    }
                }
                display.repaint();
            }
        };

        emulatorWorker.execute();
    }

    public void resetGame() {
        stop();
        cpu.loadRom(fileSelected.getAbsolutePath());
        run();
    }

    public void stop() {
        if (emulatorWorker != null) {
            emulatorWorker.cancel(true);
        }
    }

    public void closeGame() {
        stop();
        setStartScreen();
        resetItem.setVisible(false);
        pauseItem.setVisible(false);
        closeGameItem.setVisible(false);
    }
}
