import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
        Chip8Emulator emu = new Chip8Emulator();

        if(!args[0].isEmpty()) {
            try{
                Path path = Paths.get(args[0]);
                if (!Files.exists(path)) {
                    System.err.println("File doesn't exist!");
                }
                else {
                    emu.setRom(args[0]);
                }
            }catch(IllegalArgumentException e) {
                System.err.println(e.getMessage());
            }
        }


    }
}