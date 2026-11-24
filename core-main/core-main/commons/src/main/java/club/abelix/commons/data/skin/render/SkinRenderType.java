package club.abelix.commons.data.skin.render;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SkinRenderType {

    // https://skin.abelixservices.online

    AVATARS("avatars", 5), // MAX SIZE 512
    HEAD("renders/head", 7), // MAX SIZE 10
    BODY("renders/body", 7), // MAX SIZE 10
    RAW_SKINS("skins", 0), // MAX SIZE 0
    RAW_CAPE("cape", 0); // MAX SIZE 0

    private final String url;
    private final int defaultSize;
}
