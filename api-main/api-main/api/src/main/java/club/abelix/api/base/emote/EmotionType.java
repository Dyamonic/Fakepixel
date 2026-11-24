package club.abelix.api.base.emote;

import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum EmotionType {

    HEART("§c❤", ":heart:", ":love:", "<3"),
    STAR("§6✭", ":star:"),
    YES("§a✔", ":yes:", ":да:"),
    NO("§c✖", ":no:", ":нет:"),
    ONE_TWO_THREE("§a1§e2§c3", ":123:"),
    SHRUG("§e¯\\_(ツ)_/¯", ":idk:", ":i_dont_know:", ":shrug:"),
    DJ("§9ヽ§5(§d⌐§c■§6_§e■§b)§2ノ§9♬", ":dj:"),
    DAB("§d<§eo§d/", ":dab:"),
    HI("§d( ﾟ◡ﾟ)/", ":hi:", "o/"),
    OMG("§6(§a•§e_§a•§6)", ":omg:"),
    WIZARD("§e(§b'§e-§b'§e)⊃━§c☆ﾟ§d.*･｡ﾟ", ":wizard:"),
    WTF("§a0§6.§ao§c?", ":wtf:"),
    SMILE_OK("§bツ", ":okay:"),
    SMILE_SAD("§b＞§6_§b＜", ":sad:"),
    FACEPALM("§6(§2－‸ლ§6)", ":fp:", ":facepalm:"),
    LOL("§c§lLOL", ":lol:"),
    TABLE_FLIP("§c(╯ ° □ °) ╯ §7┻━┻", ":tableflip:", ":tf:"),
    FLOWER("§a✿", ":flower:", ":цветок:"),
    MUSIC("§d♫", ":music:"),
    MATHS("§a√§e(§aπ+x§e)§a=§c§lL", ":maths:"),
    TYPING("§e✎§6...", ":typing:"),
    THINKING("§6(§a0§6.§ao§c?§6)", ":thinking:"),
    OOF("§c§lOOF", ":oof:"),
    TM("§7™", ":tm:"),
    UWU("§dUwU", ":uwu:"),
    DOG("§6(▽◕ ᴥ ◕▽)", ":dog:"),
    CAT("§6(^˵◕ω◕˵^)", ":cat:"),
    COME_TO_ME("§a(づ ◕‿◕ )づ", ":come:"),
    GG("§6§lGG", ":gg:"),
    F("§c§lF", ":f:");

    // ---------------------------------------------------------

    public static final EmotionType[] VALUES = values();

    private static final Map<String, EmotionType> BY_ALIAS = new HashMap<>();

    static {
        for (EmotionType type : VALUES) {
            for (String alias : type.getAliases()) {
                BY_ALIAS.put(alias.toLowerCase(), type);
            }
        }
    }

    private final String replaceTo;

    // ---------------------------------------------------------
    private final String[] aliases;

    EmotionType(String replaceTo, String... aliases) {
        this.replaceTo = replaceTo;
        this.aliases = aliases;
    }

    public static EmotionType findEmotion(@NonNull String word) {
        return BY_ALIAS.get(word.toLowerCase());
    }

}
