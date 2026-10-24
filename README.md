# Generatory – plugin do Minecrafta (Paper 1.21+)

Odnawiające się generatory bloków, np. pod SkyPvP. Zaznaczasz obszar różdżką, tworzysz generator jedną komendą, a bloki, proporcje i ceny ustawiasz w GUI.

## Co potrafi

- **Różdżka generatorów**: LPM zaznacza pierwszą pozycję, PPM drugą.
- **Podgląd z cząsteczek**: obrys zaznaczenia (różowy) i istniejących generatorów (zielony), gdy trzymasz różdżkę.
- **Tworzenie jedną komendą**: `/generator utworz <nazwa>`.
- **GUI** (`/generator`): lista generatorów, edycja, różdżka, odnawianie i czas odnawiania.
- **Bloki i proporcje**: dowolne bloki z wagami, np. 70% kamienia i 30% terakoty. Blok dodajesz kliknięciem w swoim ekwipunku.
- **Cennik**: pieniądze za wykopanie bloku (przez Vault), doświadczenie i włączanie/wyłączanie wypadania przedmiotu.
- **Licznik na górze ekranu**: „Wszystkie generatory zostaną odnowione za 1m 26s”.
- **Bez lagów**: odnawianie jest rozłożone na kilka ticków. Gracze stojący w generatorze są przenoszeni na jego górę.
- **Zbieranie do ekwipunku**: opcjonalnie, osobno dla każdego generatora.
- **Ochrona przed nabijaniem pieniędzy**: w generatorach nie można stawiać bloków (można to wyłączyć).

## Instalacja

1. Pobierz `Generatory-1.0.0.jar`: zakładka **Actions** → najnowszy przebieg **Build** → artefakt **Generatory**.
2. Wrzuć plik do folderu `plugins/` na serwerze **Paper 1.21+** (albo Purpur lub innym forku Papera).
3. Zrestartuj serwer.
4. Opcjonalnie: jeśli ceny mają wypłacać pieniądze, zainstaluj **Vault** i plugin ekonomii, np. **EssentialsX**.

## Jak zrobić generator

1. `/generator rozdzka`: dostajesz różdżkę.
2. Kliknij **LPM** jeden róg i **PPM** drugi róg obszaru. Zobaczysz obrys z cząsteczek.
3. `/generator utworz czerwone`: generator powstaje i otwiera się jego edycja.
4. **Bloki i proporcje**: kliknij blok w swoim ekwipunku, żeby go dodać. LPM na bloku ustawia proporcję, SHIFT + LPM zamienia blok, PPM usuwa blok.
5. **Cennik**: kliknij blok i ustaw cenę, doświadczenie oraz wypadanie przedmiotu.
6. Gotowe. Generator odnowi się sam po upływie czasu albo od razu przyciskiem **Odnów teraz**.

## Komendy

Wszystkie wymagają uprawnienia `generatory.admin` (domyślnie OP). Aliasy: `/gen`, `/generatory`.

| Komenda | Opis |
|---|---|
| `/generator` | otwiera menu |
| `/generator rozdzka` | daje różdżkę |
| `/generator utworz <nazwa>` | tworzy generator z zaznaczenia |
| `/generator usun <nazwa>` | usuwa generator |
| `/generator edytuj <nazwa>` | otwiera edycję generatora |
| `/generator lista` | lista generatorów na czacie |
| `/generator odnow [nazwa]` | odnawia jeden generator albo wszystkie |
| `/generator tp <nazwa>` | teleport na górę generatora |
| `/generator przeladuj` | przeładowuje `config.yml` i generatory |

## Uprawnienia

| Uprawnienie | Opis | Domyślnie |
|---|---|---|
| `generatory.admin` | komendy, GUI i różdżka | OP |
| `generatory.stawianie` | stawianie bloków w generatorach | OP |

## Konfiguracja

W pliku `plugins/Generatory/config.yml` ustawisz między innymi:

- czas odnawiania (można go też zmienić w GUI);
- tryb odnawiania: cały generator albo tylko wykopane miejsca;
- wygląd i widoczność paska na górze ekranu;
- kolory podglądu;
- wszystkie wiadomości (format [MiniMessage](https://docs.advntr.dev/minimessage/format.html)).

Generatory zapisują się w `plugins/Generatory/generators.yml`.

## Budowanie ze źródeł

Potrzebna jest Java 21:

```bash
./gradlew build
```

Gotowy plik znajdziesz w `build/libs/Generatory-1.0.0.jar`.
