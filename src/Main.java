import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        Chip8Emulator emu = new Chip8Emulator();
        emu.run();

//        boolean value = true;

        //TEST FILL N OUT
//        for (int x = 0; x < myDisplay.getWidth() / 10; x++) {
//            for (int y = 0; y < myDisplay.getHeight() / 10; y++) {
//                try {
//                    TimeUnit.MILLISECONDS.sleep(1);
//                    myDisplay.setPixel(x, y, value);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }
//
//            if (x == ((myDisplay.getWidth() / 10) - 1)) {
//                x = -1;
//                value = !value;
//            }
//        }
    }
}