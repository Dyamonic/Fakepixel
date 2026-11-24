package club.abelix.commons.data.rank.entity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldDefaults;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class RankEntity {

    int id;
    String name;
    String displayNameKey;
    int priority;
    String prefix;
    String suffix;
    Set<String> permissions = new HashSet<>();
    Set<RankEntity> inheritedRanks = new HashSet<>();
}