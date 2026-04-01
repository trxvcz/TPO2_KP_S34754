/**
 *
 *  @author Koc Paweł s34754
 *
 */

package zad1;


import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.Optional;

public class LogParser {

  public Optional<LogEntry> parseLine(String line) {
    if (line == null || line.isBlank()) {
      return Optional.empty();
    }
    String[] parts = line.split("\\|", -1);
    if (parts.length != 8) {
      return Optional.empty();
    }

    try{
      String requestId = parts[0];
      if (requestId.isEmpty()) {
      return  Optional.empty();
      }

      LocalDateTime serverLocalTime = LocalDateTime.parse(parts[1]);
      String ip = parts[2];
      InetAddress address = InetAddress.getByName(ip);

      String method =  parts[3];
      if (method.isEmpty()) {
        return Optional.empty();
      }

      String endpoint = parts[4];
      if (endpoint.isEmpty()) {
        return Optional.empty();
      }

      String status = parts[5];
      if (status.isEmpty()) {
        return Optional.empty();
      }

      int statusCode = Integer.parseInt(status);

      String latencyMs = parts[6];
      if (latencyMs.isEmpty()) {
        return Optional.empty();
      }

      int latency =  Integer.parseInt(latencyMs);

      String bytes = parts[7];
      if (bytes.isEmpty()) {
        return Optional.empty();
      }

      int bytesMS =  Integer.parseInt(bytes);

      return Optional.of(new LogEntry(requestId,serverLocalTime,ip,method,endpoint,statusCode,latency,bytesMS));

    } catch (Exception e) {
        return  Optional.empty();
    }
  }
}
