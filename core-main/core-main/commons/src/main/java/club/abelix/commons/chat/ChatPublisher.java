package club.abelix.commons.chat;

public interface ChatPublisher {

    void publish(String type, String playerName, String server, String message);
}
