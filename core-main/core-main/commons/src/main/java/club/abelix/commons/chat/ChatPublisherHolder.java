package club.abelix.commons.chat;

public final class ChatPublisherHolder {

    private static ChatPublisher instance;

    private ChatPublisherHolder() {}

    public static void set(ChatPublisher publisher) {
        instance = publisher;
    }

    public static ChatPublisher get() {
        return instance;
    }
}
