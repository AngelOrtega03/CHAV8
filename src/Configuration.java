import java.awt.*;
import java.awt.event.KeyEvent;

public class Configuration {
    /// Attributes
    //Colors
    private Color backgroundColor;
    private Color pixelColor;
    //Button layout
    private int[] keyboardConfig;
    private static final int[] keyboardByteValues = { 0x1, 0x2, 0x3, 0xC, 0x4, 0x5, 0x6, 0xD, 0x7, 0x8, 0x9, 0xE, 0xA, 0x0, 0xB, 0xF };
    //TODO: Sound Support
    //Speed
    private int clockSpeed;
    private int framesSkipped;
    
    /// Methods
    public Configuration () {
        this.reset();
    }

    public Configuration(Color bgColor, Color pxColor, int[] keybrdConfig, int clkSpeed, int framesSkipped) {
        this.backgroundColor = bgColor;
        this.pixelColor = pxColor;

        this.keyboardConfig = keybrdConfig;

        this.clockSpeed = clkSpeed;
        this.framesSkipped = framesSkipped;
    }

    public void reset() {
        this.backgroundColor = Color.BLACK;
        this.pixelColor = Color.WHITE;

        this.keyboardConfig = new int[]{
                KeyEvent.VK_1,
                KeyEvent.VK_2,
                KeyEvent.VK_3,
                KeyEvent.VK_4,
                KeyEvent.VK_Q,
                KeyEvent.VK_W,
                KeyEvent.VK_E,
                KeyEvent.VK_R,
                KeyEvent.VK_A,
                KeyEvent.VK_S,
                KeyEvent.VK_D,
                KeyEvent.VK_F,
                KeyEvent.VK_Z,
                KeyEvent.VK_X,
                KeyEvent.VK_C,
                KeyEvent.VK_V
        };

        this.clockSpeed = 16;
        this.framesSkipped = 10;
    }

    public Color getBackgroundColor() {
        return backgroundColor;
    }

    public Color getPixelColor() {
        return pixelColor;
    }

    public int[] getKeyboardConfig() {
        return keyboardConfig;
    }

    public int[] getKeyboardByteValues() { return keyboardByteValues; }

    public int getKeyboardConfigValueByIndex(int index) { return keyboardConfig[index]; }

    public int getClockSpeed() {
        return clockSpeed;
    }

    public int getFramesSkipped() { return framesSkipped; }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setPixelColor(Color pixelColor) {
        this.pixelColor = pixelColor;
    }

    public void setKeyboardConfig(int[] keyboardConfig) {
        this.keyboardConfig = keyboardConfig;
    }

    public void setClockSpeed(int clockSpeed) {
        this.clockSpeed = clockSpeed;
    }

    public void setFramesSkipped(int framesSkipped) { this.framesSkipped = framesSkipped; }
}
