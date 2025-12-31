# Yarn Composition Calculator / Garn-Zusammensetzungs-Rechner

[🇬🇧 English Documentation](#-english-documentation) | [🇩🇪 Deutsche Dokumentation](#-deutsche-dokumentation)

---

<a name="-english-documentation"></a>
## 🇬🇧 English Documentation

A robust Java Swing application designed for textile enthusiasts, knitters, and designers. It calculates the exact fiber composition of a project that combines multiple different yarns with varying weights and blends.

### 📋 Prerequisites

*   **Java Development Kit (JDK) 25** or higher.
    *   This application leverages modern Java features. Please ensure you have the latest JDK installed.
    *   Download: [Adoptium (Eclipse Temurin)](https://adoptium.net/) or [Oracle](https://www.oracle.com/java/technologies/downloads/).
*   **Fiber names: https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A02011R1007-20180215**

### 🚀 How to Run (For Windows, skip this step)

1.  Download the `GarnRechnerProzedural.java` file.
2.  Open your terminal or command prompt.
3.  Navigate to the directory containing the file.
4.  Compile the code:
    ```bash
    javac GarnRechnerProzedural.java
    ```
5.  Run the application:
    ```bash
    java GarnRechnerProzedural
    ```

### 🪟 Windows Easy Setup (ZIP & Shortcut)

If you have downloaded a ZIP file containing the program, follow these steps to set it up permanently:

1.  **Unpack:**
    *   Right-click the downloaded ZIP file.
    *   Select "Extract All..." (or use 7-Zip/WinRAR).
    *   Choose a permanent folder where you want the program to live (e.g., `C:\Users\YourName\Documents\YarnCalculator`).
    *   Click **Extract**.

2.  **Verify/Create Batch File:**
    *   Open the folder. Look for a file named `start.bat` (or similar).
    *   *If it doesn't exist:* Right-click in the empty folder space > New > Text Document. Name it `start.bat` (make sure the file extension changes from `.txt` to `.bat`). Right-click it > Edit, and paste this text:
        ```bat
        @echo off
        start javaw GarnRechnerProzedural
        exit
        ```
    *   Save and close the file.

3.  **Create Desktop Shortcut:**
    *   Right-click the `start.bat` file.
    *   Select **Show more options** (Windows 11) if necessary.
    *   Select **Send to** > **Desktop (create shortcut)**.

4.  **Launch:**
    *   You can now start the calculator anytime by double-clicking the new shortcut on your Desktop.

### ✨ Features

*   **Multi-Yarn Calculation:** Add as many yarn sources as needed. The interface automatically numbers them (Yarn 1, Yarn 2, etc.) and uses a thick border to distinguish them.
*   **Weighted Calculation:** Enter the specific weight (in grams) for each yarn to get a mathematically accurate total composition based on the ratio of materials.
*   **Template System (JSON):**
    *   **Yarn Templates:** Save your favorite yarn blends (e.g., "Sock Wool Classic") to a local file and reuse them later.
    *   **Fiber Database:** Manage a list of fiber types (Wool, Cotton, Silk, etc.) in a dropdown menu. You can add new ones or delete existing ones directly from the UI.
*   **Smart Editing:**
    *   **Locking:** Predefined templates are locked by default to prevent accidental changes.
    *   **Edit Mode:** Clicking **"Edit Yarn"** unlocks the fields and switches the dropdown to **"New"**, allowing you to modify a preset without overwriting the original unless desired.
    *   **Save/Overwrite:** Use **"Save Yarn"** to store new templates or overwrite existing ones.
*   **Precision Rounding:** The app uses the **Largest Remainder Method** (Hare-Niemeyer) to ensure the calculated percentages always sum up to exactly **100.0%**. It minimizes statistical rounding errors better than standard rounding.
*   **Excel Export:** The result dialog includes a **"Copy"** button. This puts the data into your clipboard in a format optimized for pasting directly into Excel, Google Sheets, or other spreadsheet software (Tab-separated values).
*   **Bilingual Interface:** Toggle between **English (EN)** and **German (DE)** instantly via the buttons in the top-right corner.

### 📖 Usage Guide

1.  **Add Yarn:** Click **"Add Yarn"** to create a new input block.
2.  **Select Template:**
    *   Choose a saved yarn from the dropdown menu to auto-fill fibers.
    *   Or select **"New"** to define a custom blend.
3.  **Enter Weight:** Input the weight in **"Grams"** used for this specific yarn.
4.  **Define Fibers:**
    *   Select a fiber from the dropdown or type a new name.
    *   Enter the percentage for that fiber.
    *   **Buttons:**
        *   **`S` (Save):** Saves the currently typed fiber name to the global dropdown list.
        *   **`D` (Delete):** Removes the selected fiber type from the global dropdown list.
        *   **"Remove Fiber":** Removes the specific fiber row from the current yarn.
    *   **Fill Rest:** Click **"Fill Rest to 100%"** to automatically calculate the remaining percentage for the last row.
5.  **Calculate:** Click the **"Calculate"** button at the bottom.
6.  **Export:** In the result window, click **"Copy"** to put the table into your clipboard.

### 📂 Configuration Files

The application automatically generates two JSON files in the same directory to store your data.

#### `yarns.json`
Stores your saved yarn templates.
```json
{
  "Sock Wool Classic": {
    "f0": { "name": "Virgin Wool", "percentage": 75.00 },
    "f1": { "name": "Polyamide", "percentage": 25.00 }
  }
}
```

#### `fibers.json`
Stores the list of available fiber names for the dropdown.
```json
[
  "Cotton",
  "Merino",
  "Silk",
  "Polyamide"
]
```

---

<a name="-deutsche-dokumentation"></a>
## 🇩🇪 Deutsche Dokumentation

Eine leistungsstarke Java-Swing-Anwendung für Textilliebhaber, Stricker und Designer. Das Programm berechnet die exakte Faserzusammensetzung eines Projekts, bei dem mehrere verschiedene Garne mit unterschiedlichen Gewichten und Mischungen kombiniert werden.

### 📋 Voraussetzungen

*   **Java Development Kit (JDK) 25** oder höher.
    *   Die Anwendung nutzt moderne Java-Funktionen. Bitte stellen Sie sicher, dass Sie das aktuelle JDK installiert haben.
    *   Download: [Adoptium (Eclipse Temurin)](https://adoptium.net/) oder [Oracle](https://www.oracle.com/java/technologies/downloads/).
*   **Faser-Namen: https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A02011R1007-20180215**

### 🚀 Installation & Start (Für Windows, diesen Schritt überspringen)

1.  Laden Sie die Datei `GarnRechnerProzedural.java` herunter.
2.  Öffnen Sie Ihr Terminal oder die Eingabeaufforderung.
3.  Navigieren Sie in den Ordner, der die Datei enthält.
4.  Kompilieren Sie den Code:
    ```bash
    javac GarnRechnerProzedural.java
    ```
5.  Starten Sie das Programm:
    ```bash
    java GarnRechnerProzedural
    ```

### 🪟 Windows Einrichtung (ZIP & Verknüpfung)

Wenn Sie eine ZIP-Datei mit dem Programm heruntergeladen haben, folgen Sie diesen Schritten für eine dauerhafte Einrichtung:

1.  **Entpacken:**
    *   Rechtsklick auf die heruntergeladene ZIP-Datei.
    *   Wählen Sie "Alle extrahieren..." (oder nutzen Sie 7-Zip/WinRAR).
    *   Wählen Sie einen dauerhaften Ordner, in dem das Programm liegen soll (z. B. `C:\Benutzer\IhrName\Dokumente\GarnRechner`).
    *   Klicken Sie auf **Extrahieren**.

2.  **Start-Datei prüfen/erstellen:**
    *   Öffnen Sie den Ordner. Suchen Sie nach einer Datei namens `start.bat` (oder ähnlich).
    *   *Falls nicht vorhanden:* Rechtsklick in den leeren Bereich > Neu > Textdokument. Nennen Sie es `start.bat` (bestätigen Sie die Änderung der Dateiendung von `.txt` zu `.bat`). Rechtsklick darauf > Bearbeiten und folgenden Text einfügen:
        ```bat
        @echo off
        start javaw GarnRechnerProzedural
        exit
        ```
    *   Speichern und schließen.

3.  **Desktop-Verknüpfung erstellen:**
    *   Rechtsklick auf die Datei `start.bat`.
    *   Wählen Sie **Weitere Optionen anzeigen** (falls Windows 11).
    *   Wählen Sie **Senden an** > **Desktop (Verknüpfung erstellen)**.

4.  **Starten:**
    *   Sie können den Rechner nun jederzeit per Doppelklick auf die Verknüpfung auf Ihrem Desktop starten.

### ✨ Funktionen

*   **Mehrfach-Garn-Berechnung:** Fügen Sie beliebig viele Garnquellen hinzu. Die Oberfläche nummeriert diese automatisch (Garn 1, Garn 2, usw.) und hebt sie durch einen dicken Rahmen hervor.
*   **Gewichtete Berechnung:** Geben Sie das spezifische Gewicht (in Gramm) für jedes Garn ein, um eine mathematisch exakte Gesamtzusammensetzung basierend auf dem Materialverhältnis zu erhalten.
*   **Vorlagen-System (JSON):**
    *   **Garn-Vorlagen:** Speichern Sie Ihre bevorzugten Garnmischungen (z. B. "Sockenwolle Klassik") in einer lokalen Datei, um sie später wiederzuverwenden.
    *   **Faser-Datenbank:** Verwalten Sie eine Liste von Faserarten (Wolle, Baumwolle, Seide usw.) in einem Dropdown-Menü. Sie können neue Arten hinzufügen oder bestehende löschen.
*   **Intelligentes Bearbeiten:**
    *   **Sperre:** Vordefinierte Vorlagen sind standardmäßig gesperrt, um versehentliche Änderungen zu verhindern.
    *   **Bearbeiten-Modus:** Ein Klick auf **"Garn Bearbeiten"** entsperrt die Felder und wechselt in den Modus **"Neu"**, sodass Sie eine Mischung anpassen können, ohne das Original zu überschreiben (es sei denn, Sie wünschen dies).
    *   **Speichern/Überschreiben:** Mit **"Garn Speichern"** können Sie neue Vorlagen sichern oder bestehende nach einer Bestätigung überschreiben.
*   **Präzises Runden:** Die App verwendet das **Hare-Niemeyer-Verfahren** (Largest Remainder Method), um sicherzustellen, dass die Endsumme der Anteile immer exakt **100,0 %** beträgt. Dies minimiert statistische Rundungsfehler besser als herkömmliches Runden.
*   **Excel-Export:** Das Ergebnisfenster enthält einen **"Kopieren"**-Button. Dieser kopiert die Tabelle in die Zwischenablage in einem Format, das direkt in Excel oder Google Sheets eingefügt werden kann.
*   **Zweisprachige Oberfläche:** Wechseln Sie über die Buttons oben rechts sofort zwischen **Englisch (EN)** und **Deutsch (DE)**.

### 📖 Bedienungsanleitung

1.  **Garn hinzufügen:** Klicken Sie auf **"Garn hinzufügen"**, um einen neuen Eingabeblock zu erstellen.
2.  **Vorlage wählen:**
    *   Wählen Sie ein gespeichertes Garn aus dem Dropdown-Menü, um die Fasern automatisch auszufüllen.
    *   Oder lassen Sie die Auswahl auf **"Neu"**, um eine eigene Mischung zu definieren.
3.  **Gewicht eingeben:** Tragen Sie das Gewicht in **"Gramm"** ein, das von diesem Garn verwendet wird.
4.  **Fasern definieren:**
    *   Wählen Sie eine Faser aus dem Dropdown oder tippen Sie einen neuen Namen ein.
    *   Geben Sie den prozentualen Anteil ein.
    *   **Buttons:**
        *   **`S` (Speichern):** Speichert den aktuell eingetippten Fasernamen in die globale Dropdown-Liste.
        *   **`D` (Löschen):** Entfernt die ausgewählte Faserart aus der globalen Liste.
        *   **"Faser Entfernen":** Entfernt die spezifische Faserzeile aus dem aktuellen Garn.
    *   **Rest auffüllen:** Klicken Sie auf **"Rest auf 100%"**, um den verbleibenden Anteil für die letzte Zeile automatisch zu berechnen.
5.  **Berechnen:** Klicken Sie unten auf den Button **"Berechnen"**.
6.  **Exportieren:** Klicken Sie im Ergebnisfenster auf **"Kopieren"**, um die Tabelle in die Zwischenablage zu legen.

### 📂 Konfigurationsdateien

Die Anwendung erstellt automatisch zwei JSON-Dateien im selben Verzeichnis, um Ihre Daten zu speichern.

#### `yarns.json`
Speichert Ihre Garn-Vorlagen.
```json
{
  "Sockenwolle Klassik": {
    "f0": { "name": "Schurwolle", "percentage": 75.00 },
    "f1": { "name": "Polyamid", "percentage": 25.00 }
  }
}
```

#### `fibers.json`
Speichert die Liste der verfügbaren Fasernamen für das Dropdown-Menü.
```json
[
  "Baumwolle",
  "Merino",
  "Seide",
  "Polyamid"
]
```
