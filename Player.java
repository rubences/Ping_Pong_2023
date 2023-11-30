public class Player {

    private final String text;

    private int turns = Game.MAX_TURNS;

    private Player nextPlayer;

   public static Player create(String text, Player nextPlayer) {
        Player player = new Player(text);
        player.setNextPlayer(nextPlayer);
        return player;
    }

    public void play() {
        if (!gameFinished()) {
            System.out.println(text);
            turns--;
            nextPlayer.play();
        }
    }

    private boolean gameFinished() {
        return turns == 0;
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

}
