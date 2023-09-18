package Client;

import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketException;
import java.net.DatagramPacket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClientThread extends Thread {
    
    // Indirizzo gruppo sociale
    private String socialAddress;
    // Socket per gruppo sociale
    private MulticastSocket socialSocket;
    // Dimensione buffer del packet
    private final int size = 1024;
    // Struttura dati per salvataggio informazioni condivise sul gruppo sociale
    private ConcurrentLinkedQueue<String> othersStats;

    public ClientThread(String address, MulticastSocket socket, ConcurrentLinkedQueue<String> queue) {
        this.socialAddress = address;
        this.socialSocket = socket;
        this.othersStats = queue;
    }

    public void run() {
        try {
            // Traduzione indirizzo del gruppo e controllo validità
            InetAddress group = InetAddress.getByName(this.socialAddress);
            if(!group.isMulticastAddress())
                throw new IllegalArgumentException("[CLIENT] Indirizzo multicast non valido: "+group.getHostAddress());
            // Partecipazione al gruppo multicast
            socialSocket.joinGroup(group);
            while(true) {
                // Creazione pacchetto per ricezione messaggi
                DatagramPacket packet = new DatagramPacket(new byte[size], size);
                // Cattura eccezione se il Socket viene chiuso
                // e termina esecuzione thread
                try {
                    // Ricezione pacchetto
                    socialSocket.receive(packet);
                } catch(SocketException e) {
                    break;
                }
                // Creazione e inserimento stringa in coda
                String temp = new String(packet.getData(), packet.getOffset(), packet.getLength());
                if(!othersStats.contains(temp))
                    this.othersStats.add(temp);
            }
        }
        catch(Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

}
