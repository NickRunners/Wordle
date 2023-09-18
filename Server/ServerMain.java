package Server;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.lang.reflect.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

public class ServerMain {

    // Nome file di configurazione server
    private static final String configFile = "Server/servermain.properties";

    // PARAMETRI IMPORTATI DA FILE .PROPERTIES
    // Porta per gioco con collegamento TCP
    private static int gamePort;
    // Indirizzo e porta per gruppo sociale con collegamento UDP
    private static String multicastAddress;
    private static int multicastPort;
    // Tempo per cambio parola
    private static int changeTime;
    // Delay per terminazione ThreadPool
    private static int maxDelay;

    // ALTRE DICHIARAZIONI
    // Cached ThreadPool per gestione sessioni multi-client
    private static ExecutorService pool;
    // Scheduled ThreadPool per cambio parola segreta 
    private static ScheduledExecutorService scheduler;
    // ServerSocket per ricevere le richieste dei client
    private static ServerSocket serverSocket;
    // HashMap per credenziali
    private static ConcurrentHashMap<String, User> players;
    // Set contentente tutte le parole
    private static Set<String> words;
    // Parola segreta
    private static Word word;
    // Numero partita corrente
    private static int gameCount;

    public static void main(String[] args) {

        // INIZIALIZZAZIONE E SALVATAGGIO PAROLE DEL FILE SU HASHSET
        words = new HashSet<String>();
        try(Scanner scanWord = new Scanner(new File("Server/File/words.txt"));) {
            while(scanWord.hasNext()) {
                words.add(scanWord.nextLine());
            }
        } catch(FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // IMPORTAZIONE INFORMAZIONE UTENTI REGISTRATI
        try {
            // Inizializzazione HashMap per credenziali e
            // file JSON
            players = new ConcurrentHashMap<String, User>();
            File file = new File("Server/File/players.json");
            // Lettura da JSON se esiste e non è vuoto
            if(file.exists() && file.length()!=0) {
                JsonReader reader = new JsonReader(new FileReader(file));
                Gson gson = new Gson();
                Type userType = new TypeToken<User>() {}.getType();
                reader.beginArray();
                reader.beginObject();
                reader.nextName();
                gameCount = reader.nextInt();
                reader.endObject();
                reader.beginObject();
                while(reader.hasNext()) {
                    reader.nextName();
                    User tempUser = gson.fromJson(reader, userType);
                    players.put(tempUser.getUsername(), tempUser);
                }
                reader.endObject();
                reader.endArray();
                reader.close();
            }
            // Se file non esiste inizio da partita numero 0
            else if(file.exists() && file.length()==0)
                gameCount = 0;
            // Se file non esiste
            else {
                gameCount = 0;
                // Creazione file
                try {
                    file.createNewFile();
                } catch(IOException e) {
                    System.err.println(e.getMessage());
                    System.exit(-1);
                }
            }

            // INIZIALIZZAZIONE PARAMETRI DA FILE DI CONFIGURAZIONE
            readConfig();

            // INIZIALIZZAZIONE OGGETTO PAROLA SEGRETA
            word = new Word(null);
            word.setUpdateCounter(gameCount);

            // INIZIALIZZAZIONE E AVVIO SCHEDULER PER CAMBIO PAROLA
            scheduler = Executors.newScheduledThreadPool(1);
            scheduler.scheduleAtFixedRate(new WordRunnable(word, words), 0, changeTime, TimeUnit.HOURS);

            //INIZIALIZZAZIONE THREADPOOL PER SESSIONI
            pool = Executors.newCachedThreadPool();

            // APERTURA SERVER
            serverSocket = new ServerSocket(gamePort);
            // Avvio TerminationHandler per la gestione della terminazione del server
            Runtime.getRuntime().addShutdownHook(new TerminationHandler(serverSocket, pool, scheduler, maxDelay));
            System.out.println("[SERVER] In attesa di connessioni:");
            while (true) {
	        	Socket socket = null;
                // Cattura eccezione chiusura Socket
	        	try {
                    // Accettazione richieste provenienti dai client
                    socket = serverSocket.accept();
                    System.out.println(socket+" connesso");
                } catch(SocketException e) {
                    break;
                }
                // Avvio nuovo thread per gestione della nuova sessione di gioco col client
	            pool.execute(new SessionRunnable(socket, word, words, players, multicastAddress, multicastPort));
	        }
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        catch(Exception e) {
            System.err.println("[SERVER] Errore: "+e.getMessage());
            e.printStackTrace();
            System.exit(-1);
        }
        return;
    }

    /*
     * Funzione di utilità per scrittura su file JSON
     */
    public static synchronized void updatePlayers() throws IOException {
        // Inizializzazione e definizione file JSON
        File file = new File("Server/File/players.json");
        // Scrittura su JSON
        JsonWriter writer;
        writer = new JsonWriter(new FileWriter(file));
        writer.beginArray();
        writer.beginObject();
        writer.name("gameCount").value(word.getUpdateCounter());
        writer.endObject();
        writer.beginObject();
        Iterator<String> it = players.keySet().iterator();
        while(it.hasNext()) {
            String key = it.next();
            writer.name(key);
            writer.beginObject();
            writer.name("username").value(players.get(key).getUsername());
            writer.name("password").value(players.get(key).getPassword());
            writer.name("playedMatch").value(players.get(key).getPlayedMatch());
            writer.name("wonMatch").value(players.get(key).getWonMatch());
            writer.name("winRate").value(players.get(key).getWinRate());
            writer.name("lastStreak").value(players.get(key).getLastStreak());
            writer.name("maxStreak").value(players.get(key).getMaxStreak());
            writer.name("guessDistribution");
            writer.beginArray();
            for(int i=0; i<12; i++)
                writer.value(players.get(key).getGuessDistribution()[i]);
            writer.endArray();
            writer.name("lastGame");
            writer.beginObject();
            writer.name("clues");
            writer.beginArray();
            for(String s : players.get(key).getLastGame().getClues())
                writer.value(s);
            writer.endArray();
            writer.name("lastGameCounter").value(players.get(key).getLastGame().getLastGameCounter());
            writer.name("neverPlayed").value(players.get(key).getLastGame().getNeverPlayed());
            writer.endObject();
            writer.endObject();
        }
        writer.endObject();
        writer.endArray();
        writer.close();
    }

    /*
     * Funzione di utilità per leggere i parametri di input dal file di configurazione
     */
    public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
		Properties prop = new Properties();
		prop.load(input);
		gamePort = Integer.parseInt(prop.getProperty("gamePort"));
        multicastAddress = prop.getProperty("multicastAddress");
        multicastPort = Integer.parseInt(prop.getProperty("multicastPort"));
        changeTime = Integer.parseInt(prop.getProperty("changeTime"));
        maxDelay = Integer.parseInt(prop.getProperty("maxDelay"));
		input.close();
	}

}