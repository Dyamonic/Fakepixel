package ru.yooxa.http;

import club.abelix.commons.util.object.Pair;
import lombok.Getter;
import ru.yooxa.http.netty.NettyHttpClient;

import java.net.InetAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class HttpPool {

    public static final Set<String> KEEP_ALIVE_HOSTS = new HashSet<>();
    private static final Map<Pair<InetAddress, InetAddress>, Queue<HttpPool>> OPEN_CONNECTIONS = new ConcurrentHashMap<>();
    //region Settings
    public static volatile boolean KEEP_ALIVE_ENABLED = true;
    public static long KEEP_ALIVE = 60_000L;
    public static long MAX_REQUESTS_FOR_CONNECTION = 1000;
    public static int MIN_FREE_CONNECTIONS = Integer.parseInt(System.getProperty("pool.min_free_connections", "10"));
    private static int NUMBER = 0;
    //endregion
    public final InetAddress local;
    private final int number;
    private final String host;
    private final InetAddress dest;
    private final HttpClient connection;
    private final AtomicBoolean alive;
    @Getter
    private final boolean keepAlive;
    private volatile long lastRequest;
    private int requests = 0;

    public HttpPool(int number, InetAddress local, String host, boolean ssl, InetAddress dest, boolean keepAlive) {
        this.number = number;
        this.local = local;
        this.host = host;
        this.dest = dest;
        this.keepAlive = keepAlive;
        this.connection = new NettyHttpClient(host, ssl, dest);

        this.connection.setLocalAddress(local);
        this.alive = new AtomicBoolean(true);
    }

    public static HttpPool getConnection(InetAddress local, String host, boolean ssl, InetAddress dest, boolean priority) {
        boolean keepAlive = KEEP_ALIVE_ENABLED && KEEP_ALIVE_HOSTS.contains(host);

        if (keepAlive) {
            Pair<InetAddress, InetAddress> key = new Pair<>(local, dest);

            Queue<HttpPool> connections = OPEN_CONNECTIONS.computeIfAbsent(key, k -> new ConcurrentLinkedQueue<>());


            HttpPool connection = null;
            while ((priority || connections.size() >= MIN_FREE_CONNECTIONS) && (connection = connections.poll()) != null) {
                if (connection.isAlive()) {
                    break;
                }
            }

            if (connection != null && connection.isAlive()) {
                return connection;
            }
        }

        return new HttpPool(++NUMBER, local, host, ssl, dest, keepAlive);
    }

    public HttpClient get() {
        this.lastRequest = System.currentTimeMillis();
        this.requests++;
        return this.connection;
    }

    private boolean isAlive() {
        if (!this.alive.get()) {
            return false;
        }

        if (this.connection.isClosed()) {
            return false;
        }

        if (System.currentTimeMillis() - this.lastRequest < KEEP_ALIVE) {
            return true;
        }

        this.close();
        return false;
    }

    public void free() {
        if (!isAlive()) {
            return;
        }

        if (this.keepAlive && this.requests < MAX_REQUESTS_FOR_CONNECTION) {
            OPEN_CONNECTIONS.get(new Pair<>(this.local, this.dest)).add(this);
        } else {
            this.close();
        }
    }

    public void close() {
        if (!this.alive.getAndSet(false)) return;

        this.connection.close();

//		Log.debug(this + " | Closed after " + this.requests + " requests | " + reason);
    }

    @Override
    public String toString() {
        return this.local.getHostAddress() + "->" + this.host + "[" + this.dest.getHostAddress() + "] №" + this.number;
    }
}
