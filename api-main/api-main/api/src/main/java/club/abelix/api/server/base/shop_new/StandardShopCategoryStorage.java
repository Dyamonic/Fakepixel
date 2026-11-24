package club.abelix.api.server.base.shop_new;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class StandardShopCategoryStorage {

    List<ShopCategory> categories = new LinkedList<>();

    public void add(ShopCategory cat) {
        categories.add(cat);
        log.info("Registered new shop category {}", cat.getClass().getSimpleName());
    }

    public Collection<ShopCategory> getAll() {
        return Collections.unmodifiableCollection(categories);
    }

}
