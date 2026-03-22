import component.ColorChooserButton;
import component.KeyMapButton;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.MaskFormatter;
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
    private Configuration prevConfig;

    private File fileSelected;
    private boolean gameClosed;

    private int[] buttonMapping;

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
        this.prevConfig = new Configuration();
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
                prevConfig.setBackgroundColor(c);
            }
        });
        ColorChooserButton pixelColorChooser = new ColorChooserButton(this.config.getPixelColor());
        pixelColorChooser.addColorChangedListener(new ColorChooserButton.ColorChangedListener() {
            @Override
            public void colorChanged(Color c) {
                prevConfig.setPixelColor(c);
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
            backgroundColorChooser.setCurrentColor(this.prevConfig.getBackgroundColor());
            pixelColorChooser.setCurrentColor(this.prevConfig.getPixelColor());
            this.config.setBackgroundColor(this.prevConfig.getBackgroundColor());
            this.config.setPixelColor(this.prevConfig.getPixelColor());
            this.display.updateConfig(this.config);

            this.prevConfig.reset();
        });
        cancelButton.addActionListener(e -> {
            backgroundColorChooser.setCurrentColor(this.config.getBackgroundColor());
            pixelColorChooser.setCurrentColor(this.config.getPixelColor());
            this.colorsFrame.dispose();

            this.prevConfig.reset();
        });

        saveNExit.add(applyButton);
        saveNExit.add(cancelButton);

        this.colorsFrame.add(saveNExit, BorderLayout.SOUTH);
        this.colorsFrame.pack();

        //Clock speed window
        this.clockSpeedFrame = new JFrame("CHAV8 - Clock Speed Configuration");
        this.clockSpeedFrame.setLayout(new BorderLayout());

        JPanel clockSpeedNFrames = new JPanel();
        clockSpeedNFrames.setLayout(new GridLayout(2, 2));

        JPanel clockSpeedSaveNExit = new JPanel();
        clockSpeedSaveNExit.setLayout(new FlowLayout());

        this.clockSpeedFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JComponent clockSpeedContent = (JComponent) this.clockSpeedFrame.getContentPane();
        clockSpeedContent.setBorder(new EmptyBorder(20,20,20,20));
        this.clockSpeedFrame.setResizable(false);
        this.clockSpeedFrame.setVisible(false);
        this.clockSpeedFrame.setLocationRelativeTo(null);

        SpinnerNumberModel clockSpeedPickerModel = new SpinnerNumberModel(this.config.getClockSpeed(), 0.00, 1000.00, 1.00);
        JSpinner clockSpeedPicker = new JSpinner(clockSpeedPickerModel);
        clockSpeedPicker.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSpinner source = (JSpinner) e.getSource();
                prevConfig.setClockSpeed((int)(double)source.getValue());
            }
        });

        JLabel clockSpeedLabel = new JLabel("Clock Speed:");
        clockSpeedLabel.setBorder(new CompoundBorder(clockSpeedLabel.getBorder(), new EmptyBorder(10, 20, 10, 20)));
        clockSpeedNFrames.add(clockSpeedLabel);
        clockSpeedNFrames.add(clockSpeedPicker);

        SpinnerNumberModel frameSkipPickerModel = new SpinnerNumberModel(this.config.getFramesSkipped(), 0.00, 30.00, 1.00);
        JSpinner frameSkipPicker = new JSpinner(frameSkipPickerModel);
        frameSkipPicker.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JSpinner source = (JSpinner) e.getSource();
                prevConfig.setFramesSkipped((int)(double)source.getValue());
            }
        });

        JLabel frameSkipLabel = new JLabel("Frame(s) skipped:");
        frameSkipLabel.setBorder(new CompoundBorder(frameSkipLabel.getBorder(), new EmptyBorder(10, 20, 10, 20)));
        clockSpeedNFrames.add(frameSkipLabel);
        clockSpeedNFrames.add(frameSkipPicker);

        this.clockSpeedFrame.add(clockSpeedNFrames, BorderLayout.CENTER);

        JButton clockSpeedNFramesApplyButton = new JButton("Apply");
        JButton clockSpeedNFramesCancelButton = new JButton("Cancel");
        clockSpeedNFramesApplyButton.addActionListener(e -> {
            clockSpeedPicker.setValue((double)this.prevConfig.getClockSpeed());
            frameSkipPicker.setValue((double)this.prevConfig.getFramesSkipped());

            this.config.setClockSpeed(this.prevConfig.getClockSpeed());
            this.config.setFramesSkipped(this.prevConfig.getFramesSkipped());

            this.display.updateConfig(this.config);

            this.prevConfig.reset();
        });
        clockSpeedNFramesCancelButton.addActionListener(e -> {
            clockSpeedPicker.setValue((double)this.config.getClockSpeed());
            frameSkipPicker.setValue((double)this.config.getFramesSkipped());

            this.clockSpeedFrame.dispose();

            this.prevConfig.reset();
        });

        clockSpeedSaveNExit.add(clockSpeedNFramesApplyButton);
        clockSpeedSaveNExit.add(clockSpeedNFramesCancelButton);

        this.clockSpeedFrame.add(clockSpeedSaveNExit, BorderLayout.SOUTH);
        this.clockSpeedFrame.pack();

        //Button layout window
        this.buttonLayoutFrame = new JFrame("CHAV8 - Button Layout Configuration");
        this.buttonLayoutFrame.setLayout(new BorderLayout());

        JPanel buttonConfig = new JPanel();                     //Button config panel
        buttonConfig.setLayout(new FlowLayout());

        JPanel buttonOriginalLayout = new JPanel();                     //Original CHIP-8 Button layout
        buttonOriginalLayout.setLayout(new GridLayout(4, 4));
        JPanel buttonUserLayout = new JPanel();                         //User defined CHIP-8 Button layout
        buttonUserLayout.setLayout(new GridLayout(4, 4));

        JPanel panelTitles = new JPanel();
        panelTitles.setLayout(new GridLayout(1, 2));

        JLabel originalKeypadTitle = new JLabel("CHIP-8 Keypad");
        JLabel userKeypadTitle = new JLabel("Mapped Keypad");
        originalKeypadTitle.setHorizontalAlignment(SwingConstants.CENTER);
        originalKeypadTitle.setVerticalAlignment(SwingConstants.CENTER);
        userKeypadTitle.setHorizontalAlignment(SwingConstants.CENTER);
        userKeypadTitle.setVerticalAlignment(SwingConstants.CENTER);

        this.buttonMapping = this.config.getKeyboardConfig();

        panelTitles.add(originalKeypadTitle);
        panelTitles.add(userKeypadTitle);

        String keys = "123C456D789EA0BF";
        for (int i = 0; i<16; i++) {
            JButton originalKey = new JButton(String.valueOf(keys.charAt(i)));
            originalKey.setEnabled(false);

            KeyMapButton userKey = new KeyMapButton((String.valueOf((char)(this.buttonMapping[i]))), i);
            userKey.setPreferredSize(new Dimension(50, 25));

            userKey.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    warn(userKey.getIndex(), userKey.getText().charAt(0));
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    warn(userKey.getIndex(), userKey.getText().charAt(0));
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    warn(userKey.getIndex(), userKey.getText().charAt(0));
                }

                public void warn(int index, int value) {
                    buttonMapping[index] = value;
                }
            });

            buttonOriginalLayout.add(originalKey);
            buttonUserLayout.add(userKey);
        }

        buttonConfig.add(buttonOriginalLayout);
        buttonConfig.add(new JLabel("=>"));
        buttonConfig.add(buttonUserLayout);

        JPanel buttonConfigSaveNExit = new JPanel();
        buttonConfigSaveNExit.setLayout(new FlowLayout());

        JButton buttonConfigApplyButton = new JButton("Apply");
        JButton buttonConfigCancelButton = new JButton("Cancel");
        buttonConfigApplyButton.addActionListener(e -> {
            for (int i : this.buttonMapping) {
                System.out.println(i);
            }
            this.config.setKeyboardConfig(this.buttonMapping);

            this.display.updateConfig(this.config);

            this.buttonLayoutFrame.dispose();

            this.prevConfig.reset();
        });
        buttonConfigCancelButton.addActionListener(e -> {
            this.buttonLayoutFrame.dispose();

            this.prevConfig.reset();
        });

        buttonConfigSaveNExit.add(buttonConfigApplyButton);
        buttonConfigSaveNExit.add(buttonConfigCancelButton);

        this.buttonLayoutFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        JComponent buttonLayoutContent = (JComponent) this.buttonLayoutFrame.getContentPane();
        buttonLayoutContent.setBorder(new EmptyBorder(20,20,20,20));
        this.buttonLayoutFrame.setResizable(false);
        this.buttonLayoutFrame.setVisible(false);
        this.buttonLayoutFrame.setLocationRelativeTo(null);

        this.buttonLayoutFrame.add(panelTitles, BorderLayout.NORTH);
        this.buttonLayoutFrame.add(buttonConfig, BorderLayout.CENTER);
        this.buttonLayoutFrame.add(buttonConfigSaveNExit, BorderLayout.SOUTH);

        this.buttonLayoutFrame.pack();

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
        this.buttonLayoutItem.addActionListener(e -> {this.getButtonMapping(); this.buttonLayoutFrame.setVisible(true);});

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

    private void getButtonMapping() {
        this.buttonMapping = this.config.getKeyboardConfig();
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
                    for (int i = 0; i < config.getFramesSkipped(); i++) {
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
