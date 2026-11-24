package club.abelix.core.app.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodCondition;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.Watch;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
public class PodReadyWatcher {

    private final KubernetesClient client;
    private Watch watch;

    public void watchPodReady(String namespace, String podName, Runnable onReady) {
        AtomicBoolean readySent = new AtomicBoolean(false);

        Pod current = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .get();

        if (current != null && isPodReady(current)) {
            log.info("Pod {}.{} already in READY status", namespace, podName);
            onReady.run();
            return;
        }

        this.watch = client.pods()
                .inNamespace(namespace)
                .withName(podName)
                .watch(new Watcher<>() {
                    @Override
                    public void eventReceived(Action action, Pod pod) {
                        if (pod == null) {
                            return;
                        }

                        if (isPodReady(pod) && readySent.compareAndSet(false, true)) {
                            onReady.run();
                            close();
                        }
                    }

                    @Override
                    public void onClose(WatcherException cause) {
                        if (cause != null) {
                            log.error("Pod watcher closed with error: ", cause);
                        }
                    }
                });
    }

    private boolean isPodReady(Pod pod) {
        if (pod.getStatus() == null || pod.getStatus().getConditions() == null) {
            return false;
        }
        for (PodCondition c : pod.getStatus().getConditions()) {
            if ("Ready".equals(c.getType()) && "True".equalsIgnoreCase(c.getStatus())) {
                return true;
            }
        }
        return false;
    }

    public void close() {
        if (watch != null) {
            watch.close();
            watch = null;
        }
    }
}