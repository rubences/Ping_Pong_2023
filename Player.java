import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Player implements Runnable {

        private final String text;

        private final ReentrantLock lock;

        private Player nextPlayer;

        private volatile boolean play = false;

        public Player(String text,
                      Lock lock) {
            this.text = text;
            this.lock = (ReentrantLock) lock;
        }

        @Override
        public void run() {
            while(!Thread.interrupted()) {
                lock.lock();
                try {
                    while (!play)
                        lock.newCondition().await();

                    System.out.println(text);

                    this.play = false;
                    nextPlayer.play = true;

                    lock.unlock();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                            }
        }
    }

        public void setNextPlayer(Player nextPlayer) {
            this.nextPlayer = nextPlayer;
     }

        public void setPlay(boolean play) {
            this.play = play;
     }
}
