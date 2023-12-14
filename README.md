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

## Versión 2: juego infinito
En lugar de jugar un número determinado de turnos vamos a poner a los dos actores a jugar para siempre. O mejor dicho, hasta que el hilo principal quiera. Para ello, debemos utilizar las funcionalidades que ofrece Java para interrumpir un thread. Veamos cómo quedaría la clase Game:

public class Game {

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

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

}
Vemos que, una vez iniciados los jugadores, la clase principal se duerme durante un tiempo (2ms), y a nada que regresa a su estado “running” les pide a los dos threads que finalicen.

Repito, les pide. Lo único que ocurre al invocar el método interrupt sobre un thread es que se pone a true un flag “interrupted” en ese thread. Es responsabilidad del propio thread actuar si lo desea, realizar labores de limpieza y finalizar. Pero bien puede decidir no hacer nada de nada y continuar con su ejecución (aunque eso no sería muy correcto, claro). La forma de consultar ese flag es mediante el método Thread.interrupted(), por lo que nuestra clase Player quedarían así:

public class Player implements Runnable {

    private final String text;

    private Player nextPlayer;

    private volatile boolean mustPlay = false;

    public Player(String text) {
        this.text = text;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            while (!mustPlay);

            System.out.println(text);

            this.mustPlay = false;
            nextPlayer.mustPlay = true;

        }
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
    }

    public void setMustPlay(boolean mustPlay) {
        this.mustPlay = mustPlay;
    }
}
En lugar de chequear en cada vuelta del bucle si hemos agotado turnos miramos el estado del flag “interrupted”, y concluimos en caso de que sea true. Tan sencillo como eso.

# Versión 2b: Más sobre interrupt
Antes de finalizar este primer post de la serie, vamos a mirar un poco más en profundidad las implicaciones de interrumpir un thread.

En varias ocasiones hemos visto como algunos de los métodos de la clase Thread (join, sleep…) lanzan la excepción InterruptedException. Esto ocurre cuando un thread es interrumpido encontrándose en situación de bloqueo debido a la invocación de alguno de estos métodos. En tal caso, lo que ocurre es que el método limpia el flag “interrupted” en el thread en cuestión, y lanza la excepción InterruptedException. Sin ser yo muy fan de las checked exceptions, este es uno de los casos en los que encuentro más justificado su uso.

Modifiquemos ligeramente la clase Player para que, una vez le llegue el turno a un jugador se duerma durante 1ms antes de imprimir el texto:

public class Player implements Runnable {
    //...
    @Override
    public void run() {
        while(!Thread.interrupted()) {
            while (!mustPlay);

            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            System.out.println(text);

            this.mustPlay = false;
            nextPlayer.mustPlay = true;

        }
    }
    //...
}
Utilizando la misma versión de la clase Game, es altamente probable que el juego se ejecute indefenidamente. ¿Por qué? Porque si la interrupción le llega al thread mientras está durmiendo, el método sleep se traga el estado “interrupted” antes de lanzar la excepción, y como nosotros sólo nos hemos limitado a imprimir el error, el bucle no detecta este estado interrupted y continúa para siempre.

La solución a esto es restablecer el Thread a interrupted:

@Override
public void run() {
    while(!Thread.interrupted()) {
        while (!mustPlay);

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println(text);

        this.mustPlay = false;
        nextPlayer.mustPlay = true;

    }
}
En general, hemos de ser muy cuidadosos a la hora de manejar InterruptedException. Otra estrategia recomendada, que implicaría modificar la lógica de nuestro método run, es volver a lanzar la excepción para que sea manejada en algún otro lugar. En ningún caso nunca debemos tragarnos la excepción sin más.

## Espera activa
Esta instrucción es un ejemplo de “espera activa” o “Busy Waiting”, y no es más que la comprobación infinita de una condición, evitando el progreso de la aplicación hasta que sea cierta. El problema de este enfoque es que nuestro hilo sobrecarga de forma excesiva a la CPU, ya que para el Thread Scheduler no hay nada que le impida progresar, por lo que siempre que existen recursos lo mantiene en su estado “Running” (aquí tenéis un buen diagrama de estados de los threads en Java). El resultado es un uso de recursos excesivo e injustificado.

Os voy a contar una historia curiosa para ilustrar esto que estoy explicando. Cuando desarrollé los ejemplos para la primera parte de este post, dejé mi IDE abierto con la aplicación funcionando (y la espera activa). El resultado es que mi batería, que normalmente dura una 6-8 horas se consumió en menos de dos. Pensemos en las consecuencias de un diseño tan defectuoso en aplicaciones corporativas serias.

## Locking
La forma más fácil de deshacernos de la espera activa es mediante el uso de Locks. En pocas palabras, locking es un mecanismo que permite establecer políticas de exclusión en aplicaciones concurrentes cuando existen instancias cuyo estado puede ser compartido y modificado por diferentes threads.

Este estado susceptible de ser modificado por más de un thread debe protegrese mediante el uso de una sección crítica (critical section). Java ofrece diferentes mecanismos parar implementar secciones críticas, y en este post veremos los más importantes.

## Versión 3: Intrinsic locking
El mecanismo más antiguo implementado en Java para la creación de secciones críticas es conocido como Intrinsic Locking, o Monitor Locking. A grandes rasgos, cada objeto creado en Java tiene asociado un lock (intrinsic lock o monitor lock) que puede ser utilizado con fines de exclusión en nuestros threads mediante el uso de la keyword synchronized:

//...
Object myObject = new Object();
//...
synchronized(myObject) {
    //critical section
}

En este ejemplo utilizamos una instancia de Object como lock, de forma que cada thread que desee acceder a la sección crítica debe obtener el lock, cosa que intenta hacer en la sentencia synchronized. Si el lock está disponible, el thread se lo queda y no estará disponible para ningún otro thread, que en caso de intentar obtenerlo fracasará y será puesto en estado “Blocked” por el Thread Scheduler.

Internet está plagado de ejemplos sobre el uso de synchronized, por lo que no entraré aquí sobre las mejores o peores prácticas. Solo añadir algunos puntos a considerar:

Es habitual sincronizar en this (synchronized(this)), con lo que la propia instancia se utiliza a sí misma como lock para proteger a sus clientes de problemas de concurrencia. No obstante, hay que ser muy cuidadosos si hacemos esto porque los clientes podrían sincronizar en la misma instancia resultando en un DeadLock
Personalmente considero mejor práctica utilizar un lock privado (como el utilizado en el fragmento de código tres párrafos arriba), de forma que no exponemos el mecanismo de locking utilizado al exterior encapsulándolo en la propia clase
synchronized tiene otro fin además de la exclusión, y es la visibilidad. De la misma forma que la keyword volatile nos garantiza la visibilidad inmediata de la variable modificada, synchronized garantiza la visibilidad del estado del objeto utilizado como lock (abarcando más ámbito, pues). Esta visibilidad está garantizada por el Java Memory Model, del que hablaremos algún día.
Mecanismos de espera
Tan solo con mecanismos de locking no podemos implementar correctamente la eliminación de la espera activa en nuestra aplicación. Necesitamos algo más, y son los mecanismos de espera.

Cada objeto expone un método, wait(), que al invocarse por un thread hace que el Thread Scheduler lo suspenda, quedando en estado “Waiting”. Es decir:

//thread state is running
i++
lock.wait(); // => thread state changes to Waiting
Este ejemplo está algo cogido con pinzas, porque nunca debe invocarse wait de esta forma. El “idiom” adecuado a la hora de implementar mecanismos de espera es:

synchronized (lock) {
    try {
        while (!condition)
            lock.wait();

        //Excecute code after waiting for condition

    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    }
}
En el código vemos como:

Es necesario adquirir el lock sobre el objecto en el que queremos invocar wait
Ese wait implica que esperamos “algo”. Ese algo es una condición (condition predicate) que puede que se cumpla antes de tener que esperar. Por tanto preguntamos por esa condición antes de invocar a wait
La espera se realiza en un bucle y no en una sentencia if por varios motivos, pero el más importante de ellos es el conocido como “spurious wakeups”. Por su nombre es fácil de deducir en qué consiste, en ocasiones un thread se despierta del estado “Waiting” sin que nadie se lo haya indicado, por lo que puede que la condición no se esté cumpliendo y deba volver a esperar.
Por último, wait lanza la excepción InterruptedException, que manejamos de la forma comentada en la primera parte de esta serie
Visto esto, tenemos que un thread pasa a estado “Waiting” a la espera de una condición, pero alguien deberá indicar que uno o varios threads en espera deben despertarse, ¿no? Bien, esto se lleva a cabo mediante los métodos notify y notifyAll, que como es fácil de deducir, indican a uno o a todos los threads esperando sobre un lock que se despierten y comprueben la condición. El idiom es:

synchronized(lock) {
    //....
    condition = true;
    lock.notifyAll(); //or lock.notify();
}
De nuevo debemos tener el lock en nuestra posesión para poder invocar los métodos sobre el objeto. Sobre el uso de notify vs notifyAll se ha escrito mucho al respecto, y depende de cada aplicación en concreto. Precisamente el uso de notifyAll es otro de los motivos por los que la espera de la condición se hace en un bucle y no en una condición, en ocasiones solo un thread de todos los que estén en espera puede progresar tras cumplirse el predicado.

Por fin ha llegado el momento de ver cómo quedaría nuestro juego de Ping Pong tras aplicar los conceptos que acabamos de ver:

public class Player implements Runnable {

    private final String text;

    private final Object lock;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Object lock) {
        this.text = text;
        this.lock = lock;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            synchronized (lock) {
                try {
                    while (!play)
                        lock.wait();

                    System.out.println(text);

                    this.play = false;
                    nextPlayer.play = true;

                    lock.notifyAll();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
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
El lock en esta aplicación vendría a ser la pelota en juego, que en cada jugada solo puede estar en posesión de un jugador. También vemos que tras imprimir el texto por salida estándar notifica al otro jugador que puede continuar. He utilizado notifyAll, aunque podría ser notify sin problemas.

La clase que conduce el juego no varía mucho sobre la última versión de la primera parte de esta serie:

public class Game {

    public static void main(String[] args) {

        Object lock = new Object();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

}

## Versión 3.b: Locks explícitos

La versión anterior de nuestro juego de Ping Pong funciona correctamente, pero tiene un problema. El lock utilizado es el mismo para todos los jugadores, por lo que si en un futuro quisiéramos añadir más jugadores al juego, o incluso crear varios juegos concurrentes, tendríamos que crear un lock por cada jugador o juego, y pasarle el lock correspondiente a cada jugador. Esto es un poco engorroso, y además no es muy elegante.

Para solucionar esto, Java ofrece la interfaz Lock, que nos permite crear locks explícitos, y que además nos ofrece funcionalidades adicionales. Veamos cómo quedaría nuestro código:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock) {
        this.text = text;
        this.lock = lock;
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            lock.lock();
            try {
                while (!play)
                    lock.await();

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                lock.signalAll();

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

Y la clase Game:

public class Game {

    public static void main(String[] args) {

        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

}

## Versión 4. Locks explícitos y condiciones
Java expone en su API concurrency una interfaz, Lock, que permite implementar los mismos mecanismos de exclusión vistos mediante el uso de intrinsic locks, pero con un acercamiento diferente.

La implementación principal de Lock es ReentrantLock. El nombre se debe a que los locks en Java son reentrantes, por lo que una vez adquirido por un thread, si el mismo thread realiza un nuevo intento de adquirirlo este no fracasa. Lo que haremos será implementar los mismos ejemplos vistos más arriba con esta API.

Secciones críticas
Lock lock = new ReentrantLock();
//...
lock.lock();
try {
    //critical section...
} finally {
    lock.unlock();
}
Fácil, tan sólo tener en cuenta que debemos invocar el método unlock en la claúsula finally para garantizar que el lock es liberado incluso en caso de error.

Personalmente no diría que este mecanismo es mejor que el ofrecido por synchronized, siendo este último más compacto. Las grandes ventajas del uso de Lock vienen de una serie de métodos que nos dan la posibilidad de desarrollar mecanismos de locking más complejos como:

### tryLock(): intentamos adquirir el lock, pero el thread no se bloquea ni no lo consigue
### fairness: podemos crear un lock como “fair”. 
Por defecto los locks en Java no lo son, por lo que un thread en espera puede ser el elegido para adquirir el lock aunque sea el último que ha llegado. Con un fair lock se implementará un locking FIFO
Os recomiendo echar un vistazo completo a la API para más detalles.

### Mecanismos de espera
La implementación de estos mecanismos se realiza mediante el uso de la clase Condition. La creación de una instancia de Condition debe hacerse siempre a partir de un Lock:

### Condition condition = lock.newCondition();
La clase Condition expone dos métodos, await() y signal() que vienen a ser el equivalente a wait() y notify() en los intrinsic locks. Además podemos utilizar otros métodos como:

#### await(long time, TimeUnit unit): espera a una condición no más del tiempo proporcionado por parámetro
#### awaitUninterruptibly(): versión de await() no interrumpible. 
Es decir, si el thread que esté suspendido a la espera de una condición es interrumpido, este método no lanzará la conocida InterruptedException, por lo que solo pasará a estar activa si se invoca signal()/signalAll() sobre la condición (spurious wakeups aparte).
En general, para mecanismos de espera diría que el uso de Condition ofrece una seria de funcionalidades muy interesantes, además de permitir la creación de varias condiciones asociadas al mismo lock, cosa que no es posible (o si lo es su implementación es muy complicada) con intrinsic locks.

Veamos cómo queda nuestra aplicación mediante el uso de Lock y Condition:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;
    private final Condition myTurn;
    private Condition nextTurn;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock) {
        this.text = text;
        this.lock = lock;
        this.myTurn = lock.newCondition();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            lock.lock();

            try {
                while (!play)
                    myTurn.awaitUninterruptibly();

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                nextTurn.signal();
            } finally {
                lock.unlock();
            }
        }
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
        this.nextTurn = nextPlayer.myTurn;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }
}
Vemos como el uso de Condition hace más clara la lectura del código. Además, hemos utilizado el método awaitUninterruptibly, de forma que se garantiza fácilmente la consecución de la última jugada pendiente por parte de cada jugador cuando el hilo principal interrumpe los threads:

public class Game {

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }
}

## Versión 4.b: Locks explícitos y condiciones, con tiempo de espera

Vamos a modificar ligeramente nuestra aplicación para que los jugadores tengan un tiempo máximo para jugar. Si no lo hacen, el hilo principal los interrumpirá y finalizará el juego. Para ello, vamos a utilizar el método await de la clase Condition que nos permite esperar un tiempo máximo:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;
    private final Condition myTurn;
    private Condition nextTurn;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock) {
        this.text = text;
        this.lock = lock;
        this.myTurn = lock.newCondition();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            lock.lock();

            try {
                while (!play)
                    myTurn.await(1, TimeUnit.SECONDS);

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                nextTurn.signal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
        this.nextTurn = nextPlayer.myTurn;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }
}

Vemos que el método await recibe dos parámetros, el tiempo máximo de espera y la unidad de tiempo. Si el tiempo de espera se agota, el método retorna y el thread continúa su ejecución. En nuestro caso, si el tiempo de espera se agota el thread principal interrumpe a los jugadores y finaliza el juego.

## Versión 6: Locks explícitos y condiciones, con tiempo de espera y timeout

Vamos a modificar ligeramente nuestra aplicación para que los jugadores tengan un tiempo máximo para jugar. Si no lo hacen, el hilo principal los interrumpirá y finalizará el juego. Para ello, vamos a utilizar el método await de la clase Condition que nos permite esperar un tiempo máximo:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;
    private final Condition myTurn;
    private Condition nextTurn;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock) {
        this.text = text;
        this.lock = lock;
        this.myTurn = lock.newCondition();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            lock.lock();

            try {
                while (!play)
                    myTurn.await(1, TimeUnit.SECONDS);

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                nextTurn.signal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
        this.nextTurn = nextPlayer.myTurn;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }
}

la clase Game:

public class Game {

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }
}

# Ping Pong, Versión 6: Pool de threads y ScheduledExecutorService

Vamos a modificar ligeramente nuestro juego para que los jugadores tengan un tiempo máximo para jugar. Si no lo hacen, el hilo principal los interrumpirá y finalizará el juego. Para ello, vamos a utilizar el método await de la clase Condition que nos permite esperar un tiempo máximo:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;
    private final Condition myTurn;
    private Condition nextTurn;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock) {
        this.text = text;
        this.lock = lock;
        this.myTurn = lock.newCondition();
    }

    @Override
    public void run() {
        while(!Thread.interrupted()) {
            lock.lock();

            try {
                while (!play)
                    myTurn.await(1, TimeUnit.SECONDS);

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                nextTurn.signal();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                lock.unlock();
            }
        }
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
        this.nextTurn = nextPlayer.myTurn;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }
}

la clase Game:

public class Game {

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        Thread thread2 = new Thread(player2);
        thread2.start();
        Thread thread1 = new Thread(player1);
        thread1.start();

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        thread1.interrupt();
        thread2.interrupt();

        //Wait until players finish
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }
}

## 5. Escalando a N jugadores
Vamos a ver de qué forma tan sencilla podemos escalar el juego a varios jugadores, de forma que se vayan pasando la “pelota” entre ellos por orden. Es decir, la salida del programa sería algo como:

Game starting...!
player0
player1
player2
player3
player4
player5
...
Game finished!
Resulta que, ¡no necesitamos modificar para nada la clase Player! En efecto, como cada jugador solo ha de ser consciente del siguiente en el juego, los únicos cambios necesarios tendrán que hacerse en la clase Game:

public class GameScale {

    public static final int NUM_PLAYERS = 6;

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        int length = NUM_PLAYERS;

        Player[] players = new Player[length];

        for (int i=0; i < length; i++) {
            Player player = new Player("player"+i, lock);
            players[i] = player;
        }

        for (int i=0; i < length - 1; i++) {
            players[i].setNextPlayer(players[i+1]);
        }
        players[length - 1].setNextPlayer(players[0]);

        System.out.println("Game starting...!");

        players[0].setPlay(true);

        //Threads creation
        Thread[] threads = new Thread[length];
        for (int i=0; i < length; i++) {
            Thread thread = new Thread(players[i]);
            threads[i] = thread;
            thread.start();
        }

        //Let the players play!
        try {
            Thread.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        //Tell the players to stop
        for (Thread thread : threads) {
            thread.interrupt();
        }

        //Don't progress main thread until all players have finished
        try {
            for (Thread thread : threads) {
                thread.join();
            }
        }  catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("Game finished!");
    }

}
El código es algo más complejo, pero creo que se entiende bien. Tan sólo cambiando la constante podremos escalar el juego todo lo que queramos, y la concurrencia nos garantizará los turnos perfectamente :)


# Gestión de threads / Thread pools
Además de engorroso, no encapsular la gestión de threads convenientemente nos lleva a código poco cohesionado, ya que estamos ligando la lógica del juego en sí a la gestión de la concurrencia. Como añadido, crear theads es algo costoso a nivel de rendimiento, y en aplicaciones más complejas conlleva una carga importante en el rendimiento final de nuestras aplicaciones.

La API Concurrency de Java exporta una serie de clases e interfaces que nos permiten precisamente encapsular la gestión de hilos con gran flexibilidad, el Executor framework. 

Sus tres elementos principales son:

### Executor: es una interfaz de un sólo método, execute(Runnable). 
La idea con este framework es que ahora manejamos tareas (tasks) en lugar de hilos, por lo que le estamos pidiendo a la instancia de Executor que por favor ejecute la tarea (instancia de Runnable) cuando le sea posible

### ExecutorService: interfaz que extiende Executor 
publica una serie de métodos más avanzados, para controlar mejor el ciclo completo del trabajo a realizar (shutdown, awaitTermination), o para ejecutar tareas de tipo Callable, que a grandes rasgos son Runnable que devuelven un valor (más información aquí). En la documentación completa quedan bastante claras las posibilidades de esta interfaz

## Executors: los dos anteriores componentes son interfaces, de las que nosotros podemos crear implementaciones si así lo deseamos. 
Sin embargo, la mayoría de casos de uso están implementados en el JDK, para utilizar estas implementaciones debemos solicitar una instancia utilizando los métodos factory estáticos que expone esta clase

En general se utiliza el nombre de Thread Pool para refererise las implementaciones de Executor/ExecutorService que utilicemos para gestionar nuestros threads. 

Los tipos más comunes que podemos obtener mediante la factoría Executors son:

1. Single Thread Executor (newSingleThreadExecutor): contiene un solo thread que se encarga de ejecutar tareas. No es muy utilizado
2. Fixed Thread Pool (newFixedThreadPool): mantiene un número constante de threads “vivos”, esperando recibir tareas para ejecutar
3. Cached Thread Pool (newCachedThreadPool): mantiene un pool de threads que puede crecer o decrecer según demanda
4. Scheduled Thread Pool (newScheduledThreadPool): se utiliza para programar la ejecución de tareas. Devuelve una instancia de ScheduledExecutorService, ya que ExecutorService no expone métodos adecuados para programar tareas futuras, tan solo para ejecutarlas tan pronto como sea posible

# Ping Pong, Versión 5: Pool de threads
Sin necesidad de realizar modificaciones en la clase Player podemos adaptar nuestra clase Game para que utilice un pool de threads en lugar de encargarse ella de la engorrosa tarea de crear, arrancar y parar hilos. Veamos cómo quedaría:

public class Game {

    public static void main(String[] args) {
        Lock lock = new ReentrantLock();

        Player player1 = new Player("ping", lock);
        Player player2 = new Player("pong", lock);

        player1.setNextPlayer(player2);
        player2.setNextPlayer(player1);

        System.out.println("Game starting...!");

        player1.setPlay(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.execute(player1);
        executor.execute(player2);

        sleep(2);

        executor.shutdownNow();

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
Utilizamos un pool de dos threads (uno por jugador), y le enviamos las tareas a ejecutar. Nos dormimos 2ms para dejarles pelotear un poco, e invocamos a shutdownNow(), que viene a ser el equivalente a interrumpir los threads según hacíamos en las anteriores versiones, pero encapsulado en el pool. Es necesario invocar shutdownNow en lugar de shutdown, ya que este último deja terminar las tareas en ejecución y devuelve la lista de tareas pendientes. Cómo nuestros jugadores juegan infinitamente hasta que son interrumpidos, si intentamos terminar con shutdown la aplicación no acabaría nunca.

Bien, si probamos varias veces la aplicación, veremos que muchas veces funciona como es debido, mientras que otras la salida se presenta tal que así:

Game starting...!
ping
pong
//...
Game finished!
pong
¿Qué ha ocurrido? Tras solicitar a los threads su interrupción es posible que el hilo principal se adelante a esa finalización, y por eso el texto “Game finished!” aparece antes que la última jugada de “pong”. Explorando la API ExecutorService, vemos que tiene un método llamado awaitTermination. Este método bloquea el thread que lo invoca hasta que todas las tareas del pool han terminado o expira un timeout que le proporcionamos por parámetro:

//...
ExecutorService executor = Executors.newFixedThreadPool(2);

executor.execute(player1);
executor.execute(player2);

sleep(2);

executor.shutdownNow();

try {
    executor.awaitTermination(5, TimeUnit.SECONDS);
} catch (InterruptedException e) {
    System.out.println("Main thread interrupted while waiting for players to finish");
}

System.out.println("Game finished!");
//...
Ahora sí conseguimos la salida deseada, y el juego se comporta como queremos con una clase principal mucho más limpia y legible. ¿Hemos terminado? Aún no :)

# Barreras
Las barreras de entrada / salida, son mecanismos de sincronización que facilitan la ejecución simultánea de un grupo de threads (barrera de entrada), o la espera hasta finalizar la ejecución de (otra vez) otro pool de threads.

La idea de la barrera de salida (exit barrier) la hemos visto en el punto anterior con awaitTermination. No obstante, aunque este método nos posibilita crear una barrera de salida también nos obliga a establecer un timeout (que aunque puede ser de horas no deja de ser un timeout). Nosotros querríamos crear una barrera de salida sin timeout alguno.

Para entender lo que es una barrera de entrada vamos a añadir una instrucción a Game:

//...
executor.execute(player1);
sleep(1000);
executor.execute(player2);
//...
Dormimos el hilo principal durante un segundo antes de iniciar la ejecución del segundo jugador. Aunque el resultado es difícil de reproducir aquí, porque está relacionado con el timing de la aplicación, ocurre algo así:

Game starting...!
ping
// Waiting 1 second
pong
Es decir, el jugador “ping” pelotea, ¡pero durante un segundo no tiene a nadie al otro lado! Por lo que el juego queda “suspendido” un segundo, que podrían ser minutos (el tiempo que el hilo principal tarde en lanzar la ejecución del segundo jugador). Esta situación no es ideal, porque estamos arrancando el funcionamiento de un proceso concurrente que requiere la presencia de varios threads antes de que todos estén listos. Para evitar esto necesitamos utilizar una barrera de entrada (entry barrier).

Existen varias clases en la API concurrency que pueden utilizarse con fines de barrera, pero la más sencilla, y la que utilizaremos en ambos (barrera de entrada y salida) es CountdownLatch. El uso de esta clase puede resumirse en tres puntos:

Creamos una barrera con un contador inicializado a N
Los hilos que dependan de la barrera para continuar invocarán await(), y quedarán bloqueados hasta que el contador llegue a cero. También existe un método await() con timeout
Los actores que pueden influir en la apertura de la barrera invocarán countDown cuando se cumplan las condiciones adecuadas para liberarla. En general deben cumplirse N condiciones para que la apertura tenga lugar
Versión 6: Barreras de entrada / salida
En esta nueva versión deberemos modificar tanto Game como Player. Veamos como quedarían:

public class Player implements Runnable {

    private final String text;

    private final Lock lock;
    private final Condition myTurn;

    private final CountDownLatch entryBarrier;
    private final CountDownLatch exitBarrier;

    private Condition nextTurn;

    private Player nextPlayer;

    private volatile boolean play = false;

    public Player(String text,
                  Lock lock,
                  CountDownLatch entryBarrier,
                  CountDownLatch exitBarrier) {
        this.text = text;
        this.lock = lock;
        this.myTurn = lock.newCondition();

        this.entryBarrier = entryBarrier;
        this.exitBarrier = exitBarrier;
    }

    @Override
    public void run() {
        if(entryBarrierOpen())
            play();
    }

    public boolean entryBarrierOpen() {
        try {
            entryBarrier.await();
            return true;
        } catch (InterruptedException e) {
            System.out.println("Player "+text+
                                " was interrupted before starting Game!");
            return false;
        }
    }

    private void play() {
        while (!Thread.interrupted()) {
            lock.lock();

            try {
                while (!play)
                    myTurn.awaitUninterruptibly();

                System.out.println(text);

                this.play = false;
                nextPlayer.play = true;

                nextTurn.signal();
            } finally {
                lock.unlock();
            }
        }

        exitBarrier.countDown();
    }

    public void setNextPlayer(Player nextPlayer) {
        this.nextPlayer = nextPlayer;
        this.nextTurn = nextPlayer.myTurn;
    }

    public void setPlay(boolean play) {
        this.play = play;
    }
}
La clase no comienza a jugar hasta que la barrera de entrada (entryBarrier) esté abierta, y cuando es interrumpido para terminar invoca a countDown sobre la barrera de salida (exitBarrier), que será la forma de que Game sepa que ambos jugadores han terminado.

Pensad por un segundo a qué valores iniciaremos los contadores de entryBarrier y exitBarrier antes de seguir leyendo…

public class Game {

    public static void main(String[] args) {
        CountDownLatch entryBarrier = new CountDownLatch(1);
        CountDownLatch exitBarrier = new CountDownLatch(2);

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

        sleep(2);

        executor.shutdownNow();

        try {
            exitBarrier.await();
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
En efecto, la barrera de entrada tiene un contador de 1, porque se abrirá tan pronto como el hilo principal haya pasado todas las tareas al pool de threads, mientras que la barrera de salida, que aquí se utiliza como alternativa a awaitTermination, tiene un contador de 2, que es el número de actores que debe finalizar su ejecución antes de que el hilo principal pueda proseguir.

De esta forma el timing de la aplicación es el deseado, aunque para ello hayamos tenido que complicar un poco el código. El tema es que la concurrencia de por sí es compleja, por lo que es difícil encapsular perfectamente todos los mecanismos utilizados.

Antes de terminar el post, mencionar que la barrera de salida ha sido añadida a esta versión a efectos didácticos. El mejor mecanismo para esperar la finalización de un grupo de threads en un pool es la espera mediante awaitTermination, introduciendo un timeout razonable, de forma que si alcanzamos el timeout sea porque algún fallo está ocurriendo en las tareas de las que esperamos su terminación. 


versión 7 donde se utiliza la barrera de entrada y awaitTermination como barrera de salida, pudiéndose considerar ésta la versión óptima de la aplicación.



# 7. Conclusiones

En este post hemos visto cómo podemos implementar mecanismos de concurrencia en Java, y cómo podemos utilizarlos para resolver problemas de sincronización. Hemos visto que la concurrencia es un tema complejo, y que no es fácil encapsularla correctamente, por lo que es importante conocer bien los mecanismos que nos ofrece el lenguaje para implementarla.