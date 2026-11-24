package club.abelix.core.app.data.stream.platform.settings;

import w.util.Env;

public class StreamPlatformSettings {

    /**
     * Ключ для доступа к API ютуба
     */
    public static final String YOUTUBE_API_KEY = Env.getString("YOUTUBE_API_KEY");

    /**
     * Ключ для доступа к API твича
     */
    public static final String TWITCH_CLIENT_ID = Env.getString("TWITCH_CLIENT_ID");
    /**
     * Секретный ключ клиента твича
     */
    public static final String TWITCH_CLIENT_SECRET = Env.getString("TWITCH_CLIENT_SECRET");

}
