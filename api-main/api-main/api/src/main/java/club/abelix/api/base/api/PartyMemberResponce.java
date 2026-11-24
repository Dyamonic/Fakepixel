package club.abelix.api.base.api;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class PartyMemberResponce {
    private final List<Integer> members;
    private final int leader;
}
