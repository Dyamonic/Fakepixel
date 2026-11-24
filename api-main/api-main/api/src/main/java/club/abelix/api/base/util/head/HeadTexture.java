package club.abelix.api.base.util.head;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * @author - suuft (aka: swiftyclown, Valeriy Shushpanov) ( vk.com/suuft )
 * Created at 04/05/2023 15:40
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor
public enum HeadTexture {

    /**
     * Gift's heads testures sectiion
     */
    GIFT_RED("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvNTcyNmQ5ZDA2MzJlNDBiZGE1YmNmNjU4MzliYTJjYzk4YTg3YmQ2MTljNTNhZGYwMDMxMGQ2ZmM3MWYwNDJiNSJ9fX0="),
    GIFT_YELLOW("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvMTM3NTA2MWQwOGYxZDdiMzE3Njc1YWE3ZmE4ODAwZDZmMjA2NmUwMThkOWY5MWVjZGRmOWNhZjMwNGU5N2U5MiJ9fX0="),
    GIFT_GREEN("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvODFlNDJlMzcyNWMyYjRhZTY5MDA1ODBjNGUyYTZiODMwZjZlY2EwMjExZjdhMzY0MTQzM2ZjNjdmYmM0M2QzZiJ9fX0="),
    GIFT_BLUE("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZDZlMTJmZGFlMWZjZWJhNjg3OWY2NTk3OTYxMzJhN2ZmYTA4Y2Q5MmEyNmNiN2ExMDY3ZDQ5NzAzZDdiMWI0YiJ9fX0="),
    GIFT_GRAYED_PINK("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOGQyOWIwNmM3YzM3Y2JkMmE3NjU5MDgyNzdmYThlYWQwZTRkYzY2YTExM2YzNDdkZTNiYWI5MWZhZGU0NjkxMiJ9fX0="),

    /**
     * Something that can be shown while loading something
     */
    SOMETHING("eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvYmMzODU1MGI1ZGFjOWVmYWZhODg5OTQ1YWVjM2JjZDE3OGNiODgwODA2ZWI3ZTcxMmQxYmQ5NmE2MWRmMjNmNiJ9fX0="),
    ;

    String texture;
}
