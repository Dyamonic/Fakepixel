package ru.yooxa.concurrent;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Async<V> {

    private final List<APICallback<V>> callbacks;

    private final boolean canSync;
    @Getter
    private boolean done;
    private V data;
    private Throwable error;
    public Async() {
        this(true);
    }
    public Async(boolean canSync) {
        this.callbacks = new ArrayList<>();
        this.canSync = canSync;
    }

    public void accept(V data) {
        if (this.done) {
            return;
        }

        synchronized (this) {
            if (this.done) {
                return;
            }
            this.data = data;
            this.done = true;

            for (APICallback<V> callback : this.callbacks) {
                callback.accept(data, null);
            }

            notifyAll();
        }
    }

    public void accept(Throwable error) {
        if (error == null) return;
        if (this.done) return;

        synchronized (this) {
            if (this.done) return;

            this.data = null;
            this.error = error;
            this.done = true;

            for (APICallback<V> callback : this.callbacks) {
                callback.accept(null, error);
            }

            notifyAll();
        }
    }

    public Async<V> async(APICallback<V> callback) {
        synchronized (this) {
            if (this.done) {
                if (this.error != null) {
                    callback.accept(null, this.error);
                } else {
                    callback.accept(this.data, null);
                }
            } else {
                this.callbacks.add(callback);
            }
        }

        return this;
    }

    public V sync() throws Throwable {
        if (!canSync) {
            throw new IllegalStateException("Can't sync here");
        }

        synchronized (this) {
            while (!this.done) {
                try {
                    wait();
                } catch (InterruptedException ex) {
                    throw new IllegalStateException("Wait interrupted!", ex);
                }
            }
        }

        if (this.error != null) {
            throw this.error;
        }

        return this.data;
    }

    public V sync(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
        long millis = unit.toMillis(timeout);
        long start = System.currentTimeMillis();

        synchronized (this) {
            while (!done) {
                long remains = millis - (System.currentTimeMillis() - start);
                if (remains <= 0) {
                    throw new TimeoutException();
                }

                wait(remains);
            }
        }

        return this.data;
    }
}