package club.abelix.antivpn.data;

/**
 * @author <a href="https://github.com/Neruxov">Neruxov</a>
 */
public record CacheEntry(Boolean verdict, Boolean banned, String asn) {
}
