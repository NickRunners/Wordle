package Server;

import java.util.LinkedList;

public class LastGame {
    
    // Indizi 
    private LinkedList<String> clues;
    // Numero partita
    private int lastGameCounter;
    // Partita mai effettuata
    private boolean neverPlayed;

    public LastGame() {
        this.clues = new LinkedList<String>();
        this.lastGameCounter = 0;
        this.neverPlayed = true;
    }

    public LinkedList<String> getClues() {
        return this.clues;
    }

    public void setClues(LinkedList<String> clues) {
        this.clues = clues;
    }

    public String insertClues(String s) {
        this.clues.addFirst(s);
        return this.clues.getFirst();
    }

    public void resetClues() {
        this.clues.clear();
    }

    public int getSize() {
        return this.clues.size();
    }

    public int getLastGameCounter() {
        return this.lastGameCounter;
    }

    public void setLastGameCounter(int i) {
        this.lastGameCounter = i;
    }

    public boolean getNeverPlayed() {
        return this.neverPlayed;
    }

    public void setNeverPlayed(boolean flag) {
        this.neverPlayed = flag;
    }

}
