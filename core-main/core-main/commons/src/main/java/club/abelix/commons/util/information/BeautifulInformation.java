package club.abelix.commons.util.information;

import club.abelix.commons.language.Language;
import lombok.RequiredArgsConstructor;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "rawtypes"})
@Deprecated
@RequiredArgsConstructor
public class BeautifulInformation<Instance> {

    private final String header;

    private final Map<String, BeautifulInformationLoader> dataLoaders
            = new LinkedHashMap<>();

    public <T extends Instance> void addDataLoader(String displayName,
                                                   BeautifulInformationLoader<T> loader) {
        dataLoaders.put(displayName, loader);
    }

    public void printInformation(Collection<Instance> instances,
                                 Language language, StringBuilder builder) {
        builder.append(language.getMessage(header)).append("\n");

        Iterator<Instance> iterator = instances.iterator();

        while (iterator.hasNext()) {
            Instance instance = iterator.next();

            boolean title = true;

            for (Map.Entry<String, BeautifulInformationLoader> entry : dataLoaders.entrySet()) {
                String name = entry.getKey();
                BeautifulInformationLoader loader = entry.getValue();

                if (loader.doLoad(instance.getClass())) {
                    builder.append(language.getMessage(title
                                    ? "information_title_field"
                                    : "information_field",
                            "%key%", language.getMessage(name),
                            "%value%", loader.load(instance)
                    )).append("\n");

                    title = false;
                }
            }

            if (iterator.hasNext()) {
                builder.append("\n");
            }
        }
    }

}
