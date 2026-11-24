package club.abelix.commons.util.word;

import lombok.Getter;

public enum WordCase {

    /**
     * Именительный
     */
    NOMINATIVE("есть"),
    /**
     * Родительный
     */
    GENITIVE("нет"),
    /**
     * Дательный
     */
    DATIVE("дать"),
    /**
     * Винительный
     */
    ACCUSATIVE("винить"),
    /**
     * Творительный
     */
    INSTRUMENTAL("творить"),
    /**
     * Предложный
     */
    PREPOSITIONAL("о");

    /**
     * For debug purposes
     */
    @Getter
    private final String question;

    WordCase(String question) {
        this.question = question;
    }
}
