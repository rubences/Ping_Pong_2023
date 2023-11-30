public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = Player.create("ping", Player.create("pong", null));

        System.out.println("Game starting...!");

        player1.play();

        System.out.println("Game finished!");
    }
}