/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


public interface GeoLookup {
  GeoInfo lookup(String ip) throws GeoLookupException;
}
