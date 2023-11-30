# Ping_Pong_2023

## Versión cero: un solo hilo
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

# Version 0.1: un solo hilo, pero con un poco de azúcar

Vamos a hacer un pequeño cambio en la clase Player para que sea más fácil de usar. En lugar de tener que instanciar un jugador y luego indicarle quién es el siguiente, vamos a crear un método estático que nos devuelva un jugador ya configurado:

public class Player {

    //...

    public static Player create(String text, Player nextPlayer) {
        Player player = new Player(text);
        player.setNextPlayer(nextPlayer);
        return player;
    }

    //...
}

Y ahora nuestro código de Game quedaría así:

public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = Player.create("ping", Player.create("pong", null));

        System.out.println("Game starting...!");

        player1.play();

        System.out.println("Game finished!");
    }
}

## Versión 0.2: con dos hilos
Vamos a hacer que nuestro juego sea concurrente. Para ello vamos a crear un thread para cada jugador, y vamos a arrancarlos de forma separada. Para ello vamos a crear una clase PlayerThread que extienda de Thread:

public class PlayerThread extends Thread {

    private final Player player;

    public PlayerThread(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        player.play();
    }
}

Y ahora nuestro código de Game quedaría así:

public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = Player.create("ping", Player.create("pong", null));

        System.out.println("Game starting...!");

        Thread thread1 = new PlayerThread(player1);
        thread1.start();

        System.out.println("Game finished!");
    }
}

Si ejecutamos la aplicación veremos que no funciona como esperábamos. El mensaje “Game finished!” se imprime antes que los mensajes de los jugadores. ¿Qué ha pasado? Pues que el thread principal (el que ejecuta el método main) no espera a que los otros threads finalicen su ejecución, por lo que la salida de los mismos no es recogida por el IDE. Para solucionarlo podemos utilizar el método join() de la clase Thread:


## Versión 1: jugadores como threads
Vamos a llevar nuestra aplicación un poco más allá para que funcione en modo concurrente. Cómo la intención es hacerlo en incrementos pequeños nos iremos encontrando que nuestros primeros acercamientos no funcionan como es debido.

En primer lugar crearemos nuestra clase Player como Runnable (más información aquí):

public class Player implements Runnable {

    private final String text;

    private int turns = Game.MAX_TURNS;

    private Player nextPlayer;

    private boolean mustPlay = false;

    public Player(String text) {
        this.text = text;
    }

    @Override
    public void run() {
        while(!gameFinished()) {
            while (!mustPlay);

            System.out.println(text);
            turns--;

            this.mustPlay = false;
            nextPlayer.mustPlay = true;

        }
    }

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
El método principal (run) estará formado por un bucle que itera hasta que termina el número de turnos para el jugador. Además, en cada iteración del bucle se produce una espera activa hasta que le llegue el turno. Una vez le toca imprime el texto, se indica a sí mismo que no le toca jugar hasta que alguien indique lo contrario, y le dice al siguiente jugador que es su turno.

Una gran diferencia entre esta clase Player y la anterior es que en la primera versión un jugador le indicaba al otro que jugara mediante paso de mensajes (invocando al método play), mientras que aquí se realiza modificando el valor de un flag (mustPlay), que cada jugador de forma individual es responsable de verificar.

Veamos cómo quedaría la clase Game:

public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = new Player("ping");
        Player player2 = new Player("pong");

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setMustPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        System.out.println("Game finished!");
    }

}
La gran diferencia es que los threads se inician de forma separada, y nosotros solo somos responsables de configurar el flag mustPlay de forma adecuada. De hecho, he arrancado primero el thread de player2 a propósito para confirmar que incluso así se imprime primero el mensaje “ping”.

Veamos qué pasa al iniciar la aplicación:

Game starting...!
ping
Game finished!
¿Qué ha ocurrido? Nuestra aplicación tiene ahora tres hilos:

Hilo principal (Game.main)
Hilo player1
Hilo player2
El problema es que el hilo principal finaliza tan pronto como inicia los threads, por lo que aunque los otros dos hilos continúan su ejecución y finalizan correctamente nuestro IDE no recoge la salida de esos threads adicionales, creando además doble confusión al imprimir el mensaje “Game finished!”. Para evitar esto una forma bastante directa es utilizar el método join():

public class Game {

    public static final int MAX_TURNS = 10;

    public static void main(String[] args) {

        Player player1 = new Player("ping");
        Player player2 = new Player("pong");

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setMustPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

}
Este método condiciona el progreso del hilo en ejecución a la finalización del hilo sobre el que se invoca el método join. Al ser join un método bloqueante es susceptible de ser interrumpido, por lo que lanza la checked exception InterruptedException. Más adelante hablaremos de las complicaciones que existen para interrumpir un thread.

Por tanto, ahora nuestro thread principal esperará hasta que los jugadores agoten sus turnos…o no? Bien, si ejecutamos la aplicación un par de veces, dependiendo de la suerte que tengamos es posible que todo sea correcto, pero si lanzamos ejecuciones reiteradas es más que posible que obtengamos una salida como la que sigue:

Game starting...!
ping
¡Nuestra aplicación se queda bloqueada y no progresa en absoluto!

¿Qué ha pasado? Uno de los grandes problemas en la programación concurrente es la “visibilidad”. Java solo garantiza la visiblidad de atributos compartidos entre threads si seguimos una serie de directrices, que vienen reguladas por el Java Memory Model, y en concreto por la relación “happens-before”. Según la Wikipedia, en Java esta relación viene a decir que:

In Java specifically, a happens-before relationship is a guarantee that memory written to by statement A is visible to statement B, that is, that statement A completes its write before statement B starts its read

En nuestro código no estamos siguiendo ninguna convención que nos asegure la visibilidad de la modificación del atributo mustPlay entre threads. Evidentemente, la modificación de su propio mustPlay es visible para el propio thread, que no continúa jugando, pero de la forma en que lo estamos haciendo la modificación del atributo mustPlay del otro thread no se hace visible para el thread interesado, y nuestro programa queda en situación de bloqueo (o deadlock).

Para corregir este problema vamos a introducir el modificador volatile. Este modificador indica a la JVM que el atributo es susceptible de ser compartido entre threads, y que por tanto sus lecturas no deben ser cacheadas en modo alguno, accediendo siempre a memoria principal, y además sus escrituras deben hacerse de forma atómica y hacerse visibles de manera inmediata.

Nuestro código quedaría así:

public class Player implements Runnable {
    //...
    private volatile boolean mustPlay = false;
    //....
}
Y ahora sí, nuestra aplicación funciona de forma determinista en cada ejecución. Uno de los mayores problemas de la visibilidad en aplicaciones concurrentes es que falla aleatoriamente, por lo que si no somos conscientes de las directrices a seguir, depurar estos problemas puede ser extremadamente complicado.