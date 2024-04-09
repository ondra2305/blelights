# EMOS BLE lights
## Popis projektu
Toto je repozitář zachycující mojí snahu zprovoznit Android aplikaci pro nějaká Bluetooth LE světýlka co už mám pár let moderním androidu.
[Oficiální aplikace](https://play.google.com/store/apps/details?id=com.tuner168.zhendaemos) nefunguje od verze Android 10 nebo 11 výše.
(Zatím) se mi nepovedlo rozluštit celou logiku původní aplikace, moje aplikace pouze zasílá zachycené Bluetooth packety vytvořené oficiální aplikací na
testovacím zařízení se starým Androidem.

## Reverse engineering staré oficiální aplikace
Bluetooth packety původní aplikace jsem zachytil pomocí rooted Android telefonu a [Wiresharku](https://www.wireshark.org/), poté jsem objevil že komunikace je kompletně nezabezpečná
a uložené packety lze jednoduše vzít a pomocí aplikace jako [nRF Connect](https://play.google.com/store/apps/details?id=no.nordicsemi.android.mcp) zaslat na 
zařízení. Potom jsem se rozhodl udělat si nějakou základní Android aplikaci, aby bylo ovládání světýlek pohodlnější. Myslím si že je možné aplikaci kompletně reimplementovat, povedlo
se mi původní aplikaci "dekompilovat", ale to byl už příliš velký projekt, na který nebyl čas...

## O aplikaci
Aplikaci jsem vytvořil v Android Studiu 2023.1 v Kotlinu a funguje Androidu 13 a 14 (pravděpodobně i na starších verzích, neměl jsem možnost pořádně otestovat).
Jde jen o hobby projekt když jsem se nudil o vánočních prázdninách :-)...

## TOTO (pravděpodobně se k tomu nikdy nedostanu)
* Automatické hledání zařízení pomocí MAC adresy. (44:A6:E6:XX:XX:XX)
* Všechna (většina) původní funkčnosti aplikace (prozatím funguje pouze ON/OFF).
* Hezčí vzhled s moderním Android 14 Material You
* Implementace původní logiky místo jen zasílání zachycených zpráv
* Integrace s [HomeAssistantem](https://www.home-assistant.io/)

Tento projekt není nijak podporován firmou EMOS a jejich jméno tady používám jen protože se jedná o zařízení této firmy.
Jedná se jen o hobby projekt, který vznikl protože se mi nechtělo vyhazovat perfektně funkční zařázení jen proto, že už není podporované výrobcem.
