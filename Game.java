import javax.swing.*;
import java.awt.*;

public class Game extends JFrame {
  private JTextArea textArea;
  private Player player1;
  private Player player2;
  private Thread thread1;
  private Thread thread2;

  public Game() {
    setLayout(new BorderLayout());
    setTitle("Ping Pong Game");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setSize(500, 500);

    textArea = new JTextArea();
    add(new JScrollPane(textArea), BorderLayout.CENTER);

    player1 = new Player("ping", 0, textArea);
    player2 = new Player("pong", 0, textArea);

    player1.setNextPlayer(player2);
    player2.setNextPlayer(player1);

    JButton startButton = new JButton("Start Game");
    startButton.addActionListener(e -> startGame());

    JButton stopButton = new JButton("Stop Game");
    stopButton.addActionListener(e -> stopGame());

    JPanel panel = new JPanel();
    panel.add(startButton);
    panel.add(stopButton);
    add(panel, BorderLayout.SOUTH);
  }

  private void startGame() {
    textArea.append("Game starting...!\n");

    player1.setMustPlay(true);

    thread2 = new Thread(() -> player2.run());
    thread2.start();
    thread1 = new Thread(() -> player1.run());
    thread1.start();
  }



  private void stopGame() {
    thread1.interrupt();
    thread2.interrupt();
    textArea.append("Game finished!\n");
  }

  public static void main(String[] args) {
    SwingUtilities.invokeLater(() -> {
      new Game().setVisible(true);
    });
  }
}

// Path: Player.java