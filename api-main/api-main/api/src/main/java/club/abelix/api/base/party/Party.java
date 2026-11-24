package club.abelix.api.base.party;

import club.abelix.api.base.gamer.Gamer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.ToString;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@ToString(of = "id")
@EqualsAndHashCode(of = "id")
public class Party {

    private final int id;
    private final boolean isPrivate;
    private final Set<Gamer> members = new LinkedHashSet<>();
    private Gamer leader;

    public Party(int id, @NonNull Gamer member, boolean isLeader, boolean isPrivate) {
        this.id = id;
        this.members.add(member);
        this.isPrivate = isPrivate;

        if (isLeader) {
            this.leader = member;
        }
    }

    public void addMember(@NonNull Gamer gamer) {
        members.add(gamer);
    }

    public void removeMember(@NonNull Gamer gamer) {
        if (leader == gamer) {
            leader = null;
        }
        this.members.remove(gamer);
    }

    public Set<Gamer> getMembers() {
        return Collections.unmodifiableSet(members);
    }

    public boolean isLeader(@NonNull Gamer gamer) {
        return this.leader == gamer;
    }

    public int getMemberCount() {
        return members.size();
    }

    public boolean hasPartyMember(@NonNull Gamer gamer) {
        return members.contains(gamer);
    }

    public void sendMessage(@NonNull String key, Object... objects) {
        for (Gamer gamer : members) {
            gamer.sendLocalizedMessage(key, objects);
        }
    }

}
