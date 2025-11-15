import lzw.LZWGui;

public class Program {
    public static void main(String[] args) {
        javax.swing.SwingUtilities.invokeLater(() -> new LZWGui().setVisible(true));
    }
}
