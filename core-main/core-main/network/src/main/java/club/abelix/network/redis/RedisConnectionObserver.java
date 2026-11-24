package club.abelix.network.redis;

public interface RedisConnectionObserver {

    void onConnected();

    void onDisconnected();
}
