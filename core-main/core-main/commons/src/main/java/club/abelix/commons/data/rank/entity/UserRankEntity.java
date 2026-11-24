package club.abelix.commons.data.rank.entity;

import club.abelix.commons.data.rank.type.UserRankType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@AllArgsConstructor
public class UserRankEntity {

    int id;
    int userId;
    RankEntity rank;
    LocalDateTime expires;
    UserRankType type;
}