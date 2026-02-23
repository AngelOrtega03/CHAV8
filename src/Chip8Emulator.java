import component.ColorChooserButton;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
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
    private JFrame frame;               //Main program window
    private JFrame colorsFrame;         //Color config window
    private JFrame clockSpeedFrame;     //Clock speed config window
    private JFrame buttonLayoutFrame;   //Button layout config window

    private SwingWorker<Void, Void> emulatorWorker;

    private Configuration config;

    private File fileSelected;
    private boolean gameClosed;

    //File Menu Items
    JMenuItem pauseItem;
    JMenuItem closeGameItem;
    JMenuItem resetItem;

    //Config Menu Items
    JMenuItem colorsItem;
    JMenuItem clockSpeedItem;
    JMenuItem buttonLayoutItem;

    public Chip8Emulator() {
        this.cpu = new CPU();
        this.config = new Configuration();
        this.display = new Display(this.config);

        /// Main program window
        this.frame = new JFrame("CHAV8");
        this.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.frame.setLayout(new java.awt.FlowLayout());
        this.frame.setResizable(false);
        /// Config windows
        //Colors window
        this.colorsFrame = new JFrame("CHAV8 - Color Configuration");
        this.colorsFrame.setLayout(new BorderLayout());

        JPanel colorPickers = new JPanel();
        colorPickers.setLayout(new GridLayout(2,2));

        JPanel saveNExit = new JPanel();
        saveNExit.setLayout(new FlowLayout());

        this.colorsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JComponent content = (JComponent) this.colorsFrame.getContentPane();
        content.setBorder(new EmptyBorder(20,20,20,20));
        this.colorsFrame.setResizable(false);
        this.colorsFrame.setVisible(false);
        this.colorsFrame.setLocationRelativeTo(null);
        ColorChooserButton backgroundColorChooser = new ColorChooserButton(this.config.getBackgroundColor());
        backgroundColorChooser.addColorChangedListener(new ColorChooserButton.ColorChangedListener() {
            @Override
            public void colorChanged(Color c) {
                config.setBackgroundColor(c);
            }
        });
        ColorChooserButton pixelColorChooser = new ColorChooserButton(this.config.getPixelColor());
        pixelColorChooser.addColorChangedListener(new ColorChooserButton.ColorChangedListener() {
            @Override
            public void colorChanged(Color c) {
                config.setPixelColor(c);
            }
        });
        JLabel bgColorLabel = new JLabel("Background Color:");
        bgColorLabel.setBorder(new CompoundBorder(bgColorLabel.getBorder(), new EmptyBorder(10, 20, 10, 20)));
        colorPickers.add(bgColorLabel);
        colorPickers.add(backgroundColorChooser);
        JLabel pxColorLabel = new JLabel("Pixel Color:");
        pxColorLabel.setBorder(new CompoundBorder(pxColorLabel.getBorder(), new EmptyBorder(10,20,10,20)));
        colorPickers.add(pxColorLabel);
        colorPickers.add(pixelColorChooser);

        this.colorsFrame.add(colorPickers, BorderLayout.CENTER);

        JButton applyButton = new JButton("Apply");
        JButton cancelButton = new JButton("Cancel");
        applyButton.addActionListener(e -> {
            this.display.updateConfig(this.config);
        });
        cancelButton.addActionListener(e -> {
            this.colorsFrame.dispose();
        });

        saveNExit.add(applyButton);
        saveNExit.add(cancelButton);

        this.colorsFrame.add(saveNExit, BorderLayout.SOUTH);
        this.colorsFrame.pack();
        //Clock speed window
        this.clockSpeedFrame = new JFrame("CHAV8 - Clock Speed Configuration");
        this.clockSpeedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.clockSpeedFrame.setSize(300,200);
        this.clockSpeedFrame.setResizable(false);
        this.clockSpeedFrame.setVisible(false);
        this.clockSpeedFrame.setLocationRelativeTo(null);
        //Button layout window
        this.buttonLayoutFrame = new JFrame("CHAV8 - Button Layout Configuration");
        this.buttonLayoutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.buttonLayoutFrame.setSize(300,200);
        this.buttonLayoutFrame.setResizable(false);
        this.buttonLayoutFrame.setVisible(false);
        this.buttonLayoutFrame.setLocationRelativeTo(null);

        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenu configMenu = new JMenu("Config");

        /// File Menu
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

        /// Config menu
        //Colors Item
        this.colorsItem = new JMenuItem("Colors");
        this.colorsItem.addActionListener(e -> this.colorsFrame.setVisible(true));
        //Clock speed Item
        this.clockSpeedItem = new JMenuItem("Clock Speed");
        this.clockSpeedItem.addActionListener(e -> this.clockSpeedFrame.setVisible(true));
        //Button layout Item
        this.buttonLayoutItem = new JMenuItem("Button Layout");
        this.buttonLayoutItem.addActionListener(e -> this.buttonLayoutFrame.setVisible(true));

        fileMenu.add(openItem);
        fileMenu.add(runItem);
        fileMenu.add(resetItem);
        fileMenu.add(closeGameItem);
        fileMenu.add(pauseItem);

        configMenu.add(colorsItem);
        configMenu.add(clockSpeedItem);
        configMenu.add(buttonLayoutItem);

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
        int[] keyboardMap = this.display.getConfig().getKeyboardConfig();
        int[] keyboardBytes = this.display.getConfig().getKeyboardByteValues();

        for(int i=0; i<keyboardMap.length; i++) {
            if(keyboardMap[i] == keyCode) {
                return keyboardBytes[i];
            }
        }

        return -1;
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

                    Thread.sleep(config.getClockSpeed());
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
