# System Rezerwacji Zasobów

Przy opisywaniu poszczególnych elementów programu dodano adnotacje, jakie nazwy mają te elementy w kodzie (`name in
code`).

# Implementacja

## Organizacja Sieci

Cała aplikacja może działać w dwóch trybach:
- węzła głównego (`master host`)
- pod-węzła (`sub-host` lub `slave`)

Tryb działania jest zdefiniowany w zależności od tego, czy została podana brama (`gateway`). Cała sieć może posiadać 
tylko jeden *węzeł główny*, który będzie odpowiedzialny za przechowywanie stanu bieżącego alokacji sieci 
(`network status`). Zadaniem pod-węzłów będzie natomiast przekazywanie informacji pomiędzy klientami i węzłem głównym 
(`pass`).

Węzły będą komunikować się ze sobą w postaci bardzo krótkich sesji połączeniowych (`connection`) niezależnie od 
używanego protokołu warstwy transportu. Dokładniejsze informacje dotyczące wysyłanych danych oraz sposobie są 
opisane w akapitach niżej.

> Używany protokół TCP lub UDP może być zmieniony przez zmianę 
> stałej `USE_UNRELIABLE_CONNECTION` znajdującej się
> w klasie `rscnet.Constants.App`.



#### Ogólny plan działania aplikacji:

1. Odczytaj konfigurację z argumentów programu.
2. (Jeśli jest używana) Uruchom serwis komunikacji UDP (`UnreliableConnectionFactory`).
3. Uruchom komunikację między serwisami (`InternalCommunication`).
4. Uruchom serwis klienta (odpowiedni dla odpowiedniego typu węzła 
`ClientSubHostPortHandler` lub `ClientMasterPortHandler`).
5. Uruchom serwis serwera (uniwersalny dla obu rodzajów węzłów 
`ServerPortHandler`).
6. Oczekuj wezwania do zakończenia aplikacji (`keepAlive`) lub przekroczenia maksymalnego czasu działania aplikacji.
7. Zakończ aplikację przez wezwanie serwisów do zatrzymania pętli. Dołącz wątki.
8. (Jeśli jest używane) Zaczekaj na wejście od użytkownika, który może chcieć obejrzeć log programu.

> Każdy serwis posiada obiekt kontrolny (różne nazwy) oraz swój własny wątek. W czasie działania wykonuje swoje zadania
> w pętlach (`update`). Pętla może wyrzucić wyjątek, który zostanie przechwycony, po to, aby potencjalnie w przyszłym
> obrocie pętli wykonać zadanie prawidłowo. Pętla jest wykonywania aż do śmierci serwisu.
> 
> Serwisy komunikują się pomiędzy sobą za pomocą specjalnego obiektu komunikacji wewnętrznej aplikacji 
> (`InternalCommunication`).

#### Strategia działania serwisu komunikacji UDP:

 (`UnreliableConnectionFactory`)

1. Spróbuj odebrać pakiet. Jeśli nadszedł:
   1. Parsuj pakiet.
   2. Sprawdź ID połączenia (`UnreliableConnection`, `connectionID`).
   3. Sprawdź, czy pakiet chce przesłać informację, która już nadeszła
   przy pomocy ID wiadomości (`messageID`).
   4. Jeśli pakiet nie został potwierdzony, odeślij potwierdzenie.
2. Spróbuj nadać kolejny pakiet z kolejki. Jeśli taki jest:
   1. Sformatuj pakiet.
   2. Sprawdź, czy wiadomość została potwierdzona. Jeśli tak, podnieś ID wiadomości połączenia.
   3. Podejmuj kolejne próby wysłania wiadomości. Po przekroczeniu limitu wyrzuć wyjątek.

#### Strategia działania klienta węzła głównego:

1. Sprawdź, czy należy złożyć sieć. Jeśli tak, utwórz kolejkę zwijania i nadaj sygnały o zwinięciu.

#### Strategia działania klienta pod-węzła:
1. Sprawdź, czy został znaleziony adres węzła głównego.
   1. Jeśli nie ma żadnego adresu, spytaj się bramy. Odpowiedź bramy uznaj za fałszywy węzeł główny.
   2. Rekurencyjnie wypytuj kolejne węzły o węzeł główny, aż ten sam się za taki przedstawi. (patrz: protokół wyższy 
   aplikacji)
2. Sprawdź, czy węzeł jest zarejestrowany. (patrz: protokół wyższy aplikacji)
3. Sprawdź, czy węzeł ma za zadanie przekazać serwerowi alokację lub terminację. Przekaż ją.

#### Strategia działania serwera:
1. Jeśli jest to serwer węzła głównego, obsłuż wezwania rejestracji, alokacji lub terminacji zgodnie z protokołem
wyższym aplikacji.
2. Jeśli jest to serwer pod-węzła, przekaż węzłowi głównemu wezwania do rejestracji, alokacji lub terminacji zgodnie
z protokołem wyższym aplikacji.

## Protokół Wyższy Aplikacji

Protokół wyższy aplikacji jest odpowiedzialny za przekazywanie informacji o stanie wchodzących w skład sieci węzłów.
Należy on czysto do warstwy aplikacji sieci.

Każdy węzeł musi posiadać swoje **unikalne** ID. To ono jest używane do rozróżniania adresatów wiadomości, a nie port.
Jest to podyktowane faktem, że węzeł może posiadać 1 lub 2 porty.

> Gwiazdka * dotyczy tylko pod-węzłów, komunikujących się przy użyciu domyślnego portu komunikacji niezawodnej TCP.

Węzły obsługują 5 rodzajów żądań:
- Wskazanie węzła głównego *
- Rejestrację *
- Alokację
- Terminację (sesji alokacji)
- Złożenie (sieci)

### Wskazanie węzła głównego *

Nowy pod-węzeł sieci zadaje pytanie kolejnym węzłom, które są kandydatami na węzeł główny przy pomocy wezwania o składni
`SHOW_MASTER`. Przewidziane są następujące odpowiedzi:

1. `ASK_HIM <IP> <Port>` informująca o tym, że węzeł nie jest głównym. Posiada on jednak bramę, która może zostać
odpytana o to samo.
2. `IM_MASTER` informująca o tym, że węzeł uważa się za węzeł główny. Posiada on stan sieci, zmodyfikowany
serwis klienta, etc. Nie udziela on jednak żadnej innej informacji o stanie sieci, możliwości rejestracji, ani innych.
3. `UNKNOWN_MASTER` (nieobsługiwane) informująca o tym, że węzeł nie jest węzłem głównym, ale też nie posiada bramy.
Został on niepoprawnie skonfigurowany.

Każdy węzeł jest samodzielnie odpowiedzialny za wypytywanie kolejnych węzłów o adres dostępu do węzła głównego.

### Rejestracja *

Pod-węzeł po odnalezieniu adresu portu węzła głównego prosi o rejestrację. Wezwanie ma następującą składnię:
`REG_SLAVE <HostID> <Port> (<A:0> <B:0> ...)`. Pod-węzeł dzieli się informacją o swoim ID (która jest sprawdzana, przez
węzeł główny) oraz swojej przestrzeni na alokację zasobów.

Węzeł główny może udzielić następujących odpowiedzi: `REG_SALVE_OK` lub `REG_SLAVE_DENY`. Poprawna rejestracja wymaga,
aby:
1. Nie zaistniał konflikt identyfikatorów węzłów (jakichkolwiek).
2. Stan sieci zarejestrował nową przestrzeń na alokację zasobów.

W trakcie rejestracji przekazywany jest tylko port dostępu. Za adres uważa się adres, z którego nadeszło połączenie.

> Od momentu rejestracji komunikacja może odbywać się przy pomocy UDP (zgodnie z protokołem niższym), w zależności od
> konfiguracji.

### Alokacja i terminacja

Alokacja i terminacja są bardzo podobne. Ich składnia i działanie jest zdefiniowana przez założenia projektowe.

Dodatkowo terminacja, po obsłużeniu, ustawia na węźle głównym flagę, że sieć powinna zostać złożona.

Zadaniem pod-węzłów przy alokacji i terminacji jest pełnienie tylko funkcji przekazywania tych wezwań węzłowi głównemu.
Jedyną logiką pod-węzłów jest sprawdzenie składni tych wezwań.

### Złożenie sieci

Złożenie sieci to specjalne wezwanie pochodzące od węzła głównego do pod-węzłów, informujące, że wszystkie procesy
powinny się zakończyć po obsłużeniu obecnie obsługiwanego wezwania, bądź wcześniej. Składnia: `COLLAPSE`. Węzeł główny
nie spodziewa się żadnej odpowiedzi.

Proces węzła głównego kończy się po wysłaniu tego żądania do wszystkich pod-węzłów.

> Log protokołu wyższego zaczyna się oznaczeniem serwera, klienta, lub klienta głównego w nawiasie kwadratowym oraz
> kierunkiem przekazu np. `[Server] * ->` *wiadomość wychodząca z serwera*.

## Protokół Niższy Aplikacji

Protokół niższy aplikacji jest odpowiedzialny za sprawną komunikację przy pomocy protokołu UDP, czyli:
1. Organizowanie połączeń między różnymi węzłami.
2. Potwierdzenie, że wiadomości dotarły do adresata.

Pakiet przekazany tym protokołem ma następującą składnię:

`<ConnectionID> :::: <MessageID> :::: <AttemptNo> :::: <MessageMode> :::: <Meta1> :::: <Meta2> :::: <Line0> (:: <Line1> 
::<LineN>) :::: <IgnoredData>`

- Numer połączenia '*ConnectionID*' - Pozwala rozróżnić różne przychodzące i wychodzące połączenia.
- Numer wiadomości '*MessageID*' - Pozwala rozróżnić poszczególne wiadomości. (W przypadku, gdyby 'zagubiony' pakiet
nadszedł po następnej przychodzącej wiadomości).
- Numer próby '*AttemptNo*' - Ignorowany przez adresata. (Przydatny przy debugowaniu aplikacji)
- Tryb pakietu '*AttemptNo*'
- Informacje dodatkowe '*Meta1*' oraz '*Meta2*' - Ignorowane przez adresata. (Przydatne przy debugowaniu aplikacji)
- Kolejne linie wiadomości '*LineN*' - Zawartość przekazywanej wiadomości.
- Informacje ignorowane '*IgnoredData*'

> Protokół niższy aplikacji wykorzystuje `::` do kodowania znaku nowej linii `\n`. Protokół wyższy korzysta z `:::`.

Przewidziane są 2 tryby pakietów:
- `Message` do przekazania zawartości wiadomości
- `Acknowledgement` do potwierdzenia, że przynajmniej jedna próba dotarła do adresata.

> Log protokołu niższego zaczyna się symbolami `<UC>` *unreliable communication*.

## Komunikacja wewnątrz aplikacji

Aplikacja korzysta z 2 do 5 wątków, które komunikują się przy pomocy klasy pomocniczej, pełniącej funkcję jedynie
agregacyjną. W środku znajdują się 'przekazanie stanu' (`InternalPass` *application internal communication value 
pass*). Ich zadaniami jest:
1. Zapewnienie bezpieczeństwa związanego z wielowątkowym dostępem do obiektu.
3. Zapewnienie możliwości zresetowania stanu po odczytaniu.
2. Boxing typów prostych. Możliwość nadania pseudowartości `null`.

Serwisy komunikujące się tymi obiektami blokują wątki przy pomocy pętli usypiających. Blokady te mają też za zadanie
sprawdzanie, czy serwisy, które korzystają z tych blokad, są żywe. Aby aplikacja bezpiecznie kończyła działanie,
blokada sama się zwalnia, kiedy odkryje, że flaga śmierci serwisu się zmieni. Jednak to serwis jest odpowiedzialny
za przekazanie blokadzie wyrażenia lambda do sprawdzenia tej flagi lub spełnienia oczekiwanego przez tę blokadę 
działania.

# Informacje dodatkowe

Przedmiot: SKJ 2021/22 sem.1 (Projekt spełnia wszystkie założenia, włącznie z dodatkowymi.)
Autor: Mateusz Karbowiak - Edytory: InteliJ, Git

