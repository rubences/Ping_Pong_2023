# Ping_Pong_2023

Versión cero: un solo hilo
La primera versión correrá en un solo hilo de ejecución, por lo que no habrá programación concurrente que valga :). En estas primeras versiones, además, el juego finalizará después de que ambos jugadores hayan participado un número de veces (definido por constante), pongamos 10.

Veamos como quedaría el código de la clase Player:

public class Player {

    private final String text;

    private int turns = Game.MAX_TURNS;

    private Player nextPlayer;

    public Player(String text) {
        this.text = text;
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
Cada jugador imprime su texto y le dice al otro que juegue, por lo que se van alternando. La clase que inicia el juego sería bastante sencilla también:

public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = new Player("ping");
        Player player2 = new Player("pong");

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.play();

        System.out.println("Game finished!");
    }
}
Poca historia. Vemos que en el constructor de Player no es posible pasar quien es el otro jugador porque no siempre va a estar instanciado, por lo que hay que hacerlo mediante setter.

También vemos como en Player el atributo text es declarado final. Es buena práctica en aplicaciones concurrentes (y en todas, la verdad) declarar un atributo como final si sabemos que no va a ser modificado. No sólo hace más fiable a nuestro código, también garantiza la visibilidad de las variables entre threads, un concepto conocido como “Safe publication”, y del que podéis leer una discusión aquí. Yendo un poco más allá, siempre que podamos deberíamos diseñar nuestras clases como inmutables, aunque en nuestro ejemplo no es posible.