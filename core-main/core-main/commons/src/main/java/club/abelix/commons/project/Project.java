package club.abelix.commons.project;

import club.abelix.commons.language.Language;
import club.abelix.commons.util.EnvUtility;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Project {

    AGERA_PVP(Namespaces.AGERA_PVP, Language.RUSSIAN),
    FIXMINE(Namespaces.FIXMINE, Language.RUSSIAN),
    FAKEPIXEL(Namespaces.FAKEPIXEL, Language.ENGLISH),
    ABELIX_MC(Namespaces.ABELIX_MC, Language.ENGLISH)
    ;

    public static final Project[] VALUES = values();
    public static final Project CURRENT;

    private static final Map<String, Project> BY_NAMESPACE = new HashMap<>();

    static {
        for (val project : VALUES) {
            BY_NAMESPACE.put(project.namespace, project);
        }

        val namespace = EnvUtility.getEnvStrict("NAMESPACE");

        CURRENT = Objects.requireNonNull(
                BY_NAMESPACE.get(namespace),
                "No project found with namespace " + namespace
        );
    }

    String namespace;
    Language defaultLanguage;
}
