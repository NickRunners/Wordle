package Client;

import java.io.InputStream;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.net.Socket;
import java.net.MulticastSocket;

public class ClientMain {

    // Nome file di configurazione Client
    public static final String configFile = "Client/clientmain.properties";

    // PARAMETRI IMPORTATI DA FILE .PROPERTIES
    // Indirizzo e porta per gioco con collegamento TCP
    private static String gameAddress;
    private static int gamePort;
    // Indirizzo e porta per gruppo sociale con collegamento UDP
    private static String socialAddress;
    private static int socialPort;

    // ALTRE DICHIARAZIONI
    // Scanner per lettura da tastiera e da Socket (client e server)
    private static Scanner keyboardScan;
    private static Scanner sockScan;
    // Stringhe temporanee per comunicazione tra client e server
    private static String serverLine;
    private static String clientLine;
    // Struttura dati per salvataggio informazioni condivise sul gruppo sociale
    private static ConcurrentLinkedQueue<String> othersStats;

    public static void main(String[] args) {

        // LETTURA PARAMETRI DI CONFIGURAZIONE DA .PROPERTIES
        try {
            readConfig();
        } catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // CREAZIONE SOCKET PER GIOCO
        try(Socket gameSocket = new Socket(gameAddress, gamePort)) {
            // Connessione con server
            System.out.println("[CLIENT] Connesso al server.");
            // Inizializzazione Scanner per lettura da tastiera e da Socket
            keyboardScan = new Scanner(System.in);
            sockScan = new Scanner(gameSocket.getInputStream());
            // Inizializzazione PrintWriter per scrittura su Socket
            PrintWriter out = new PrintWriter(gameSocket.getOutputStream(), true);

            // Dichiarazione Socket e thread per Multicast (gruppo sociale)
            MulticastSocket socialSocket = null;
            ClientThread receiver = null;

            // INIZIO COMUNICAZIONE CON SERVER
            boolean end = false;
            // Finché il gioco non viene chiuso
            while(!end) {
                serverLine = sockScan.nextLine();
                // Notifica utente loggato
                if(serverLine.equals("logged")) {
                    // Inizializzazione Socket per gruppo sociale
                    socialSocket = new MulticastSocket(socialPort);
                    // Inizializzazione coda
                    othersStats = new ConcurrentLinkedQueue<String>();
                    // Inizializzazione ed esecuzione thread parallelo per ricezione
                    receiver = new ClientThread(socialAddress, socialSocket, othersStats);
                    receiver.start();
                    System.out.println("Accesso effettuato con successo.\n");
                }
                // Richiesta inserimento input da tastiera
                else if(serverLine.equals("write")) {
                    clientLine =  keyboardScan.nextLine();
                    out.println(clientLine);
                }
                // Richiesta inserimento con controllo scelta in fase di login
                else if(serverLine.equals("writeLogin")) {
                    clientLine =  checkLoginInput(keyboardScan);
                    out.println(clientLine);
                }
                // Richiesta inserimento con controllo scelta in fase di gioco
                else if(serverLine.equals("writeGame")) {
                    clientLine =  checkGameInput(keyboardScan);
                    out.println(clientLine);
                }
                // Richiesta stampa informazioni gruppo sociale
                else if(serverLine.equals("info")) {
                    if(othersStats.size() != 0) {
                        System.out.println("");
                        Iterator<String> i = othersStats.iterator();
                        while(i.hasNext())
                            System.out.println(i.next());
                    }
                    else
                        System.out.println("Non ci sono messaggi condivisi...\n");
                }
                // Disconnessione senza chiusura del client
                else if(serverLine.equals("logout")) {
                    // Uscita da gruppo sociale
                    // e interruzione thread con eccezione
                    socialSocket.close();
                    // Pulizia coda gruppo sociale
                    othersStats.clear();
                }
                // Uscita, chiusura client
                else if(serverLine.equals("exitLogged")) {
                    end = true;
                    socialSocket.close();
                    break;
                }
                else if(serverLine.equals("exitNotLogged")) {
                    end = true;
                    break;
                }
                else {
                    System.out.println(serverLine);
                }
            }
            System.out.println("[CLIENT] Disconnessione e terminazione avvenute con successo.");
            return;
        }
        catch(NoSuchElementException e) {
            System.out.println("[CLIENT] Il server è stato chiuso, il client verrà chiuso.");
            System.exit(0);
        }
        catch(IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /*
     * Funzione di utilità per leggere i parametri di input dal file di configurazione
     */
    public static void readConfig() throws FileNotFoundException, IOException {
		InputStream input = new FileInputStream(configFile);
		Properties prop = new Properties();
		prop.load(input);
        gameAddress = prop.getProperty("gameAddress");
		gamePort = Integer.parseInt(prop.getProperty("gamePort"));
        socialAddress = prop.getProperty("socialAddress");
        socialPort = Integer.parseInt(prop.getProperty("socialPort"));
		input.close();
	}

    /*
     * Funzione di utilità per controllo dell'input da tastiera durante la fase di login
     */
    private static String checkLoginInput(Scanner keyboardScanner) {
        int choose = 0;
        while(true) {
            try {
                choose = Integer.parseInt(keyboardScanner.nextLine());
                if(choose<1 || choose>3)
                    System.out.println("Comando non valido, inserire un numero compreso tra 1 e 3");
                else
                    break;
            } catch(NumberFormatException e) {
                System.out.println("Comando non valido, inserire un numero compreso tra 1 e 3");
            }
        }
        return Integer.toString(choose);
    }

    /*
     * Funzione di utilità per controllo dell'input da tastiera durante la fase di gioco
     */
    private static String checkGameInput(Scanner keyboardScanner) {
        int choose = 0;
        while(true) {
            try {
                choose = Integer.parseInt(keyboardScanner.nextLine());
                if(choose<1 || choose>6)
                    System.out.println("Comando non valido, inserire un numero compreso tra 1 e 6");
                else
                    break;
            } catch(NumberFormatException e) {
                System.out.println("Comando non valido, inserire un numero compreso tra 1 e 6");
            }
        }
        return Integer.toString(choose);
    }

}
