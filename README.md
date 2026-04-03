# GeoLog Core R

Biblioteka analizująca logi HTTP zapisane w lokalnym czasie serwera. Program obsługuje problemy związane ze zmianami czasu, takie jak czasy nieistniejące (gdy zegar przeskakuje do przodu) i czasy niejednoznaczne (gdy zegar jest cofany). Biblioteka odrzuca linie błędne składniowo, naprawia czasy nieistniejące, ujednoznacznia sekwencje czasu w miarę możliwości, odrzuca sekwencje niejednoznaczne i wykonuje analizę odwiedzeń na podstawie poprawionych danych.

## Cel zadania

Napisz bibliotekę analizującą logi HTTP zapisane w lokalnym czasie serwera. Część wpisów może mieć błędny albo niejednoznaczny czas z powodu zmiany czasu. Program ma odrzucić linie błędne składniowo, naprawić czasy nieistniejące, w miarę możliwości ujednoznacznić poprawne sekwencje czasu, odrzucić sekwencje niejednoznaczne i dopiero na poprawionych danych wykonać analizę odwiedzeń.

## Problem

Serwer zapisuje tylko lokalny `LocalDateTime`, przez co przy zmianie czasu pojawiają się dwa problemy:

- **Czas nieistniejący**: Lokalna godzina nie występuje, bo zegar przeskakuje do przodu.
- **Czas niejednoznaczny**: Lokalna godzina występuje więcej niż raz; zegar cofany o wartość wynikającą z reguł strefy.

## Schemat rozwiązania

```
GeoLogOptions.yaml -> LogParser -> TimestampRepairService -> GeoLookup: ipwho.is -> AnalyticsService -> AnalysisReport
```

## Wymagania

- Java 11 lub nowsza
- Maven 3.6+
- Dostęp do internetu (dla zapytań do API ipwho.is)

## Instalacja

1. Sklonuj repozytorium:
   ```
   git clone https://github.com/trxvcz/TPO2_KP_S34754.git
   cd TPO2_KP_S34754
   ```

2. Zbuduj projekt za pomocą Maven:
   ```
   mvn clean compile
   ```

## Konfiguracja

Program czyta plik `GeoLogOptions.yaml` z katalogu domowego użytkownika (`user.home`). Plik zawiera:

- `serverZoneId`: Strefa czasowa serwera, np. `Europe/Warsaw`.
- `logLines`: Lista linii logów (opcjonalne; brak oznacza pustą listę).

Przykład pliku `GeoLogOptions.yaml`:

```yaml
serverZoneId: Europe/Warsaw
logLines:
  - "r0001|2024-01-10T12:00:00|8.8.8.8|GET|/login|200|15|1234"
```

## Format linii logu

Pojedyncza linia logu ma format:

```
requestId|serverLocalTime|clientIp|method|endpoint|status|latencyMs|bytes
```

Przykład poprawnej linii:

```
r0001|2024-01-10T12:00:00|8.8.8.8|GET|/login|200|15|1234
```

Znaczenie pól:

- `requestId`: Niepusty identyfikator żądania.
- `serverLocalTime`: Lokalny czas serwera bez strefy, format ISO `yyyy-MM-dd'T'HH:mm:ss`.
- `clientIp`: Poprawny adres IPv4.
- `method`: Niepusta metoda HTTP, np. `GET` lub `POST`.
- `endpoint`: Niepusta ścieżka zasobu, np. `/login`.
- `status`: Kod odpowiedzi HTTP jako liczba całkowita.
- `latencyMs`: Liczba całkowita nieujemna.
- `bytes`: Liczba całkowita nieujemna.

## Implementacje

### 1. LogParser

Klasa odpowiada za walidację i parsowanie jednej linii logu. Jeżeli linia jest niepoprawna (np. null, pusty tekst, zła liczba pól, puste pola obowiązkowe, zły adres IPv4, zła data, błędne wartości liczbowe), metoda zwraca `Optional.empty()` zamiast rzucić wyjątek.

### 2. TimestampRepairService

Pracuje na `LocalDateTime` serwera i przygotowuje sekwencję zapisów czasu w strefie serwera.

Zachowanie:

- Poprawne czasy: Akceptuje bez zmian.
- Czasy nieistniejące: Naprawia przez przesunięcie do przodu o rzeczywistą długość luki.
- Bloki niejednoznacznych czasów: Jeśli da się wskazać jeden punkt "powrotu do przeszłości", rozwiązuje blok; w przeciwnym razie odrzuca cały blok.

### 3. OptionsLoader

Czyta konfigurację z YAML i buduje `GeoTimeOptions`.

- `serverZoneId`: Pole wymagane, nie może być puste.
- `logLines`: Pole opcjonalne; brak oznacza pustą listę.

### 4. IpWhoIsGeoLookup

Łączy się z API `https://ipwho.is/<ip>` przez HTTPS. Z odpowiedzi JSON odczytuje pola: `success`, `country_code`, `timezone.id`.

- Metoda `lookup(String ip)` wykonuje żądanie sieciowe i wywołuje `parseGeoInfo(String json)`.
- `parseGeoInfo(String json)` zwraca `GeoInfo` tylko jeśli odpowiedź jest poprawna i zawiera poprawne `country_code` oraz `timezone.id`; w przeciwnym razie rzuca `GeoLookupException`.

### 5. AnalyticsService

Główna klasa zadania:

- Odrzuca linie błędne składniowo.
- Naprawia i ujednoznacznia sekwencję czasu po stronie serwera.
- Zapisuje, które wpisy zostały naprawione lub odrzucone.
- Dla zaakceptowanych wpisów pobiera `GeoInfo` z `GeoLookup`.
- Przelicza czas serwera na lokalny czas nadawcy.
- Buduje analitykę per kraj i per strefa czasowa nadawcy.

## Jak rozumieć poprawną i niejednoznaczną sekwencję

### Przykład sekwencji umożliwiającej korektę:

```
a 2024-10-27T02:30
b 2024-10-27T02:35
c 2024-10-27T02:20
d 2024-10-27T02:25
```

W tej sekwencji występuje jeden punkt powrotu do wcześniejszego czasu. Blok można rozdzielić na dwa fragmenty i naprawić.

### Przykład sekwencji, której nie należy naprawiać:

```
x 2025-10-26T02:40
y 2025-10-26T02:45
```

Taki blok nie ma jednego punktu cofnięcia. Wpisy należy odrzucić jako niejednoznaczne.

## Raporty

### Raport zbiorczy

Metoda `AnalysisReport.toText()` zwraca tekst w strukturze:

```
SUMMARY
Metric                      Value
--------------------------  -----
Invalid lines               1
Repaired gap times          1
Resolved overlap entries    2
Dropped ambiguous entries   2
GeoLookup failures          0

AMBIGUOUS REQUEST IDS
f
g

COUNTRIES
Code Count
---- -----
CN       2
US       4

TIMEZONES
Timezone                 Count
------------------------ -----
America/Los_Angeles          4
Asia/Shanghai                2

HOURS (sender)
Hour range  Count
----------- -----
10:00-10:59     1
14:00-14:59     1
17:00-17:59     1
18:00-18:59     2
19:00-19:59     1
```

- **SUMMARY**: Liczniki jakości danych i przebiegu analizy.
- **AMBIGUOUS REQUEST IDS**: Identyfikatory wpisów odrzuconych jako niejednoznaczne.
- **COUNTRIES**: Liczba poprawnych żądań per kraj.
- **TIMEZONES**: Liczba poprawnych żądań per strefa czasowa nadawcy.
- **HOURS (sender)**: Globalny histogram godzin nadawcy.

### Raport szczegółowy dla strefy czasowej

Metoda `timezoneHistogram(String timezoneId)` zwraca histogram godzin dla jednej strefy czasowej nadawcy, zawsze w 24 wierszach.

```
TIMEZONE HISTOGRAM
Timezone: America/Los_Angeles

Hour range  Count
----------- -----
00:00-00:59     0
01:00-01:59     0
02:00-02:59     0
...
23:00-23:59     0
```

## Użycie

Uruchom program za pomocą Maven:

```
mvn compile exec:java -Dexec.mainClass=zad1.Main
```

Program wczyta `GeoLogOptions.yaml` z katalogu domowego, przetworzy logi i wyświetli raporty.

## Struktura projektu

- `src/zad1/`: Kod źródłowy.
  - `Main.java`: Główna klasa uruchamiająca program.
  - `LogParser.java`: Parsowanie linii logów.
  - `TimestampRepairService.java`: Naprawa czasów.
  - `OptionsLoader.java`: Ładowanie konfiguracji YAML.
  - `IpWhoIsGeoLookup.java`: Geograficzne lookup IP.
  - `AnalyticsService.java`: Główna logika analizy.
  - `AnalysisReport.java`: Generowanie raportów.
  - Inne klasy pomocnicze: `GeoInfo.java`, `GeoLookup.java`, `GeoLookupException.java`, `GeoTimeOptions.java`, `LogEntry.java`, `ResolutionKind.java`, `ResolvedLogEntry.java`.

- `GeoLogOptions.yaml`: Przykładowy plik konfiguracyjny.
- `pom.xml`: Konfiguracja Maven.

## Przykład

Dla przykładowego `GeoLogOptions.yaml` program przetworzy logi, naprawi czasy, wykona geolookup i wygeneruje raporty jak opisane powyżej.