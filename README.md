# Yarn Calculator / Garn-Rechner

[🇬🇧 English](#-english) | [🇩🇪 Deutsch](#-deutsch)

Knitted or crocheted something from several different yarns? Yarn Calculator tells you what the finished piece is made of overall, for example for a care label.

> Example: 150 g sock yarn (75 % virgin wool, 25 % polyamide) + 25 g mohair silk (70 % mohair, 30 % silk) + 40 g cotton
> → **52.3 % virgin wool, 18.6 % cotton, 17.5 % polyamide, 8.1 % mohair, 3.5 % silk**

---

<a name="-english"></a>
## 🇬🇧 English

### Contents
1. [Installation](#installation)
2. [Upgrading from the old version (GarnRechnerProzedural)](#upgrading-from-the-old-version)
3. [How to use it](#how-to-use-it)
4. [Your data and backups](#your-data-and-backups)
5. [Troubleshooting](#troubleshooting)

### Installation

**1. Install Java (once).** You need a **JDK version 17 or newer**. A plain "JRE" is not enough.
Download the free *Eclipse Temurin JDK* from **[adoptium.net](https://adoptium.net/)** (the "LTS" version is fine).
On Windows, keep the **"Add to PATH"** option enabled in the installer.

**2. Download Yarn Calculator.** Put these files into a folder of your choice, e.g. `Documents\YarnCalculator`:

| File | What it is |
|---|---|
| `YarnCalculator.java` | the program |
| `start.bat` | starter for Windows |
| `start.sh` | starter for Linux / macOS |

**3. Start it.**

- **Windows:** double-click `start.bat`.
  For a desktop icon, right-click `start.bat` → *Show more options* (Windows 11) → *Send to* → *Desktop (create shortcut)*.
- **macOS / Linux:** open a terminal in the folder and run `./start.sh`.
  On macOS you can rename `start.sh` to `start.command` to start it with a double-click.
- **Any system, by hand:** `java YarnCalculator.java`

There's nothing to compile. Java runs the file directly, so the first start can take a few seconds.

### Upgrading from the old version

The old version was called `GarnRechnerProzedural.java` and stored its data in `yarns.json` and `fibers.json`.

1. Copy `YarnCalculator.java` and `start.bat` (or `start.sh`) **into the same folder as before**, where `yarns.json` and `fibers.json` are.
2. Start the program with the **new** `start.bat`. If you have a desktop shortcut to the old `start.bat`, replace that file with the new one.
3. On the first start, your saved yarns and fiber names are **imported automatically**. A green box in the welcome guide confirms this.

Your old files are **not changed or deleted**, so you can always go back.
If the new version was started somewhere else and found nothing, use **File › Import…** and select your old `yarns.json`. The `fibers.json` next to it is imported too.

You can delete the old `GarnRechnerProzedural.java` and `*.class` files afterwards.

### How to use it

The program has a built-in guide (**Help › Guide**, or **F1**) that opens automatically on the first start. In short:

1. **Add a yarn**: one entry for every yarn you used. Use **+ Add yarn**, or **From library…** for a saved yarn.
2. **Enter the weight** you used, in grams.
   *Tip:* weigh the leftover balls and subtract them from the full ball weight.
3. **Enter the fibers** as printed on the ball band, e.g. *Wool 75* and *Polyamide 25*. Fiber names are suggested while you type.
4. **Read the result** on the right. It updates as you type.
   - **Copy for Excel** copies the table, which you can paste into Excel, LibreOffice or Google Sheets.
   - **Copy label text** copies a line like `75% Wool, 25% Polyamide`.
   - **Round to** switches between whole percent and one decimal place.

**Colors:** each yarn shows a status line.

- 🟢 **Green:** the yarn is complete.
- 🟠 **Orange:** something is missing, e.g. the shares only add up to 90 %. A **Fill up** button enters the missing share for you.
- 🔴 **Red:** an entry is invalid, e.g. a letter in a number field.

The result appears as soon as all yarns are green. Both `12,5` and `12.5` work as decimal input.

**Yarn library:** click **Save to library** on a yarn to reuse it later. The line under the name shows whether the yarn is *saved in library* or *differs from library*. If it differs, **Update library** saves your change.
You can rename, edit, duplicate or delete saved yarns under **Library › Manage yarns**.

**Fiber names:** the list only provides suggestions, and you can type any name. New names are added automatically when you save a yarn to the library. Manage the list under **Library › Manage fibers**.
For textile labels in the EU, only the fiber names in Annex I of [Regulation (EU) No 1007/2011](https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A02011R1007-20180215) may be used.

**Rounding:** percentages are rounded with the *largest remainder method* (Hare-Niemeyer), so they always add up to exactly 100 %.

**More:**

- **View › Language** switches between English and German.
- **View › Text size** makes everything bigger.
- **Keyboard shortcuts:** <kbd>Ctrl</kbd>+<kbd>N</kbd> adds a yarn, <kbd>Ctrl</kbd>+<kbd>L</kbd> adds a yarn from the library, and <kbd>F1</kbd> opens the guide. On a Mac, use <kbd>Cmd</kbd> instead of <kbd>Ctrl</kbd>.

### Your data and backups

Everything is **saved automatically**: your yarn library, your fiber list, your settings, and the project you're working on, which is still there the next time you start the program.

All of it is stored in a single file, `data.json`, in your personal data folder:

| System | Folder |
|---|---|
| Windows | `%APPDATA%\YarnCalculator` |
| macOS | `~/Library/Application Support/YarnCalculator` |
| Linux | `~/.local/share/yarncalculator` |

**File › Open data folder** takes you there.

- **File › Export backup…** saves everything into one file, e.g. for a USB stick or a new computer.
- **File › Import…** adds yarns from a backup or from the old `yarns.json`/`fibers.json`. Nothing is overwritten: if a yarn with the same name but a different composition already exists, the imported one gets a number, e.g. `Sock yarn (2)`.
- `data.json.bak` in the data folder is a copy of the state from the last time the program started.
- If `data.json` is ever damaged, the program restores the `.bak` copy automatically and keeps the damaged file for inspection.

<details>
<summary>File format (for the curious)</summary>

```json
{
  "format": "yarncalculator",
  "version": 2,
  "settings": { "language": "en", "textSize": 100, "decimals": 1, "guideSeen": true },
  "fibers": ["Polyamide", "Virgin wool"],
  "yarns": [
    {
      "name": "Classic sock yarn",
      "composition": [
        { "fiber": "Virgin wool", "percent": 75 },
        { "fiber": "Polyamide", "percent": 25 }
      ]
    }
  ],
  "project": { "yarns": [ { "name": "Classic sock yarn", "grams": "150", "composition": [ … ] } ] }
}
```

`version` lets future versions migrate the file safely. Values in `project` are stored exactly as typed, so half-finished input survives a restart.
To keep the data somewhere else (e.g. a USB stick), start with `java -Dyarncalc.dir=PATH YarnCalculator.java`.
</details>

### Troubleshooting

- **Nothing happens when I double-click `start.bat`.** Open a command prompt in the folder and run `java YarnCalculator.java` to see the error message. Most often:
  - `'java' is not recognized`: Java isn't installed or isn't on the PATH. Reinstall it with the "Add to PATH" option.
  - Many lines like `error: ... not supported in -source 11`: your Java is too old. Install version 17 or newer.
  - `error: ... jdk.compiler` / compiler not found: you installed a JRE. Install a **JDK**.
- **The text is too small.** Use **View › Text size**.
- **My old yarns are missing.** Use **File › Import…** and select the old `yarns.json`.

---

<a name="-deutsch"></a>
## 🇩🇪 Deutsch

### Inhalt
1. [Installation](#installation-1)
2. [Umstieg von der alten Version (GarnRechnerProzedural)](#umstieg-von-der-alten-version)
3. [Bedienung](#bedienung)
4. [Deine Daten und Sicherungen](#deine-daten-und-sicherungen)
5. [Probleme lösen](#probleme-lösen)

### Installation

**1. Java installieren (einmalig).** Du brauchst ein **JDK ab Version 17**. Ein reines „JRE“ reicht nicht.
Lade das kostenlose *Eclipse Temurin JDK* von **[adoptium.net](https://adoptium.net/)** herunter (die „LTS“-Version passt).
Lass unter Windows im Installer die Option **„Add to PATH“** eingeschaltet.

**2. Garn-Rechner herunterladen.** Lege diese Dateien in einen Ordner deiner Wahl, z. B. `Dokumente\GarnRechner`:

| Datei | Was ist das? |
|---|---|
| `YarnCalculator.java` | das Programm |
| `start.bat` | Starter für Windows |
| `start.sh` | Starter für Linux / macOS |

**3. Starten.**

- **Windows:** Doppelklick auf `start.bat`.
  Für ein Desktop-Symbol: Rechtsklick auf `start.bat` → *Weitere Optionen anzeigen* (Windows 11) → *Senden an* → *Desktop (Verknüpfung erstellen)*.
- **macOS / Linux:** Terminal im Ordner öffnen und `./start.sh` ausführen.
  Unter macOS kannst du `start.sh` in `start.command` umbenennen, dann startet es per Doppelklick.
- **Überall von Hand:** `java YarnCalculator.java`

Du musst nichts kompilieren. Java führt die Datei direkt aus, deshalb kann der erste Start ein paar Sekunden dauern.

### Umstieg von der alten Version

Die alte Version hieß `GarnRechnerProzedural.java` und speicherte ihre Daten in `yarns.json` und `fibers.json`.

1. Kopiere `YarnCalculator.java` und `start.bat` (bzw. `start.sh`) **in denselben Ordner wie bisher**, also dorthin, wo `yarns.json` und `fibers.json` liegen.
2. Starte das Programm mit der **neuen** `start.bat`. Wenn deine Desktop-Verknüpfung auf die alte `start.bat` zeigt, ersetze diese Datei einfach durch die neue.
3. Beim ersten Start werden deine gespeicherten Garne und Fasernamen **automatisch übernommen**. Ein grüner Kasten in der Begrüßungs-Anleitung bestätigt das.

Deine alten Dateien werden **nicht verändert oder gelöscht**, du kannst also jederzeit zurück.
Falls die neue Version woanders gestartet wurde und nichts gefunden hat: **Datei › Importieren…** wählen und die alte `yarns.json` auswählen. Die `fibers.json` daneben wird automatisch mit übernommen.

Die alte `GarnRechnerProzedural.java` und die `*.class`-Dateien kannst du danach löschen.

### Bedienung

Das Programm hat eine eingebaute Anleitung (**Hilfe › Anleitung** oder **F1**), die sich beim ersten Start automatisch öffnet. Kurz gefasst:

1. **Garn hinzufügen:** ein Eintrag für jedes verwendete Garn. Nutze **+ Garn hinzufügen** oder **Aus der Bibliothek…** für ein gespeichertes Garn.
2. **Gewicht eintragen:** wie viel Gramm du davon verbraucht hast.
   *Tipp:* Restknäuel wiegen und vom vollen Knäuelgewicht abziehen.
3. **Fasern eintragen:** so wie sie auf der Banderole stehen, z. B. *Schurwolle 75* und *Polyamid 25*. Fasernamen werden beim Tippen vorgeschlagen.
4. **Ergebnis ablesen:** rechts, es aktualisiert sich beim Tippen.
   - **Für Excel kopieren** kopiert die Tabelle zum Einfügen in Excel, LibreOffice oder Google Sheets.
   - **Etikett-Text kopieren** kopiert eine Zeile wie `75 % Schurwolle, 25 % Polyamid`.
   - **Runden auf** wechselt zwischen ganzen Prozent und einer Nachkommastelle.

**Farben:** Jedes Garn zeigt eine Statuszeile.

- 🟢 **Grün:** Das Garn ist vollständig.
- 🟠 **Orange:** Es fehlt noch etwas, z. B. ergeben die Anteile erst 90 %. Der Knopf **Auffüllen** trägt den fehlenden Anteil ein.
- 🔴 **Rot:** Eine Eingabe ist ungültig, z. B. ein Buchstabe in einem Zahlenfeld.

Das Ergebnis erscheint, sobald alle Garne grün sind. `12,5` und `12.5` funktionieren beide.

**Garn-Bibliothek:** Klicke bei einem Garn auf **In Bibliothek speichern**, um es später wiederzuverwenden. Unter dem Namen siehst du, ob das Garn *in der Bibliothek gespeichert* ist oder *von der Bibliothek abweicht*. Weicht es ab, speichert **Bibliothek aktualisieren** deine Änderung.
Unter **Bibliothek › Garne verwalten** kannst du Garne umbenennen, bearbeiten, duplizieren und löschen.

**Fasernamen:** Die Liste macht nur Vorschläge, du kannst jeden Namen eintippen. Neue Namen landen automatisch in der Liste, wenn du ein Garn in der Bibliothek speicherst. Die Liste bearbeitest du unter **Bibliothek › Fasern verwalten**.
Für Textiletiketten in der EU sind nur die Fasernamen aus Anhang I der [Verordnung (EU) Nr. 1007/2011](https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A02011R1007-20180215) erlaubt.

**Runden:** Die Prozente werden nach dem *Hare-Niemeyer-Verfahren* (größter Rest) gerundet und ergeben daher immer genau 100 %.

**Außerdem:**

- **Ansicht › Sprache** wechselt zwischen Deutsch und Englisch.
- **Ansicht › Schriftgröße** macht alles größer.
- **Tastenkürzel:** <kbd>Strg</kbd>+<kbd>N</kbd> fügt ein Garn hinzu, <kbd>Strg</kbd>+<kbd>L</kbd> ein Garn aus der Bibliothek, <kbd>F1</kbd> öffnet die Anleitung. Am Mac nimmst du <kbd>Cmd</kbd> statt <kbd>Strg</kbd>.

### Deine Daten und Sicherungen

Alles wird **automatisch gespeichert**: deine Garn-Bibliothek, die Faserliste, deine Einstellungen und das Projekt, an dem du gerade arbeitest. Es ist beim nächsten Start wieder da.

Alles steht in einer einzigen Datei `data.json` in deinem persönlichen Datenordner:

| System | Ordner |
|---|---|
| Windows | `%APPDATA%\YarnCalculator` |
| macOS | `~/Library/Application Support/YarnCalculator` |
| Linux | `~/.local/share/yarncalculator` |

**Datei › Datenordner öffnen** bringt dich direkt dorthin.

- **Datei › Sicherung exportieren…** speichert alles in einer Datei, z. B. für einen USB-Stick oder einen neuen Computer.
- **Datei › Importieren…** übernimmt Garne aus einer Sicherung oder aus der alten `yarns.json`/`fibers.json`. Es wird nichts überschrieben: Gibt es schon ein Garn mit gleichem Namen, aber anderer Zusammensetzung, bekommt das importierte eine Nummer, z. B. `Sockenwolle (2)`.
- `data.json.bak` im Datenordner ist eine Kopie des Stands vom letzten Programmstart.
- Sollte `data.json` einmal beschädigt sein, stellt das Programm automatisch die `.bak`-Kopie wieder her und bewahrt die beschädigte Datei auf.

<details>
<summary>Dateiformat (für Neugierige)</summary>

Siehe das Beispiel im [englischen Teil](#your-data-and-backups). `version` erlaubt künftigen Versionen eine sichere Übernahme der Daten. Die Werte unter `project` werden genau so gespeichert, wie sie eingetippt wurden, damit halbfertige Eingaben einen Neustart überstehen.
Um die Daten woanders abzulegen (z. B. auf einem USB-Stick), starte mit `java -Dyarncalc.dir=PFAD YarnCalculator.java`.
</details>

### Probleme lösen

- **Beim Doppelklick auf `start.bat` passiert nichts.** Öffne im Ordner eine Eingabeaufforderung und führe `java YarnCalculator.java` aus, dann siehst du die Fehlermeldung. Häufigste Ursachen:
  - `'java' ist nicht ... erkannt`: Java ist nicht installiert oder nicht im PATH. Neu installieren und dabei „Add to PATH“ einschalten.
  - Viele Zeilen wie `error: ... nicht unterstützt in -source 11`: Deine Java-Version ist zu alt. Version 17 oder neuer installieren.
  - Compiler nicht gefunden: Du hast ein JRE installiert. Installiere ein **JDK**.
- **Die Schrift ist zu klein.** Nutze **Ansicht › Schriftgröße**.
- **Meine alten Garne fehlen.** Nutze **Datei › Importieren…** und wähle die alte `yarns.json`.

---

License: GNU GPL v3 – see [LICENSE](LICENSE).
