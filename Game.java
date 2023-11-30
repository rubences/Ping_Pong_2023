import java.lang.Thread;

public class Game {
  
  public static final int MAX_TURNS = 10;
  
  public static void main(String[] args) {
    Thread player1 = new Thread(new PlayerRunnable("ping"));
    Thread player2 = new Thread(new PlayerRunnable("pong", player1));
    player1.start();
    player2.start();
  }
}

class PlayerRunnable implements Runnable {
  private String message;
  private Thread nextPlayer;
  
  public PlayerRunnable(String message) {
    this.message = message;
  }
  
  public PlayerRunnable(String message, Thread nextPlayer) {
    this.message = message;
    this.nextPlayer = nextPlayer;
  }
  
  public void setNextPlayer(Thread nextPlayer) {
    this.nextPlayer = nextPlayer;
  }
  
  public void run() {
    for (int i = 0; i < Game.MAX_TURNS; i++) {
      System.out.println(message);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
      if (nextPlayer != null) {
        synchronized (nextPlayer) {
          nextPlayer.notify();
        }
      }
      synchronized (this) {
        try {
          wait();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }
  }
}