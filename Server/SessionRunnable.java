package Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.NoSuchElementException;
import java.net.Socket;
import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;


public class SessionRunnable implements Runnable {
    
    // Socket per connessione TCP con client
    private Socket socket;
    // Utente partecipante alla sessione di gioco
    private User user;
    // Classe per parola segreta e Set di tutte le possibili parole
    private Word word;
    private Set<String> words;
    // HashMap di dati utenti
    private ConcurrentHashMap<String, User> players;
    // Indirizzo e porta per gruppo sociale con UDP
    private String socialAddress;
    private int socialPort;

    public SessionRunnable(Socket socket, Word word, Set<String> words, ConcurrentHashMap<String, User> players, String address, int port) {
        this.socket = socket;
        this.user = null;
        this.word = word;
        this.words = words;
        this.players = players;
        this.socialAddress = address;
        this.socialPort = port;
    }

    public void run() {
        // Inizializzazione Scanner per lettura da Socket
        // e PrintWriter per scrittura su Socket
        try (Scanner in = new Scanner(this.socket.getInputStream());
            PrintWriter out = new PrintWriter(this.socket.getOutputStream(), true);) {
            // Scelta comando utente
            int choose;
            // Credenziali utente
            String[] credentials;
            // Variabili di controllo per accesso, registrazione e uscita
            int logged;
            boolean registered;
            boolean exit = false;
            out.println("Benvenuto in Wordle!");
            while(!exit) {

                // Inizializzazione utente
                this.user = null;
                
                // FASE DI ACCESSO-REGISTRAZIONE
                logged = 1;
                registered = false;
                // Ciclo finché utente non loggato con possibilità di uscita
                while((this.user==null || this.user.getStatus()==false) && !exit) {
                    out.println("1> ACCESSO\n2> REGISTRAZIONE\n3> ESCI");
                    // Comunicazione a client di attesa comando da tastiera (utente)
                    out.println("writeLogin");
                    choose = Integer.parseInt(in.nextLine());
                    switch(choose) {
                        // RICHIESTA ACCESSO
                        case 1:
                            // Controllo credenziali login, gira finché utente non loggato
                            while(logged != 0) {
                                out.println("[ACCESSO] Inserisci nome utente e password separate da uno spazio");
                                out.println("write");
                                credentials = in.nextLine().split(" ");
                                while(credentials.length != 2) {
                                    out.println("Input non valido, riprovare: assicurati di aver inserito solo nome utente e password non vuota separati da uno spazio");
                                    out.println("write");
                                    credentials = in.nextLine().split(" ");
                                }
                                logged = login(credentials[0], credentials[1]);
                                if(logged == -1) {
                                    out.println("Nome utente non valido: utente non registrato");
                                    continue;
                                }
                                else if(logged == -2) {
                                    out.println("Un utente con lo stesso nome utente sta già giocando");
                                    continue;
                                }
                                else if(logged == -3) {
                                    out.println("Password errata");
                                    continue;
                                }
                                // Recupero utente da struttura
                                // e set stato a true
                                user = players.get(credentials[0]);
                                user.setStatus(true);
                            }
                            out.println("logged");
                            break;

                        //RICHIESTA REGISTRAZIONE
                        case 2:
                            // Controllo credenziali registrazione, gira finché utente non registrato
                            while(!registered) {
                                out.println("[REGISTRAZIONE] Inserisci nome utente e password separate da uno spazio");
                                out.println("write");
                                credentials = in.nextLine().split(" ");
                                while(credentials.length != 2) {
                                    out.println("Input non valido, riprovare: assicurati di aver inserito solo nome utente e password non vuota separati da uno spazio");
                                    out.println("write");
                                    credentials = in.nextLine().split(" ");
                                }
                                registered = register(credentials[0], credentials[1]);
                                if(registered == false)
                                    out.println("Nome utente già in uso");
                            }
                            out.println("Registrazione avvenuta con successo.\n");
                            // Aggiorno file JSON
                            ServerMain.updatePlayers();
                            break;

                        // RICHIESTA USCITA
                        case 3:
                            out.println("exitNotLogged");
                            exit = true;
                            break;
        
                        // ALTRO INPUT
                        default:
                            out.println("Comando non valido, inserire un numero compreso tra 1 e 3\n");
                            break;
                    }
                }
                // Se utente ha richiesto uscita
                if(exit == true)
                    break;

                //FASE DI SCELTA AZIONE - MENÙ PRINCIPALE
                out.println("Benvenuto "+user.getUsername()+"!");
                // Contatore tentativi
                int nTentativi;
                // Contatore parola estratta
                int counterWord;
                // Parola segreta attuale
                String secretWord;
                // Tentativo utente
                String guess;
                // Indizi
                char[] tempRes;
                // Indica se utente sta giocando
                boolean playing;
                // Indica se la parola è cambiata
                // mentre l'utente sta giocando
                boolean changed;
                while(user!=null && user.getStatus()==true && !exit) {
                    out.println("1> GIOCA\n2> VISUALIZZA STATISTICHE\n3> CONDIVIDI STATISTICHE ULTIMA PARTITA\n4> VISUALIZZA STATISTICHE ULTIMA PARTITA DEGLI ALTRI GIOCATORI\n5> LOGOUT\n6> ESCI");
                    out.println("writeGame");
                    choose = Integer.parseInt(in.nextLine());
                    switch(choose) {
                        // RICHIESTA AVVIO PARTITA
                        case 1:
                            // Inizializzazione variabili
                            nTentativi = 0;
                            secretWord = word.getActualWord();
                            counterWord = word.getUpdateCounter();
                            guess = "";
                            tempRes = "XXXXXXXXXX".toCharArray();
                            playing = true;
                            changed = false;
                            while(playing) {
                                // Utente ha già partecipato per la parola corrente
                                if(this.user.getLastGame().getLastGameCounter() == this.word.getUpdateCounter()) {
                                    out.println("Hai già partecipato al gioco, ritorna domani\n");
                                    playing = false;
                                    break;
                                }
                                // Reset cronologia tentativi
                                user.getLastGame().resetClues();
                                // Aggiornamento numero di partita
                                user.getLastGame().setLastGameCounter(counterWord);
                                while(nTentativi < 12 && !(secretWord.equals(guess))) {
                                    out.println("Inserisci un tentativo di parola di 10 lettere o 'esci' per abbandonare la partita");
                                    out.println("write");
                                    guess = in.nextLine();
                                    // Controllo se la parola è cambiata mentre l'utente sta giocando
                                    // controllo anche su numero parola estratta per evitare
                                    // caso in cui la parola nuova sia uguale alla vecchia ma sia 
                                    // comunque cambiata
                                    if(secretWord.equals(word.getPreviousWord()) && counterWord<word.getUpdateCounter()) {
                                        nTentativi = 0;
                                        counterWord = word.getUpdateCounter();
                                        secretWord = word.getActualWord();
                                        out.println("Hai esaurito il tempo, la parola da indovinare è cambiata.");
                                        out.println("La partita verrà considerata persa, le statistiche relative ad essa saranno comunque salvate.");
                                        changed = true;
                                        break;
                                    }
                                    if(guess.equals("esci"))
                                        break;
                                    if(guess.length() != 10) {
                                        out.println("La parola non è di 10 lettere, riprova");
                                        continue;
                                    }
                                    if(!this.words.contains(guess)) {
                                        out.println("La parola inserita non è tra quelle indovinabili, riprova");
                                        continue;
                                    }
                                    // Composizione stringa per indizi
                                    for(int i=0; i<guess.length(); i++) {
                                        for(int j=0; j<secretWord.length(); j++) {
                                            if(i!=j && guess.charAt(i)==secretWord.charAt(j))
                                                tempRes[i] = '?';
                                            if(guess.charAt(i)==secretWord.charAt(i))
                                                tempRes[i] = '+';
                                        }
                                    }
                                    out.println(user.getLastGame().insertClues(new String(tempRes)));
                                    nTentativi++;
                                }
                                if(secretWord.equals(guess) && changed!=true) {
                                    out.println("Congratulazioni! Hai indovinato la parola!");
                                    // Aggiornamento statistiche personali
                                    user.increaseWonMatch();
                                    user.increaseLastStreak();
                                    user.refreshMaxStreak();
                                    user.setGuessDistribution(nTentativi-1);
                                }
                                else {
                                    out.println("Peccato, non sei riuscito ad indovinare...");
                                    user.resetLastStreak();
                                }
                                out.println("");
                                // Aggiornamento statistiche personali
                                user.increasePlayedMatch();
                                user.refreshWinRate();
                                user.getLastGame().setNeverPlayed(false);
                                break;
                            }
                            break;

                        // VISUALIZZA STATISTICHE
                        case 2:
                            out.println("[STATISTICHE] "+user.getUsername());
                            out.println("Partite giocate: "+user.getPlayedMatch());
                            out.println("Partite vinte: "+user.getWonMatch());
                            out.println("Rateo di vittoria: "+user.getWinRate());
                            out.println("Serie attuale di vittorie: "+user.getLastStreak());
                            out.println("Serie massima di vittorie: "+user.getMaxStreak());
                            out.println("Guess distribution: ");
                            for(int i=0; i<user.getGuessDistribution().length; i++)
                                out.println((i+1)+" tentativi -> "+user.getGuessDistribution()[i]+" vittorie");
                            out.println("");
                            break;

                        // CONDIVIDI STATISTICHE CON ALTRI GIOCATORI
                        case 3:
                            if(this.user.getLastGame().getNeverPlayed())
                                out.println("Non puoi condividere dati di gioco senza aver mai giocato!\n");
                            else{
                                sendPacket(this.user.lastGameToString(), this.socialAddress, this.socialPort);
                                out.println("Statistiche condivise!\n");
                            }
                            break;

                        // VISUALIZZA STATISTICHE DEGLI ALTRI
                        case 4:
                            out.println("info");
                            break;

                        // LOGOUT
                        case 5:
                            logout(this.user);
                            // Aggiorno file JSON
                            ServerMain.updatePlayers();
                            out.println("Grazie per aver giocato, sarai reinderizzato al portale di accesso.\n");
                            out.println("logout");
                            break;

                        // USCITA
                        case 6:
                            logout(this.user);
                            // Aggiorno file JSON
                            ServerMain.updatePlayers();
                            out.println("Grazie per aver giocato!");
                            out.println("exitLogged");
                            exit = true;
                            break;
                        
                        // ALTRO INPUT
                        default:
                            out.println("!Comando non valido, inserire un numero compreso tra 1 e 6\n");
                            break;
                    }
                }
            }
            System.out.println("[SEREVR] "+this.socket+" disconnesso");
            return;
        }
        // Eccezione se ^C su client
        catch(NoSuchElementException e) {
            System.out.println("[SERVER] "+this.socket+" disconnesso, eccezione NoSuchElement (^C)");
            // L'utente viene scollegato se viene forzata la chiusura del client
            if(this.user!=null && this.user.getStatus()==true)
                this.user.setStatus(false);
            return;
        }
        // Eccezione updatePlayers()
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
     * false se username registrato
     * true altrimenti
     */
    private boolean checkUsername(String username) {
        if(players.get(username) != null)
            return false;
        return true;
    }

    /*
    * false se nome utente già in uso
    * true se registrazione va a buon fine
    */
    private boolean register(String username, String password) {
        synchronized(players) {
            if(checkUsername(username) == false)
                return false;
            players.put(username, new User(username, password));
            return true;
        }
    }

    /*
     * -1 se utente non registrato
     * -2 se utente attualmente loggato da altro client
     * -3 se password non corrisponde
     * 0 altrimenti
     */
    private int login(String username, String password) {
        synchronized(players) {
            if(checkUsername(username) == true)
                return -1;
            if(players.get(username).getStatus() == true)
                return -2;
            if(!players.get(username).getPassword().equals(password))
                return -3;
            else {
                players.get(username).setStatus(true);
                return 0;
            }
        }
    }

    /*
     * esegue il logout dell'utente impostando "status" a false
     * sincronizzazione per evitare concorrenza se altro utente
     * prova a fare login contemporaneamente
     */
    private void logout(User user) {
        synchronized(players) {
            user.setStatus(false);
            user = null;
        }
    }

    /*
     * Condivisione informaioni su gruppo Multicast
     */
    private static void sendPacket(String message, String ipAddress, int port) throws IOException {
        DatagramSocket socket = new DatagramSocket();
        InetAddress group = InetAddress.getByName(ipAddress);
        byte[] msg = message.getBytes();
        DatagramPacket packet = new DatagramPacket(msg, msg.length, group, port);
        socket.send(packet);
        socket.close();
    }

}
