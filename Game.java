import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class Game {

    public static void main(String[] args) {
        CountDownLatch entryBarrier = new CountDownLatch(1);
        CountDownLatch exitBarrier = new CountDownLatch(1);

        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock, entryBarrier, exitBarrier);
        Player player2 = new Player("pong", lock, entryBarrier, exitBarrier);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(player1);

        sleep(1000);

        executor.execute(player2);

        entryBarrier.countDown();

        executor.shutdown();


        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
            exitBarrier.countDown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

    public static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

// Path: Player.java