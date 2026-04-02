/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.ZoneId;
import com.google.gson.Gson;

public class IpWhoIsGeoLookup implements GeoLookup {
  private static final Gson GSON = new Gson();
  private record TimezoneData(String id) {}
  private record IpWhoIsResponse(boolean success, String country_code, TimezoneData timezone) {}
  @Override
  public GeoInfo lookup(String ip) throws GeoLookupException {
    try {
      URI uri = new URI("https://ipwho.is/" + ip);
      String jsonResponse;
      try(var inputStream = uri.toURL().openStream()){
        jsonResponse = new String(inputStream.readAllBytes());
      }
      return parseGeoInfo(jsonResponse);
    } catch (Exception e) {
      throw new GeoLookupException("Błąd podczas pobierania danych geolokalizacyjnych dla IP: " + ip, e);
    }
    
  }

  public GeoInfo parseGeoInfo(String json) throws GeoLookupException {
    if (json == null|| json.isBlank()) {
      throw new GeoLookupException("Odpowiedź z serwera jest pusta");
    }
    try{

    
    IpWhoIsResponse response = GSON.fromJson(json, IpWhoIsResponse.class);
    if (!response.success) {
      throw new GeoLookupException("Nie można znaleźć informacji geolokalizacyjnych dla podanego IP");
    }

    if (response.country_code() == null || response.timezone()==null || response.timezone().id() == null) {
      throw new GeoLookupException("Brak wymaganych danych geolokalizacyjnych w odpowiedzi");
    }

    return new GeoInfo(response.country_code(), ZoneId.of(response.timezone().id()));
    }catch (Exception e) {
      throw new GeoLookupException("Błąd podczas parsowania danych geolokalizacyjnych: " + e.getMessage(), e);
    }  
  }
}
