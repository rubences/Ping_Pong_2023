public class PlayerThead {
    
        private final String text;
        private int turns = Game.MAX_TURNS;
        private PlayerThead nextPlayer;
    
        public PlayerThead(String text) {
            this.text = text;
        }
    
        public static PlayerThead create(String text, PlayerThead nextPlayer) {
            PlayerThead player = new PlayerThead(text);
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
    
        public void setNextPlayer(PlayerThead nextPlayer) {
            this.nextPlayer = nextPlayer;
        }
}
