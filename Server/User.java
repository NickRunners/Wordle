package Server;

//import java.util.HashMap;

public class User {
    
    // Username e password
    private String username;
    private String password;
    // Numero di partite giocate
    private int playedMatch;
    // Numero di partite vinte
    private int wonMatch;
    // Percentuale di vittoria
    private double winRate;
    // Serie attuale di vittorie
    private int lastStreak;
    // Serie massima di vittorie
    private int maxStreak;
    private int[] guessDistribution;
    // Status: online/offline
    private boolean status;
    // Informazioni riguardanti
    // l'ultima partita giocata
    private LastGame lastGame;

    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.playedMatch = 0;
        this.wonMatch = 0;
        this.winRate = 0;
        this.lastStreak = 0;
        this.maxStreak = 0;
        this.guessDistribution = new int[12];
        this.status = false;
        this.lastGame = new LastGame();
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    public int getPlayedMatch() {
        return this.playedMatch;
    }

    public void increasePlayedMatch() {
        this.playedMatch++;
        return;
    }

    public int getWonMatch() {
        return this.wonMatch;
    }

    public void increaseWonMatch() {
        this.wonMatch++;
        return;
    }

    public double getWinRate() {
        return this.winRate;
    }

    public void refreshWinRate() {
        this.winRate = ((double)this.wonMatch/(double)this.playedMatch)*100;
        return;
    }

    public int getLastStreak() {
        return this.lastStreak;
    }

    public void increaseLastStreak() {
        this.lastStreak++;
        return;
    }

    public void resetLastStreak() {
        this.lastStreak = 0;
        return;
    }

    public int getMaxStreak() {
        return this.maxStreak;
    }

    public void refreshMaxStreak() {
        if(this.maxStreak < this.lastStreak)
            this.maxStreak = this.lastStreak;
        return;
    }

    public int[] getGuessDistribution() {
        return this.guessDistribution;
    }

    public void setGuessDistribution(int i) {
        this.guessDistribution[i]++;
        return;
    }

    public boolean getStatus() {
        return this.status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public LastGame getLastGame() {
        return this.lastGame;
    }

    public void setLastGame(LastGame lastGame) {
        this.lastGame = lastGame;
    }

    /*
     * Rende le informazioni relative all'ultima partita giocata sottoforma 
     * di stringa per la condivisione su gruppo sociale UDP
     */
    public String lastGameToString() {
        String toString1 = this.username+"\n--------\n"+"Wordle "+this.getLastGame().getLastGameCounter()+": "+this.getLastGame().getSize()+"/12\n"; 
        String toString2 = "";
        for(int i=0; i<this.getLastGame().getSize(); i++)
            toString2 += this.getLastGame().getClues().get(i)+"\n";
        String toString = toString1+toString2;
        if(this.getLastGame().getSize() == 0)
            toString += "Nessun tentativo effettuato\n"; 
        return toString;
    }

}