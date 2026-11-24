package club.abelix.api.nms.team;

import club.abelix.api.nms.packet.IVisibleObject;
import club.abelix.api.nms.team.rule.CollisionRule;
import club.abelix.api.nms.team.rule.FakeTeamFlag;
import club.abelix.api.nms.team.rule.TagVisibility;
import club.abelix.api.nms.team.rule.TeamFlag;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import lombok.NonNull;
import org.bukkit.ChatColor;

import java.util.List;

public interface FakeTeam extends IVisibleObject {

    String getName();

    String getPrefix();

    String getSuffix();

    ChatColor getColor();

    default int getColorIndex() {
        return getColor().ordinal();
    }

    TagVisibility getTagVisibility();

    CollisionRule getCollisionRule();

    List<String> getPlayers();

    void addPlayer(@NonNull String playerName);

    void addPlayers(@NonNull List<String> playerNames);

    void removePlayer(@NonNull String playerName);

    void setPrefix(@NonNull String prefix);

    void setSuffix(@NonNull String suffix);

    void setTagVisibility(@NonNull TagVisibility tagVisibility);

    void setCollisionRule(@NonNull CollisionRule collisionRule);

    void setColor(@NonNull ChatColor color);

    boolean setFlag(@NonNull TeamFlag flag);

    boolean unsetFlag(@NonNull TeamFlag flag);

    boolean hasFlag(@NonNull TeamFlag flag);

    boolean setFlag(@NonNull FakeTeamFlag flag);

    boolean unsetFlag(@NonNull FakeTeamFlag flag);

    boolean hasFlag(@NonNull FakeTeamFlag flag);

    void update();

    void setFriendlyFire(boolean b);

    void setSeeInvisible(boolean b);

    boolean isSeeInvisible();

    boolean isFriendlyFire();

    byte getFakeTeamFlags();

    byte getMinecraftTeamFlags();

    WrappedChatComponent getPrefixComponent();

    WrappedChatComponent getSuffixComponent();
}
