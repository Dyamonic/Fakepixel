package club.abelix.commons.data.currency;

public enum Currency {

    RUB("₽"), USD("$"), EUR("€");

    private static final Currency[] VALUES = values();

    private final String sign;

    Currency(String sign) {
        this.sign = sign;
    }

    public static Currency getById(int id) {
        if (id >= VALUES.length || id < 0) {
            return null;
        }

        return VALUES[id];
    }

    public static Currency getByName(String name) {
        if (name != null && !name.isEmpty()) {
            for (Currency currency : VALUES) {
                if (currency.name().equals(name)) {
                    return currency;
                }
            }
        }

        return null;
    }


    public String getSign() {
        return sign;
    }

}
