package Server;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TerminationHandler extends Thread {
    
    // ServerSocket da chiudere
    private ServerSocket serverSocket;
    // ThreadPool per sessioni da terminare
    private ExecutorService pool;
    // ThreadPool per cambio parola
    private ScheduledExecutorService scheduler;
    // Delay per terminazione ThreadPool
    private int maxDelay;

    public TerminationHandler(ServerSocket serverSocket, ExecutorService pool, ScheduledExecutorService scheduler, int maxDelay) {
        this.serverSocket = serverSocket;
        this.pool = pool;
        this.scheduler = scheduler;
        this.maxDelay = maxDelay;
    }

    public void run() {
        System.out.println("[SERVER] Inizio terminazione...");

        // SERIALIZZAZIONE E SALVATAGGIO INFORMAZIONI SU FILE JSON
        try {
            ServerMain.updatePlayers();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        
        // CHIUSURA SOCKET, INTERRUZIONE NUOVE RICHIESTE DI CONNESSIONE
        try {
            this.serverSocket.close();
        } catch(IOException e) {
            System.err.println("[SERVER] Errore: "+e.getMessage());
            System.exit(-1);
        }

        // TERMINAZIONE THREADPOOL CONNESSIONI E CAMBIO PAROLA
        this.pool.shutdown();
        try {
            if(!this.pool.awaitTermination(this.maxDelay, TimeUnit.MILLISECONDS))
                this.pool.shutdownNow();
        } catch(InterruptedException e) {
            this.pool.shutdownNow();
        }
        this.scheduler.shutdown();
        try {
            if(!this.scheduler.awaitTermination(this.maxDelay, TimeUnit.MILLISECONDS))
                this.scheduler.shutdownNow();
        } catch(InterruptedException e) {
            this.scheduler.shutdownNow();
        }

        System.out.println("[SERVER] terminazione avvenuta con successo.");
    }

}
