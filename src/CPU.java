import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class CPU {
    //OPCODE
    private short opcode;
    //4K Memory
    private short[] memory = new short[4096];
    //Registers (15 general purpose, 1 carry flag)
    private short[] registers = new short[16];
    //Other registers (index and program counter)
    private short index;
    private short pc;
    //Graphics
    private boolean[] video = new boolean[64 * 32];
    //Delay and sound timer
    private int delayTimer;
    private int soundTimer;
    //Stack and stack pointer
    private short[] stack = new short[16];
    private short sp;
    //Keypad
    private boolean[] keyboard = new boolean[16];
    //Random number generator
    private Random random = new Random();
    //ROM Loaded flag
    private boolean romLoaded;

    //Op tables
    private Map<Integer, Runnable> table;
    private Map<Integer, Runnable> table0;
    private Map<Integer, Runnable> table8;
    private Map<Integer, Runnable> tableE;
    private Map<Integer, Runnable> tableF;

    //Start address
    static final int START_ADDRESS = 0x200;
    static final int FONTSET_START_ADDRESS = 0x50;

    //Font (size and initialization)
    static final int FONTSET_SIZE = 80;
    public short[] fontset = {
            0xF0, 0x90, 0x90, 0x90, 0xF0, // 0
            0x20, 0x60, 0x20, 0x20, 0x70, // 1
            0xF0, 0x10, 0xF0, 0x80, 0xF0, // 2
            0xF0, 0x10, 0xF0, 0x10, 0xF0, // 3
            0x90, 0x90, 0xF0, 0x10, 0x10, // 4
            0xF0, 0x80, 0xF0, 0x10, 0xF0, // 5
            0xF0, 0x80, 0xF0, 0x90, 0xF0, // 6
            0xF0, 0x10, 0x20, 0x40, 0x40, // 7
            0xF0, 0x90, 0xF0, 0x90, 0xF0, // 8
            0xF0, 0x90, 0xF0, 0x10, 0xF0, // 9
            0xF0, 0x90, 0xF0, 0x90, 0x90, // A
            0xE0, 0x90, 0xE0, 0x90, 0xE0, // B
            0xF0, 0x80, 0x80, 0x80, 0xF0, // C
            0xE0, 0x90, 0x90, 0x90, 0xE0, // D
            0xF0, 0x80, 0xF0, 0x80, 0xF0, // E
            0xF0, 0x80, 0xF0, 0x80, 0x80  // F
    };

    private void reset() {
        this.pc = START_ADDRESS;
        this.delayTimer = 0;
        this.soundTimer = 0;
        this.sp = 0;
        this.index = 0;

        Arrays.fill(this.registers, (short)0);
        Arrays.fill(this.memory, (short)0);
        Arrays.fill(this.video, false);
        Arrays.fill(this.stack, (short)0);

        this.romLoaded = false;

        for (int i = 0; i < FONTSET_SIZE; ++i) {
            this.memory[FONTSET_START_ADDRESS + i] = (short)this.fontset[i];
        }

        // Initialize tables as HashMaps
        table = new HashMap<>();
        table0 = new HashMap<>();
        table8 = new HashMap<>();
        tableE = new HashMap<>();
        tableF = new HashMap<>();

        // Configure Op Table
        table.put(0x0, this::Table0);
        table.put(0x1, this::OP_1NNN);
        table.put(0x2, this::OP_2NNN);
        table.put(0x3, this::OP_3XKK);
        table.put(0x4, this::OP_4XKK);
        table.put(0x5, this::OP_5XY0);
        table.put(0x6, this::OP_6XKK);
        table.put(0x7, this::OP_7XKK);
        table.put(0x8, this::Table8);
        table.put(0x9, this::OP_9XY0);
        table.put(0xA, this::OP_ANNN);
        table.put(0xB, this::OP_BNNN);
        table.put(0xC, this::OP_CXKK);
        table.put(0xD, this::OP_DXYN);
        table.put(0xE, this::TableE);
        table.put(0xF, this::TableF);

        // Initialize secondary tables with OP_NULL
        for (int i = 0; i <= 0xE; i++) {
            table0.put(i, this::OP_NULL);
            table8.put(i, this::OP_NULL);
            tableE.put(i, this::OP_NULL);
        }

        // Table0 configuration
        table0.put(0x0, this::OP_00E0);
        table0.put(0xE, this::OP_00EE);

        // Table8 configuration
        table8.put(0x0, this::OP_8XY0);
        table8.put(0x1, this::OP_8XY1);
        table8.put(0x2, this::OP_8XY2);
        table8.put(0x3, this::OP_8XY3);
        table8.put(0x4, this::OP_8XY4);
        table8.put(0x5, this::OP_8XY5);
        table8.put(0x6, this::OP_8XY6);
        table8.put(0x7, this::OP_8XY7);
        table8.put(0xE, this::OP_8XYE);

        // TableE configuration
        tableE.put(0x1, this::OP_EXA1);
        tableE.put(0xE, this::OP_EX9E);

        // Initialize tableF
        for (int i = 0; i <= 0x65; i++) {
            tableF.put(i, this::OP_NULL);
        }

        // TableF configuration
        tableF.put(0x07, this::OP_FX07);
        tableF.put(0x0A, this::OP_FX0A);
        tableF.put(0x15, this::OP_FX15);
        tableF.put(0x18, this::OP_FX18);
        tableF.put(0x1E, this::OP_FX1E);
        tableF.put(0x29, this::OP_FX29);
        tableF.put(0x33, this::OP_FX33);
        tableF.put(0x55, this::OP_FX55);
        tableF.put(0x65, this::OP_FX65);
    }

    public CPU() {
        reset();
    }

    private int randByte() {
        return random.nextInt(256);
    }

    // Table Methods
    private void Table0() {
        int index = opcode & 0x000F;
        table0.getOrDefault(index, this::OP_NULL).run();
    }

    private void Table8() {
        int index = opcode & 0x000F;
        table8.getOrDefault(index, this::OP_NULL).run();
    }

    private void TableE() {
        int index = opcode & 0x000F;
        tableE.getOrDefault(index, this::OP_NULL).run();
    }

    private void TableF() {
        int index = opcode & 0x00FF;
        tableF.getOrDefault(index, this::OP_NULL).run();
    }

    // Null Method
    private void OP_NULL() {
        // Does nothing
        //System.out.println("NULL command called");
    }

    // Start Operations
    private void OP_00E0() { // CLS video matrix with zeroes
        Arrays.fill(video, false);
        //System.out.println("CLS command called");
    }

    private void OP_00EE() { // RET Return from a subroutine
        this.sp--;
        this.pc = this.stack[sp];
        //System.out.println("RET command called");
    }

    private void OP_1NNN() { // JP Jump to location NNN
        this.pc = (short)(this.opcode & 0x0FFF);
        //System.out.println("JP command called");
    }

    private void OP_2NNN() { // CALL Call subroutine at NNN
        this.stack[this.sp++] = this.pc;
        this.pc = (short)(this.opcode & 0x0FFF);
        //System.out.println("CALL command called");
    }

    private void OP_3XKK() { // SE Skip next instruction if Vx == kk
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Byte = this.opcode & 0x00FF;

        if ((this.registers[Vx] & 0xFF) == Byte){
            this.pc += 2;
        }
        //System.out.println("SE Vx == kk command called");
    }

    private void OP_4XKK() { // SNE Skip next instruction if Vx != kk
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Byte = this.opcode & 0x00FF;

        if ((this.registers[Vx] & 0xFF) != Byte){
            this.pc += 2;
        }
        //System.out.println("SNE Vx != kk command called");
    }

    private void OP_5XY0() { // SE Skip next instruction if Vx = Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        if ((this.registers[Vx] & 0xFF) == (this.registers[Vy] & 0xFF)) {
            this.pc += 2;
        }
        //System.out.println("SE Vx == Vy command called");
    }

    private void OP_6XKK() { // LD Set Vx = kk
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Byte = this.opcode & 0x00FF;

        this.registers[Vx] = (short)(Byte & 0xFF);
        //System.out.println("LD Vx = kk command called");
    }

    private void OP_7XKK() { // ADD Set Vx = Vx + kk
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Byte = this.opcode & 0x00FF;

        this.registers[Vx] += (short)(Byte & 0xFF);
        //System.out.println("ADD Vx = Vx + kk command called");
    }

    private void OP_8XY0() { // LD Set Vx = Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        this.registers[Vx] = this.registers[Vy];
        //System.out.println("LD Vx = Vy command called");
    }

    private void OP_8XY1() { // OR Set Vx = Vx OR Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        this.registers[Vx] |= this.registers[Vy];
        //System.out.println("OR Vx = Vx OR Vy command called");
    }

    private void OP_8XY2() { // AND Set Vx = Vx AND Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        this.registers[Vx] &= this.registers[Vy];
        //System.out.println("AND Vx = Vx AND Vy command called");
    }

    private void OP_8XY3() { // XOR Set Vx = Vx XOR Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        this.registers[Vx] ^= this.registers[Vy];
        //System.out.println("XOR Vx = Vx XOR Vy command called");
    }

    private void OP_8XY4() { // ADD Set Vx = Vx + Vy, set VF = carry
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        int sum = (this.registers[Vx] & 0xFF) + (this.registers[Vy] & 0xFF);

        this.registers[0xF] = (short)((sum > 0xFF) ? 1 : 0);
        this.registers[Vx] = (short)(sum & 0xFF);
        //System.out.println("ADD Vx = Vx + Vy SET VF command called");
    }

    private void OP_8XY5() { // SUB Set Vx = Vx - Vy, set VF = NOT borrow
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        this.registers[0xF] = (short)((this.registers[Vx] > this.registers[Vy]) ? 1 : 0);
        this.registers[Vx] -= this.registers[Vy];
        //System.out.println("SUB Vx = Vx - Vy SET VF command called");
    }

    private void OP_8XY6() { // SHR Set Vx = Vx SHR 1, If the least-significant bit of Vx is 1, then VF is set to 1, otherwise 0. Then Vx is divided by 2
        int Vx = (this.opcode & 0x0F00) >> 8;
        int vxValue = this.registers[Vx] & 0xFF;

        this.registers[0xF] = (short)(vxValue & 1);

        vxValue = (vxValue >> 1) & 0xFF;

        this.registers[Vx] = (short)vxValue;
        //System.out.println("SHR Vx = Vx SHR 1 command called");
    }

    private void OP_8XY7() { // SUBN Set Vx = Vy - Vx SHR 1, set VF = NOT borrow
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        int result = (this.registers[Vy] & 0xFF) - (this.registers[Vx] & 0xFF);

        this.registers[0xF] = (short)((this.registers[Vy] > this.registers[Vx]) ? 1 : 0);
        this.registers[Vx] = (short)(result & 0xFF);
        //System.out.println("SUBN Vx = Vy - Vx SHR 1 command called");
    }

    private void OP_8XYE() { // SHL Set Vx = Vx SHL 1, If the most-significant bit of Vx is 1, then VF is set to 1, otherwise to 0. Then Vx is multiplied by 2.
        int Vx = (this.opcode & 0x0F00) >> 8;
        int vxValue = this.registers[Vx] & 0xFF;

        this.registers[0xF] = (short)((vxValue >> 7) & 1);

        vxValue = (vxValue << 1) & 0xFF;

        this.registers[Vx] = (short)vxValue;
        //System.out.println("SHL Vx = Vx SHL 1 command called");
    }

    private void OP_9XY0() { // SNE, Skip next instruction if Vx != Vy
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;

        if ((this.registers[Vx] & 0xFF) != (this.registers[Vy] & 0xFF)) {
            this.pc += 2;
        }
        //System.out.println("SNE Vx != Vy command called");
    }

    private void OP_ANNN() { // LD, Set I = NNN
        int address = this.opcode & 0x0FFF;

        this.index = (short)address;
        //System.out.println("LD I = NNN command called");
    }

    private void OP_BNNN() { // JP, Jump to location NNN + V0
        int address = this.opcode & 0x0FFF;

        int v0value = this.registers[0] & 0xFF;

        this.pc = (short)(address + v0value);
        //System.out.println("JP NNN + V0 command called");
    }

    private void OP_CXKK() { // RND, Set Vx = random byte AND KK.
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Byte = this.opcode & 0x00FF;

        this.registers[Vx] = (short)((randByte() & Byte) & 0xFF);
        //System.out.println("RND Vx = random AND kk command called");
    }

    private void OP_DXYN() { // DRW, Display n-byte sprite starting at memory location I at (Vx, Vy), set VF = collision
        int Vx = (this.opcode & 0x0F00) >> 8;
        int Vy = (this.opcode & 0x00F0) >> 4;
        int height = this.opcode & 0x000F;

        int xPos = this.registers[Vx] & 0xFF;
        int yPos = this.registers[Vy] & 0xFF;

        xPos = xPos % 64;
        yPos = yPos % 32;

        this.registers[0xF] = 0;

        for (int row = 0; row < height; row++) {
            short spriteByte = this.memory[this.index + row];

            for (int col = 0; col < 8; col++) {
                int spritePixel = (spriteByte >> (7 - col)) & 1;

                if (spritePixel == 1) {
                    int screenX = (xPos + col) % 64;
                    int screenY = (yPos + row) % 32;
                    int pixelIndex = screenY * 64 + screenX;

                    if (this.video[pixelIndex]) {
                        this.registers[0xF] = 0x1;
                    }

                    this.video[pixelIndex] ^= true;
                }
            }
        }
        //System.out.println("DRW command called");
    }

    private void OP_EX9E() { // SKP, Skip next instruction if key with the value of Vx is pressed.
        int Vx = (this.opcode & 0x0F00) >> 8;

        int keyIndex = this.registers[Vx] & 0x0F;

        if(keyIndex < this.keyboard.length && this.keyboard[keyIndex]) {
            this.pc += 2;
        }
        //System.out.println("SKP key[Vx] = 1 command called");
    }

    private void OP_EXA1() { // SKNP, Skip next instruction if key with the value of Vx is not pressed.
        int Vx = (this.opcode & 0x0F00) >> 8;

        int keyIndex = this.registers[Vx] & 0x0F;

        if(keyIndex < this.keyboard.length && !this.keyboard[keyIndex]) {
            this.pc += 2;
        }
        //System.out.println("SKP key[Vx] = 0 command called");
    }

    private void OP_FX07() { // LD, Set Vx = delay timer value.
        int Vx = (this.opcode & 0x0F00) >> 8;

        this.registers[Vx] = (byte)(this.delayTimer & 0xFF);
        //System.out.println("LD Vx = delay command called");
    }

    private void OP_FX0A() { // LD, Wait for a key press, store the value of the key in Vx.
        int Vx = (this.opcode & 0x0F00) >> 8;
        boolean keyPressed = false;

        for (int i = 0; i < this.keyboard.length; i++) {
            if (this.keyboard[i]) {
                this.registers[Vx] = (short) i;
                keyPressed = true;
                break;
            }
        }

        if (!keyPressed) {
            this.pc -= 2;
        }
        //System.out.println("LD Vx = key command called");
    }

    private void OP_FX15() { // LD, Set DelayTimer = Vx
        int Vx = (this.opcode & 0x0F00) >> 8;

        this.delayTimer = this.registers[Vx];
        //System.out.println("LD delay = Vx command called");
    }

    private void OP_FX18() { // LD, Set SoundTimer = Vx
        int Vx = (this.opcode & 0x0F00) >> 8;

        this.soundTimer = this.registers[Vx];
        //System.out.println("LD sound = Vx command called");
    }

    private void OP_FX1E() { // ADD, Set I = I + Vx;
        int Vx = (this.opcode & 0x0F00) >> 8;

        this.index += this.registers[Vx];
        //System.out.println("ADD I = I + Vx = 0 command called");
    }

    private void OP_FX29() { // LD, Set I = location of sprite for digit Vx.
        int Vx = (this.opcode & 0x0F00) >> 8;

        int digit = this.registers[Vx] & 0x0F;

        this.index = (short)(FONTSET_START_ADDRESS + (5 * digit));
        //System.out.println("LD I = digit[vx] command called");
    }

    private void OP_FX33() { // LD, Store BCD representation of Vx in memory locations I, I+1, and I+2
        int Vx = (this.opcode & 0x0F00) >> 8;
        int value = (this.registers[Vx] & 0xFFFF) & 0xFF;

        int hundreds = value / 100;
        int tens = (value % 100) / 10;
        int units = value % 10;

        this.memory[this.index] = (short)hundreds;
        this.memory[this.index + 1] = (short)tens;
        this.memory[this.index + 2] = (short)units;
        //System.out.println("LD Vx BCD command called");
    }

    private void OP_FX55() { // LD, Store registers V0 through Vx in memory starting at location I.
        int Vx = (this.opcode & 0x0F00) >> 8;

        for (int i = 0; i <= Vx; i++) {
            this.memory[this.index + i] = (short)(this.registers[i] & 0xFF);
        }

        //System.out.println("LD V0 - Vx -> memory[I] command called");
    }

    private void OP_FX65() { // LD, Read registers V0 through Vx from memory starting at location I.
        int Vx = (this.opcode & 0x0F00) >> 8;

        for (int i = 0; i <= Vx; i++) {
            this.registers[i] = this.memory[this.index + i];
        }
        //System.out.println("LD memory[I] -> V0 - Vx command called");
    }

    // End Operations

    public void cycle() {
        this.opcode = (short)((this.memory[this.pc] << 8) | (this.memory[this.pc + 1] & 0xFF));

        this.pc += 2;

        int tableIndex = (this.opcode & 0xF000) >> 12;

        Runnable operation = table.get(tableIndex);
        if (operation != null) {
            operation.run();
        } else {
            this.OP_NULL();
        }

        if (this.delayTimer > 0) {
            --this.delayTimer;
        }

        if (this.soundTimer > 0) {
            --this.soundTimer;
        }

        dumpState();
    }

    public void loadRom(String fileName) {
        reset();
        try {
            Path path = Paths.get(fileName);
            byte[] romData = Files.readAllBytes(path);

            if (romData.length > this.memory.length - START_ADDRESS) {
                throw new IllegalArgumentException("ROM too large: "+romData.length + " bytes");
            }

            for (int i=0; i<romData.length; i++) {
                this.memory[START_ADDRESS+i] = (short)(romData[i] & 0xFF);
            }

            this.romLoaded = true;

            System.out.println("ROM cargado: "+romData.length+" bytes");

            dumpState();
        } catch(IOException e) {
            System.err.println("Error al cargar el ROM: "+e.getMessage());
        } catch(IllegalArgumentException e) {
            System.err.println("Error: "+e.getMessage());
        }
    }

    public boolean isRomLoaded() {
        return romLoaded;
    }

    public void keyPressed(int keyCode) {
        this.keyboard[keyCode] = true;
    }

    public void keyReleased(int keyCode) {
        this.keyboard[keyCode] = false;
    }

    public boolean[][] getVideoMatrix() {
        boolean[][] matrix = new boolean[64][32];
        for(int y = 0; y < 32; y++) {
            for(int x = 0; x < 64; x++) {
                matrix[x][y] = this.video[y * 64 + x];
            }
        }
        return matrix;
    }

    public void dumpState() {
        System.out.println("=== CPU State ===");
        System.out.printf("PC: 0x%04X, I: 0x%04X, SP: %d%n",
                this.pc, this.index, this.sp);
        System.out.print("Registers: ");
        for(int i = 0; i < 16; i++) {
            System.out.printf("V%X:0x%02X ", i, this.registers[i] & 0xFF);
        }
        System.out.println();

        // Verifica las primeras instrucciones del ROM
        System.out.print("ROM (primeras 10 bytes): ");
        for(int i = START_ADDRESS; i < START_ADDRESS + 10; i++) {
            System.out.printf("%02X ", this.memory[i] & 0xFF);
        }
        System.out.println();
    }
}
