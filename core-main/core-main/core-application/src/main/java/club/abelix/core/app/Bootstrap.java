package club.abelix.core.app;

import io.netty.util.ResourceLeakDetector;
import lombok.SneakyThrows;
import w.util.Env;

import java.security.Security;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Bootstrap {

    @SneakyThrows
    public static void main(String[] args) {
        Logger.getLogger("org.yaml.snakeyaml").setLevel(Level.OFF);

        System.setProperty("java.awt.headless", "true");
        System.setProperty("library.jansi.version", "AbelixCore");

        System.setProperty("io.netty.selectorAutoRebuildThreshold", "0");
        if (System.getProperty("io.netty.leakDetectionLevel") == null &&
                System.getProperty("io.netty.leakDetection.level") == null) {
            ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.DISABLED);
        }

        // Оптимизации сети
        Security.setProperty("networkaddress.cache.ttl", "30");
        Security.setProperty("networkaddress.cache.negative.ttl", "5");
        //====

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv6Addresses", "false");
        System.setProperty("io.netty.eventLoopThreads", Env.getString("CORE_NETWORK_THREADS", "24"));

        // Ешё немного настроек netty
        System.setProperty("io.netty.allocator.type", "pooled");
        System.setProperty("io.netty.noPreferDirect", "false");
        System.setProperty("io.netty.selectorAutoRebuildThreshold", "512");

        System.setProperty("io.netty.processId", Integer.toString(new Random().nextInt(65535)));
        System.setProperty("io.netty.serviceThreadPrefix", "Netty ");

        CoreApplication core = new CoreApplication();

        Runtime.getRuntime().addShutdownHook(
                new Thread(core::shutdown, "Abelix Core Hook Shutdown")
        );

        core.launch();
    }
}