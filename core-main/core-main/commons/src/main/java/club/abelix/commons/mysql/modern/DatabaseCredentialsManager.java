package club.abelix.commons.mysql.modern;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Data;

import java.util.concurrent.atomic.AtomicInteger;

@Data
public class DatabaseCredentialsManager {

    private final HikariDataSource source;
    private final AtomicInteger usages;

}
