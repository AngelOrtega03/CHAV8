package component;

import javax.swing.*;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class KeyMapButton extends JTextField {
    private int index;
    private char assignedKey;
    private boolean awaitingKeyPress = false;
    private String originalText = "";

    public KeyMapButton() { this(0); };

    public KeyMapButton(int index) {
        this("0",index);
    }

    public KeyMapButton(String value, int index) {
        super();
        this.setIndex(index);
        this.assignedKey = ' ';
        setHorizontalAlignment(JTextField.CENTER);
        setText(value.toUpperCase());
        setEditable(false);
        setPreferredSize(new Dimension(40, 26));

        // Manejar el foco para seleccionar todo el texto
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (!awaitingKeyPress) {
                    selectAll();
                }
            }
        });

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (awaitingKeyPress) {
                    int keyCode = e.getKeyCode();

                    // Bloquear teclas especiales excepto ESC
                    if (keyCode == KeyEvent.VK_ESCAPE) {
                        cancelKeyAssignment();
                        return;
                    }

                    // Obtener el caracter correspondiente a la tecla presionada
                    char keyChar = e.getKeyChar();

                    // Verificar que sea un caracter válido (letra o número)
                    if (Character.isLetterOrDigit(keyChar) || (keyChar >= 32 && keyChar <= 126)) { // Caracteres imprimibles
                        assignedKey = Character.toUpperCase(keyChar);
                        setText(String.valueOf(assignedKey));
                        awaitingKeyPress = false;
                        setEditable(false);

                        // Disparar un evento personalizado si es necesario
                        fireKeyAssigned();
                    }
                } else {
                    // Si no está esperando tecla, no permitir edición
                    e.consume();
                }
            }
        });
    }

    //Private methods
    /**
     * Activa el modo de espera para asignar una nueva tecla
     */
    public void startKeyAssignment() {
        if (!awaitingKeyPress) {
            originalText = getText();
            awaitingKeyPress = true;
            setEditable(true);
            setText("?"); // Indicador visual
            setCaretPosition(0);
        }
    }

    /**
     * Cancela la asignación de tecla
     */
    private void cancelKeyAssignment() {
        awaitingKeyPress = false;
        setEditable(false);
        setText(originalText);
    }

    /**
     * Obtiene la tecla asignada
     */
    public char getAssignedKey() {
        return assignedKey;
    }

    /**
     * Establece la tecla asignada
     */
    public void setAssignedKey(char key) {
        this.assignedKey = Character.toUpperCase(key);
        setText(String.valueOf(this.assignedKey));
        awaitingKeyPress = false;
        setEditable(false);
    }

    /**
     * Verifica si está esperando una tecla
     */
    public boolean isAwaitingKey() {
        return awaitingKeyPress;
    }

    /**
     * Interfaz para notificar cuando se asigna una tecla
     */
    public interface KeyAssignedListener {
        void onKeyAssigned(KeyMapButton source, char key, int index);
    }

    private KeyAssignedListener listener;

    public void addKeyAssignedListener(KeyAssignedListener listener) {
        this.listener = listener;
    }

    private void fireKeyAssigned() {
        if (listener != null) {
            listener.onKeyAssigned(this, assignedKey, index);
        }
    }

    //Public methods

    public void setIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return this.index;
    }
}
