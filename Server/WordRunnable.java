package Server;

import java.util.Iterator;
import java.util.Set;

public class WordRunnable implements Runnable {

    private Word word;
    private Set<String> wordSet;

    public WordRunnable(Word word, Set<String> wordSet) {
        this.word = word;
        this.wordSet = wordSet;
    }

    public void run() {
        this.word.setPreviousWord();
        this.word.setActualWord(pickWord(this.wordSet));
        System.out.println("La parola attuale è: "+this.word.getActualWord());
        return;
    }

    /*
     * Sceglie una parola casuale dal Set di parole
     */
    private String pickWord(Set<String> set) {
        Iterator<String> iterator = set.iterator();
        String randomWord = null;
        int random = (int)(Math.random()*(set.size()+1));
        int count = 0;
        while(iterator.hasNext()) {
            randomWord = iterator.next();
            if(count == random)
                return randomWord;
            count++;
        }
        return randomWord;
    }

}