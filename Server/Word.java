package Server;

public class Word {
    
    // Parola segreta precedente
    private volatile String previousWord;
    // Parola segreta attuale
    private volatile String actualWord;
    // Contatore numero di parole estratte
    private volatile int updateCounter;

    public Word(String secretWord) {
        this.actualWord = secretWord;
        this.previousWord = null;
        this.updateCounter = 0;
    }

    public String getActualWord() {
        return actualWord;
    }

    public void setActualWord(String newWord) {
        this.actualWord = newWord;
        this.updateCounter++;
        return;
    }

    public String getPreviousWord() {
        return this.previousWord;
    }

    public void setPreviousWord() {
        this.previousWord = this.actualWord;
        return;
    }

    public int getUpdateCounter() {
        return this.updateCounter;
    }

    public void setUpdateCounter(int i) {
        this.updateCounter = i;
    }

}
