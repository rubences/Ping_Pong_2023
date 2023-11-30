import javax.swing.*;

public class Player implements Runnable {
    private String text;
    private int turns;
    private boolean mustPlay;
    private Player nextPlayer;
    private JTextArea textArea;

    public Player(String text, int turns, JTextArea textArea) {
        this.text = text;
        this.turns = turns;
        this.textArea = textArea;
    }

    @Override
    public void run() {
        while(!gameFinished()) {
            while (!mustPlay);

            textArea.append(text + "\n");
            turns--;

            this.mustPlay = false;
            nextPlayer.mustPlay = true;
        }
    }
    // Rest of the code...
    private boolean gameFinished() {
        return turns == 0;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public void setMustPlay(boolean mustPlay) {
        this.mustPlay = mustPlay;
    }

  
}
