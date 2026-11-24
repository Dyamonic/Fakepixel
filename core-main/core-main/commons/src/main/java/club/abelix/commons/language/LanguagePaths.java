package club.abelix.commons.language;

import ru.yooxa.json.JElement;
import ru.yooxa.json.JObject;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author WhileInside
 */
public class LanguagePaths {

    private final Map<String, Map<String, String[]>> languages;

    public LanguagePaths(JObject object) {
        this.languages = new HashMap<>();

        Set<Map.Entry<String, JElement>> entries = object.entrySet();

        for (Map.Entry<String, JElement> entry : entries) {
            String languageName = entry.getKey();
            JObject languageData = entry.getValue().getAsObject();

            Set<Map.Entry<String, JElement>> languageDataEntries = languageData.entrySet();
            Map<String, String[]> paths = new HashMap<>(languageDataEntries.size());

            for (Map.Entry<String, JElement> languageDataEntry : languageDataEntries) {
                paths.put(languageDataEntry.getKey(), languageDataEntry.getValue().getAsString().toLowerCase()
                        .split(" "));
            }

            languages.put(languageName, paths);
        }
    }

    public Map<String, String[]> getPaths(Language language) {
        return languages.getOrDefault(language.getShortName(), Collections.emptyMap());
    }

    public Collection<String> getPaths(Language language, Collection<String> flags) {
        List<String> foundPaths = new LinkedList<>();
        Map<String, String[]> paths = getPaths(language);

        main:
        for (Map.Entry<String, String[]> entry : paths.entrySet()) {
            // Путь -> Флаг1, флаг2, флаг3
            // Если Флаг1 или флаг2 или флаг3 есть в flags, то этот путь будет использован

            String[] pathFlags = entry.getValue();

            for (String requiredFlag : pathFlags) {
                if (flags.contains(requiredFlag)) {
                    foundPaths.add(entry.getKey());

                    continue main;
                }
            }
        }

        return foundPaths;
    }
}
