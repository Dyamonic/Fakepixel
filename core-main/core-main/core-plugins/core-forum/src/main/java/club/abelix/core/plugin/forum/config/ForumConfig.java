package club.abelix.core.plugin.forum.config;


import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ForumConfig {
    Boolean enabled;
    String mailFrom;

    ForumSectionConfig forum;
    List<RankSectionConfig> ranks;
}
