package club.abelix.api.base.box.entity;

import club.abelix.commons.data.cases.rarity.Rarity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class BoxDto {

    int id;
    int ownerId;
    int boxTypeId;
    String serverMode;
    Rarity rarity;
    LocalDateTime obtainedAt;

}
