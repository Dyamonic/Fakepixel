package club.abelix.network.common.locale;

import java.util.List;

public interface UserLocale {

    String getName();

    String getMessage(String path, Object... params);

    List<String> getMessageList(String path, Object... params);

}
