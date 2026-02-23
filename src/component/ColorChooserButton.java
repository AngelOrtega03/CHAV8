package component;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;

public class ColorChooserButton extends JButton {
    /// Attributes
    private Color currentColor;
    public static interface ColorChangedListener {
        public void colorChanged(Color c);
    }
    private List<ColorChangedListener> listeners = new ArrayList<ColorChangedListener>();

    /// Methods
    public ColorChooserButton(Color c) {
        setCurrentColor(c);
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Color newColor = JColorChooser.showDialog(null, "Choose a color:", currentColor);
                setCurrentColor(newColor);
            }
        });
    }

    public Color getCurrentColor() {
        return this.currentColor;
    }

    public void setCurrentColor(Color c) {
        setCurrentColor(c, true);
    }

    public void setCurrentColor(Color c, boolean notify) {
        if (c == null) {
            return;
        }

        this.currentColor = c;
        setIcon(createIcon(this.currentColor,16,16));
        repaint();

        if (notify) {
            for (ColorChangedListener l : this.listeners) {
                l.colorChanged(c);
            }
        }
    }

    public void addColorChangedListener(ColorChangedListener toAdd) {
        listeners.add(toAdd);
    }

    public static ImageIcon createIcon(Color main, int width, int height) {
        BufferedImage image = new BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(main);
        graphics.fillRect(0, 0, width, height);
        graphics.setXORMode(Color.DARK_GRAY);
        graphics.drawRect(0, 0, width-1, height-1);
        image.flush();
        return new ImageIcon(image);
    }

}
