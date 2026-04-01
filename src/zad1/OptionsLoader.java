/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OptionsLoader {
  public GeoTimeOptions load(String fileName) throws Exception {
    Yaml yaml = new Yaml();
    try(InputStream inputStream = Files.newInputStream(Paths.get(fileName))) {
      Map<String, Object> data = yaml.load(inputStream);

      if  (data == null) {
        throw new Exception("Plik jest pusty" + fileName);
      }

      String serverZoneId = (String) data.get("serverZoneId");
      if (serverZoneId == null || serverZoneId.isBlank()) {
        throw new IllegalArgumentException("Brak wymaganego pola: serverZoneId");
      }

      List<String> logLines = (List<String>) data.get("logLines");
      if (logLines == null) {
        logLines = new ArrayList<>();
      }

      return new GeoTimeOptions(serverZoneId, logLines);
    }
  }
}
