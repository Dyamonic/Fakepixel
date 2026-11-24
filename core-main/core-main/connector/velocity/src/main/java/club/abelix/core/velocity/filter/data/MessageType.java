package club.abelix.core.velocity.filter.data;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {

    WORDS(1d),
    SINGLE(1d),
    SYMBOLIC(1.8d);

    private final double common;
}