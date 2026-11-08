/*
 * Yarn Calculator / Garn-Rechner
 * Calculates the fiber composition of a project made from several yarns.
 *
 * Run it directly (JDK 17 or newer, no compiling needed):
 *     java YarnCalculator.java
 *
 * License: GNU GPL v3 (see LICENSE)
 */

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.Collator;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YarnCalculator {

    static final String VERSION = "2.0";
    static final int DATA_VERSION = 2;

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        T.lang = Lang.fromSystem();
        Store store = new Store(Store.defaultDir());
        Store.Loaded loaded = store.load();
        AppData data = loaded.data();
        T.lang = data.settings.lang;
        SwingUtilities.invokeLater(() -> {
            Ui.installLookAndFeel(data.settings.fontScale);
            Ui.applyLanguage();
            MainFrame frame = new MainFrame(data, store);
            frame.setVisible(true);
            frame.startupDialogs(loaded);
        });
    }

    // =====================================================================
    // Translations
    // =====================================================================

    enum Lang {
        DE("de", Locale.GERMANY, "Deutsch"), EN("en", Locale.US, "English");
        final String code; final Locale locale; final String label;
        Lang(String code, Locale locale, String label) { this.code = code; this.locale = locale; this.label = label; }
        static Lang fromSystem() { return "de".equals(Locale.getDefault().getLanguage()) ? DE : EN; }
        static Lang fromCode(String c) {
            for (Lang l : values()) if (l.code.equals(c)) return l;
            return fromSystem();
        }
    }

    static String tr(String key) { return T.get(key); }
    static String trf(String key, Object... args) { return String.format(T.lang.locale, T.get(key), args); }

    static final class T {
        static Lang lang = Lang.EN;
        private static final Map<String, String[]> M = new HashMap<>();
        static String get(String k) {
            String[] v = M.get(k);
            if (v == null) return "!" + k;
            return v[lang == Lang.DE ? 0 : 1];
        }
        private static void p(String k, String de, String en) { M.put(k, new String[]{de, en}); }

        static {
            p("app_title", "Garn-Rechner", "Yarn Calculator");
            // Menus
            p("menu_file", "Datei", "File");
            p("mi_import", "Importieren…", "Import…");
            p("mi_export", "Sicherung exportieren…", "Export backup…");
            p("mi_open_folder", "Datenordner öffnen", "Open data folder");
            p("mi_exit", "Beenden", "Exit");
            p("menu_project", "Projekt", "Project");
            p("mi_add_yarn", "Garn hinzufügen", "Add yarn");
            p("mi_add_from_lib", "Garn aus der Bibliothek hinzufügen…", "Add yarn from library…");
            p("mi_clear", "Neues Projekt", "New project");
            p("menu_library", "Bibliothek", "Library");
            p("mi_manage_yarns", "Garne verwalten…", "Manage yarns…");
            p("mi_manage_fibers", "Fasern verwalten…", "Manage fibers…");
            p("menu_view", "Ansicht", "View");
            p("mi_language", "Sprache", "Language");
            p("mi_text_size", "Schriftgröße", "Text size");
            p("menu_help", "Hilfe", "Help");
            p("mi_guide", "Anleitung", "Guide");
            p("mi_about", "Über den Garn-Rechner", "About Yarn Calculator");
            // Project area
            p("proj_title", "Dein Projekt", "Your project");
            p("proj_hint", "Lege für jedes verwendete Garn einen Eintrag an.", "Add one entry for every yarn you used.");
            p("btn_add_yarn", "+ Garn hinzufügen", "+ Add yarn");
            p("btn_add_from_lib", "Aus der Bibliothek…", "From library…");
            p("btn_clear", "Neues Projekt", "New project");
            p("empty_title", "Noch keine Garne im Projekt", "No yarns in this project yet");
            p("empty_text", "Füge ein Garn hinzu, um loszulegen.", "Add a yarn to get started.");
            // Yarn card
            p("card_title", "Garn %d", "Yarn %d");
            p("lbl_name", "Name", "Name");
            p("hint_name", "z. B. Sockenwolle Uni", "e.g. Sock yarn solid");
            p("lbl_weight", "Gewicht", "Weight");
            p("unit_g", "g", "g");
            p("btn_from_lib", "Aus Bibliothek…", "From library…");
            p("tip_from_lib", "Zusammensetzung eines gespeicherten Garns übernehmen", "Use the composition of a saved yarn");
            p("btn_save_lib", "In Bibliothek speichern", "Save to library");
            p("btn_update_lib", "Bibliothek aktualisieren", "Update library");
            p("btn_saved_lib", "Gespeichert", "Saved");
            p("tip_save_lib", "Dieses Garn für spätere Projekte speichern", "Save this yarn for future projects");
            p("tip_remove_yarn", "Garn aus dem Projekt entfernen", "Remove yarn from project");
            p("lib_state_saved", "In der Bibliothek gespeichert", "Saved in library");
            p("lib_state_changed", "Weicht von der Bibliothek ab", "Differs from library");
            p("lib_state_new", "Noch nicht in der Bibliothek", "Not in library yet");
            p("hdr_fiber", "Faser", "Fiber");
            p("hdr_share", "Anteil", "Share");
            p("btn_add_fiber", "+ Faser", "+ Add fiber");
            p("tip_remove_fiber", "Faser entfernen", "Remove fiber");
            p("tip_fiber", "Tippen zum Suchen – oder einen eigenen Namen eingeben", "Type to search – or enter your own name");
            p("btn_fill", "Auffüllen", "Fill up");
            p("tip_fill", "Fehlenden Anteil automatisch eintragen", "Enter the missing share automatically");
            // Checks
            p("chk_ok", "Vollständig – die Anteile ergeben 100 %", "Complete – shares add up to 100%");
            p("chk_no_fibers", "Trage mindestens eine Faser ein.", "Enter at least one fiber.");
            p("chk_name_missing", "Bei einer Faser fehlt der Name.", "A fiber has no name.");
            p("chk_percent_invalid", "„%s“ bei %s ist keine gültige Zahl.", "\"%s\" for %s is not a valid number.");
            p("chk_percent_range", "Der Anteil von %s muss zwischen 0 und 100 %% liegen.", "The share of %s must be between 0 and 100%%.");
            p("chk_percent_blank", "Trage bei jeder Faser einen Anteil ein.", "Enter a share for every fiber.");
            p("chk_percent_blank_fill", "Bei %s fehlt der Anteil (übrig: %s).", "The share for %s is missing (%s left).");
            p("chk_sum_low", "Die Anteile ergeben %s – es fehlen %s.", "Shares add up to %s – %s missing.");
            p("chk_sum_high", "Die Anteile ergeben %s – %s zu viel.", "Shares add up to %s – %s too much.");
            p("chk_grams_blank", "Trage das verwendete Gewicht in Gramm ein.", "Enter the weight used, in grams.");
            p("chk_grams_invalid", "Das Gewicht „%s“ ist keine gültige Zahl.", "The weight \"%s\" is not a valid number.");
            p("chk_grams_negative", "Das Gewicht darf nicht negativ sein.", "The weight can't be negative.");
            // Result
            p("res_title", "Ergebnis", "Result");
            p("res_total", "Gesamtgewicht %s g aus %d Garn(en)", "Total weight %s g from %d yarn(s)");
            p("res_empty", "Füge links ein Garn hinzu –\ndas Ergebnis erscheint dann hier.", "Add a yarn on the left –\nthe result will appear here.");
            p("res_todo_title", "Fast geschafft! Noch offen:", "Almost there! Still to do:");
            p("res_todo_hint", "Das Ergebnis erscheint, sobald alle Garne grün markiert sind.", "The result appears as soon as every yarn is marked green.");
            p("res_total_zero", "Das Gesamtgewicht ist 0 g – trage bei mindestens einem Garn ein Gewicht ein.", "The total weight is 0 g – enter a weight for at least one yarn.");
            p("col_fiber", "Faser", "Fiber");
            p("col_share", "Anteil (%)", "Share (%)");
            p("col_grams", "Gramm", "Grams");
            p("row_total", "Gesamt", "Total");
            p("lbl_round", "Runden auf", "Round to");
            p("round_0", "ganze Prozent", "whole percent");
            p("round_1", "eine Nachkommastelle", "one decimal");
            p("lbl_label_text", "Etikett-Text", "Label text");
            p("btn_copy_table", "Für Excel kopieren", "Copy for Excel");
            p("tip_copy_table", "Tabelle kopieren – danach in Excel, LibreOffice oder Google Sheets einfügen", "Copy the table – then paste it into Excel, LibreOffice or Google Sheets");
            p("btn_copy_label", "Etikett-Text kopieren", "Copy label text");
            p("st_copied_table", "Tabelle kopiert – einfach in eine Tabellenkalkulation einfügen.", "Table copied – just paste it into a spreadsheet.");
            p("st_copied_label", "Etikett-Text kopiert.", "Label text copied.");
            // Status bar
            p("st_removed", "„%s“ entfernt.", "Removed \"%s\".");
            p("btn_undo", "Rückgängig", "Undo");
            p("st_save_error", "Speichern fehlgeschlagen: %s", "Could not save: %s");
            p("st_saved_lib", "„%s“ in der Bibliothek gespeichert.", "Saved \"%s\" to the library.");
            p("st_already_saved", "„%s“ ist bereits genau so gespeichert.", "\"%s\" is already saved exactly like this.");
            p("st_added_to_project", "„%s“ zum Projekt hinzugefügt.", "Added \"%s\" to the project.");
            // Dialogs
            p("dlg_name_prompt", "Unter welchem Namen soll das Garn gespeichert werden?", "What name should the yarn be saved under?");
            p("dlg_fix_first", "Bitte zuerst die Zusammensetzung vervollständigen:\n%s", "Please complete the composition first:\n%s");
            p("dlg_replace", "In der Bibliothek ist „%s“ gespeichert als\n    %s\n\nDurch diese Zusammensetzung ersetzen?\n    %s",
                    "The library has \"%s\" saved as\n    %s\n\nReplace it with this composition?\n    %s");
            p("btn_replace", "Ersetzen", "Replace");
            p("btn_cancel", "Abbrechen", "Cancel");
            p("btn_ok", "OK", "OK");
            p("btn_close", "Schließen", "Close");
            p("btn_save", "Speichern", "Save");
            p("yes", "Ja", "Yes");
            p("no", "Nein", "No");
            p("dlg_clear", "Alle Garne aus dem aktuellen Projekt entfernen?\nDeine Bibliothek bleibt unverändert.", "Remove all yarns from the current project?\nYour library is not affected.");
            p("btn_clear_confirm", "Projekt leeren", "Clear project");
            // Picker / library
            p("picker_title", "Garn aus der Bibliothek wählen", "Choose a yarn from the library");
            p("btn_use", "Übernehmen", "Use");
            p("hint_search", "Suchen nach Name oder Faser…", "Search by name or fiber…");
            p("lib_empty", "Die Bibliothek ist noch leer.\n\nFülle im Projekt ein Garn aus und klicke auf „In Bibliothek speichern“.",
                    "Your library is still empty.\n\nFill in a yarn in your project and click \"Save to library\".");
            p("lib_title", "Garn-Bibliothek", "Yarn library");
            p("lib_count", "%d Garn(e) gespeichert", "%d yarn(s) saved");
            p("btn_new", "Neu…", "New…");
            p("btn_edit", "Bearbeiten…", "Edit…");
            p("btn_duplicate", "Duplizieren", "Duplicate");
            p("btn_delete", "Löschen", "Delete");
            p("btn_to_project", "Zum Projekt hinzufügen", "Add to project");
            p("dlg_delete_yarn", "„%s“ aus der Bibliothek löschen?", "Delete \"%s\" from the library?");
            p("copy_suffix", "Kopie", "copy");
            p("edit_title_new", "Neues Garn", "New yarn");
            p("edit_title", "Garn bearbeiten", "Edit yarn");
            p("err_name_blank", "Bitte einen Namen eingeben.", "Please enter a name.");
            p("err_name_taken", "Es gibt bereits ein Garn namens „%s“.", "There is already a yarn called \"%s\".");
            // Fibers
            p("fib_title", "Faserliste", "Fiber list");
            p("fib_info", "Diese Namen werden beim Tippen vorgeschlagen. Du kannst jederzeit auch andere Namen eingeben.",
                    "These names are suggested while you type. You can always enter other names too.");
            p("btn_add", "Hinzufügen…", "Add…");
            p("btn_rename", "Umbenennen…", "Rename…");
            p("btn_add_common", "Gängige Fasern ergänzen", "Add common fibers");
            p("dlg_fiber_name", "Name der Faser:", "Fiber name:");
            p("dlg_fiber_rename", "Neuer Name für „%s“\n(wird auch in gespeicherten Garnen und im Projekt geändert):",
                    "New name for \"%s\"\n(also changed in saved yarns and in the project):");
            p("msg_fiber_exists", "„%s“ ist bereits in der Liste.", "\"%s\" is already in the list.");
            p("dlg_delete_fiber", "„%s“ aus der Vorschlagsliste entfernen?", "Remove \"%s\" from the suggestions?");
            p("dlg_delete_fiber_used", "„%s“ aus der Vorschlagsliste entfernen?\n\n%d gespeicherte(s) Garn(e) verwenden diese Faser und behalten sie.",
                    "Remove \"%s\" from the suggestions?\n\n%d saved yarn(s) use this fiber and will keep it.");
            p("msg_common_added", "%d Fasernamen ergänzt.", "Added %d fiber names.");
            // Import / export
            p("imp_title", "Daten importieren", "Import data");
            p("imp_filter", "Garn-Rechner-Daten (*.json)", "Yarn Calculator data (*.json)");
            p("imp_done", "Import aus %s\n\n•  %d Garn(e) neu hinzugefügt\n•  %d Garn(e) mit Nummer hinzugefügt (Name war mit anderer Zusammensetzung schon vergeben)\n•  %d Garn(e) waren bereits vorhanden\n•  %d neue Fasernamen",
                    "Import from %s\n\n•  %d yarn(s) added\n•  %d yarn(s) added with a number (name already used with a different composition)\n•  %d yarn(s) were already there\n•  %d new fiber names");
            p("imp_nothing", "In der gewählten Datei bzw. im Ordner wurden keine Garn-Daten gefunden.", "No yarn data was found in the selected file or folder.");
            p("imp_error", "Die Datei konnte nicht gelesen werden:\n%s", "The file could not be read:\n%s");
            p("exp_title", "Sicherung exportieren", "Export backup");
            p("exp_done", "Sicherung gespeichert:\n%s", "Backup saved:\n%s");
            p("exp_overwrite", "„%s“ existiert bereits. Ersetzen?", "\"%s\" already exists. Replace it?");
            p("folder_text", "Deine Daten liegen in diesem Ordner:", "Your data is stored in this folder:");
            // Startup notices
            p("notice_migrated", "Deine bisherigen Daten wurden übernommen: %d Garn(e) und %d Fasernamen aus %s. Die alten Dateien yarns.json und fibers.json wurden nicht verändert.",
                    "Your existing data was imported: %d yarn(s) and %d fiber names from %s. The old files yarns.json and fibers.json were left untouched.");
            p("notice_restored", "Die Datendatei war beschädigt. Der Stand vom letzten Programmstart wurde wiederhergestellt. Die beschädigte Datei wurde aufbewahrt als %s",
                    "The data file was damaged. The state from the last program start was restored. The damaged file was kept as %s");
            p("notice_broken", "Die Datendatei war beschädigt und es gab keine Sicherung. Das Programm startet mit leeren Daten. Die beschädigte Datei wurde aufbewahrt als %s",
                    "The data file was damaged and there was no backup. The program starts with empty data. The damaged file was kept as %s");
            p("notice_newer", "Diese Daten wurden mit einer neueren Version des Garn-Rechners gespeichert. Bitte aktualisiere das Programm.",
                    "This data was saved by a newer version of Yarn Calculator. Please update the program.");
            p("notice_title", "Hinweis", "Note");
            // Misc
            p("guide_title", "Anleitung", "Guide");
            p("about_text", "Garn-Rechner %s\nBerechnet die Faserzusammensetzung von Projekten aus mehreren Garnen.\n\nDatenordner:\n%s\n\nLizenz: GNU GPL v3",
                    "Yarn Calculator %s\nCalculates the fiber composition of projects made from several yarns.\n\nData folder:\n%s\n\nLicense: GNU GPL v3");
            p("ex_yarn", "Sockenwolle Klassik", "Classic sock yarn");
        }

        static final String[] COMMON_FIBERS_DE = {"Alpaka", "Angora", "Baumwolle", "Elasthan", "Hanf", "Kamel", "Kaschmir", "Lama",
                "Leinen", "Lyocell", "Modal", "Mohair", "Polyacryl", "Polyamid", "Polyester", "Schurwolle", "Seide", "Viskose", "Wolle", "Yak"};
        static final String[] COMMON_FIBERS_EN = {"Alpaca", "Angora", "Camel", "Cashmere", "Cotton", "Elastane", "Hemp", "Llama",
                "Linen", "Lyocell", "Modal", "Mohair", "Acrylic", "Polyamide", "Polyester", "Virgin wool", "Silk", "Viscose", "Wool", "Yak"};
        static String[] commonFibers() { return lang == Lang.DE ? COMMON_FIBERS_DE : COMMON_FIBERS_EN; }

        static String guideHtml(String dataDir) {
            String shortcut = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("mac") ? "Cmd" : "Strg";
            if (lang == Lang.EN && shortcut.equals("Strg")) shortcut = "Ctrl";
            String dir = "<code>" + Ui.esc(dataDir) + "</code>";
            if (lang == Lang.DE) return """
                <h2>Willkommen beim Garn-Rechner</h2>
                <p>Du hast ein Projekt aus mehreren Garnen gestrickt oder gehäkelt? Der Garn-Rechner sagt dir,
                woraus das fertige Stück insgesamt besteht – zum Beispiel für ein Pflegeetikett.</p>
                <h3>In vier Schritten</h3>
                <ol>
                <li><b>Garn hinzufügen</b> – ein Eintrag für jedes Garn, das du verwendet hast
                    (<i>+ Garn hinzufügen</i>, oder <i>Aus der Bibliothek…</i> für ein gespeichertes Garn).</li>
                <li><b>Gewicht eintragen</b> – wie viel Gramm du von diesem Garn verbraucht hast.
                    Tipp: Restknäuel wiegen und vom Knäuelgewicht abziehen.</li>
                <li><b>Fasern eintragen</b> – so wie sie auf der Banderole stehen, z.&nbsp;B. <i>Schurwolle 75</i> und
                    <i>Polyamid 25</i>. Beim Tippen wird der Fasername vorgeschlagen.</li>
                <li><b>Ergebnis ablesen</b> – rechts, es aktualisiert sich beim Tippen. <i>Für Excel kopieren</i> legt die
                    Tabelle in die Zwischenablage, <i>Etikett-Text kopieren</i> eine Zeile wie <i>75 % Schurwolle, 25 % Polyamid</i>.</li>
                </ol>
                <h3>Was die Farben bedeuten</h3>
                <ul>
                <li><font color="#2E7D32"><b>Grün</b></font> – das Garn ist vollständig.</li>
                <li><font color="#C77700"><b>Orange</b></font> – es fehlt noch etwas, z.&nbsp;B. ergeben die Anteile noch keine 100 %.
                    Wenn möglich, erscheint ein Knopf <i>Auffüllen</i>, der den fehlenden Anteil einträgt.</li>
                <li><font color="#C62828"><b>Rot</b></font> – eine Eingabe ist ungültig, z.&nbsp;B. Buchstaben in einem Zahlenfeld.</li>
                </ul>
                <p>Das Ergebnis erscheint, sobald alle Garne grün sind. Komma und Punkt funktionieren beide als Dezimaltrenner.</p>
                <h3>Deine Garn-Bibliothek</h3>
                <p>Garne, die du öfter verwendest, kannst du speichern: Garn ausfüllen und <i>In Bibliothek speichern</i> klicken.
                Beim nächsten Projekt auf <i>Aus Bibliothek…</i> klicken und das Garn auswählen – dann musst du nur noch das Gewicht eintragen.</p>
                <p>Unter dem Namen siehst du, ob das Garn <i>in der Bibliothek gespeichert</i> ist oder <i>von der Bibliothek abweicht</i>
                (dann speichert <i>Bibliothek aktualisieren</i> deine Änderung). Umbenennen, bearbeiten und löschen kannst du
                gespeicherte Garne unter <b>Bibliothek › Garne verwalten</b>.</p>
                <h3>Fasernamen</h3>
                <p>Die Faserliste macht nur Vorschläge – du kannst jeden Namen eintippen. Neue Namen landen automatisch in der Liste,
                wenn du ein Garn in der Bibliothek speicherst. Bearbeiten unter <b>Bibliothek › Fasern verwalten</b>.
                Für Textiletiketten in der EU sind nur die Fasernamen aus Anhang I der
                <a href="https://eur-lex.europa.eu/legal-content/DE/TXT/?uri=CELEX%3A02011R1007-20180215">Verordnung (EU) Nr. 1007/2011</a> erlaubt.</p>
                <h3>Runden</h3>
                <p>Wähle ganze Prozent oder eine Nachkommastelle. Der Rechner nutzt das Hare-Niemeyer-Verfahren
                (größter Rest), daher ergeben die Anteile immer genau 100 %. Eine Faser, die nur in sehr kleiner Menge
                enthalten ist, wird nie mit 0 % angezeigt, sondern mit mindestens 1 % (bzw. 0,1 %) – dieser Anteil wird
                anteilig von den anderen Fasern abgezogen.</p>
                <h3>Deine Daten</h3>
                <p>Alles wird automatisch gespeichert – die Bibliothek und auch dein aktuelles Projekt, das beim nächsten Start wieder da ist.
                Datenordner: %DIR%</p>
                <ul>
                <li><b>Datei › Sicherung exportieren…</b> speichert alles in einer Datei, z.&nbsp;B. für einen USB-Stick oder einen neuen Computer.</li>
                <li><b>Datei › Importieren…</b> übernimmt Garne aus einer Sicherung <i>oder</i> aus den Dateien yarns.json/fibers.json
                    der alten Version. Es wird nichts überschrieben: Gibt es schon ein Garn mit gleichem Namen, aber anderer
                    Zusammensetzung, bekommt das importierte eine Nummer.</li>
                <li>Der Stand beim letzten Programmstart liegt zusätzlich als <code>data.json.bak</code> im Datenordner.</li>
                </ul>
                <h3>Tastenkürzel</h3>
                <table cellpadding="2">
                <tr><td><b>%K%+N</b></td><td>Garn hinzufügen</td></tr>
                <tr><td><b>%K%+L</b></td><td>Garn aus der Bibliothek hinzufügen</td></tr>
                <tr><td><b>F1</b></td><td>Diese Anleitung</td></tr>
                </table>
                """.replace("%DIR%", dir).replace("%K%", shortcut);
            return """
                <h2>Welcome to Yarn Calculator</h2>
                <p>Knitted or crocheted something from several yarns? Yarn Calculator tells you what the finished
                piece is made of overall – for example for a care label.</p>
                <h3>In four steps</h3>
                <ol>
                <li><b>Add a yarn</b> – one entry for every yarn you used
                    (<i>+ Add yarn</i>, or <i>From library…</i> for a saved one).</li>
                <li><b>Enter the weight</b> – how many grams of that yarn you used.
                    Tip: weigh the leftovers and subtract them from the ball weight.</li>
                <li><b>Enter the fibers</b> – as printed on the ball band, e.g. <i>Wool 75</i> and <i>Polyamide 25</i>.
                    Fiber names are suggested while you type.</li>
                <li><b>Read the result</b> – on the right; it updates while you type. <i>Copy for Excel</i> puts the table
                    on the clipboard, <i>Copy label text</i> copies a line like <i>75% Wool, 25% Polyamide</i>.</li>
                </ol>
                <h3>What the colors mean</h3>
                <ul>
                <li><font color="#2E7D32"><b>Green</b></font> – the yarn is complete.</li>
                <li><font color="#C77700"><b>Orange</b></font> – something is still missing, e.g. the shares don't add up to 100% yet.
                    If possible, a <i>Fill up</i> button enters the missing share for you.</li>
                <li><font color="#C62828"><b>Red</b></font> – an entry is invalid, e.g. letters in a number field.</li>
                </ul>
                <p>The result appears as soon as every yarn is green. Both comma and dot work as decimal separator.</p>
                <h3>Your yarn library</h3>
                <p>Save the yarns you use often: fill in a yarn and click <i>Save to library</i>. Next time, click
                <i>From library…</i> and pick it – then only the weight is left to enter.</p>
                <p>Under the name you can see whether the yarn is <i>saved in library</i> or <i>differs from library</i>
                (then <i>Update library</i> saves your change). Rename, edit or delete saved yarns under
                <b>Library › Manage yarns</b>.</p>
                <h3>Fiber names</h3>
                <p>The fiber list only makes suggestions – you can type any name. New names are added to the list automatically
                when you save a yarn to the library. Edit the list under <b>Library › Manage fibers</b>.
                For textile labels in the EU, only the fiber names in Annex I of
                <a href="https://eur-lex.europa.eu/legal-content/EN/TXT/?uri=CELEX%3A02011R1007-20180215">Regulation (EU) No 1007/2011</a> may be used.</p>
                <h3>Rounding</h3>
                <p>Choose whole percent or one decimal. The calculator uses the largest remainder method,
                so the shares always add up to exactly 100%. A fiber that is only present in a tiny amount is never shown
                as 0% but with at least 1% (or 0.1%) – that amount is taken proportionally from the other fibers.</p>
                <h3>Your data</h3>
                <p>Everything is saved automatically – your library and also your current project, which is back when you start
                the program again. Data folder: %DIR%</p>
                <ul>
                <li><b>File › Export backup…</b> saves everything in one file, e.g. for a USB stick or a new computer.</li>
                <li><b>File › Import…</b> adds yarns from a backup <i>or</i> from the yarns.json/fibers.json files of the old version.
                    Nothing is overwritten: if a yarn with the same name but a different composition exists, the imported one gets a number.</li>
                <li>The state at the last program start is also kept as <code>data.json.bak</code> in the data folder.</li>
                </ul>
                <h3>Keyboard shortcuts</h3>
                <table cellpadding="2">
                <tr><td><b>%K%+N</b></td><td>Add yarn</td></tr>
                <tr><td><b>%K%+L</b></td><td>Add yarn from library</td></tr>
                <tr><td><b>F1</b></td><td>This guide</td></tr>
                </table>
                """.replace("%DIR%", dir).replace("%K%", shortcut);
        }
    }

    // =====================================================================
    // Numbers
    // =====================================================================

    static boolean blank(String s) { return s == null || s.isBlank(); }

    /** Parses user input; accepts comma or dot, ignores spaces and a trailing % or g. Returns null if invalid. */
    static Double parseNum(String s) {
        if (blank(s)) return null;
        String c = s.replaceAll("[\\s\\u00A0\\u202F]", "");
        if (c.endsWith("%") || c.endsWith("g") || c.endsWith("G")) c = c.substring(0, c.length() - 1);
        c = c.replace(',', '.');
        if (!c.matches("[+-]?(\\d+\\.?\\d*|\\.\\d+)")) return null;
        try { return Double.parseDouble(c); } catch (NumberFormatException e) { return null; }
    }

    static String fmt(double v, int decimals) {
        NumberFormat nf = NumberFormat.getNumberInstance(T.lang.locale);
        nf.setGroupingUsed(false);
        nf.setMinimumFractionDigits(decimals);
        nf.setMaximumFractionDigits(decimals);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return nf.format(v);
    }

    /** Up to two decimals, trailing zeros removed. */
    static String fmtFlex(double v) {
        NumberFormat nf = NumberFormat.getNumberInstance(T.lang.locale);
        nf.setGroupingUsed(false);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        nf.setRoundingMode(RoundingMode.HALF_UP);
        return nf.format(v);
    }

    static String pct(String number) { return T.lang == Lang.DE ? number + " %" : number + "%"; }

    static String capitalize(String s) {
        s = s.trim();
        return s.isEmpty() ? s : s.substring(0, 1).toUpperCase(T.lang.locale) + s.substring(1);
    }

    static String key(String s) { return s == null ? "" : s.trim().toLowerCase(Locale.ROOT); }

    // =====================================================================
    // Data model
    // =====================================================================

    record FiberShare(String fiber, double percent) {}

    record LibraryYarn(String name, List<FiberShare> composition) {
        String summary() { return compositionText(composition); }
    }

    static String compositionText(List<FiberShare> shares) {
        StringBuilder b = new StringBuilder();
        for (FiberShare s : shares) {
            if (b.length() > 0) b.append(", ");
            b.append(pct(fmtFlex(s.percent()))).append(' ').append(s.fiber());
        }
        return b.toString();
    }

    static boolean sameComposition(List<FiberShare> a, List<FiberShare> b) {
        Map<String, Double> ma = new HashMap<>(), mb = new HashMap<>();
        for (FiberShare s : a) ma.merge(key(s.fiber()), s.percent(), Double::sum);
        for (FiberShare s : b) mb.merge(key(s.fiber()), s.percent(), Double::sum);
        if (!ma.keySet().equals(mb.keySet())) return false;
        for (var e : ma.entrySet()) if (Math.abs(e.getValue() - mb.get(e.getKey())) > 0.001) return false;
        return true;
    }

    static final class Library {
        final List<String> fibers = new ArrayList<>();
        final List<LibraryYarn> yarns = new ArrayList<>();

        LibraryYarn find(String name) {
            String k = key(name);
            for (LibraryYarn y : yarns) if (key(y.name()).equals(k)) return y;
            return null;
        }

        String findFiber(String name) {
            String k = key(name);
            for (String f : fibers) if (key(f).equals(k)) return f;
            return null;
        }

        /** Adds a fiber name if not yet present (case-insensitive). Returns true if added. */
        boolean addFiber(String name) {
            if (blank(name) || findFiber(name) != null) return false;
            fibers.add(name.trim());
            fibers.sort(Collator.getInstance(T.lang.locale));
            return true;
        }

        void putYarn(LibraryYarn y) {
            LibraryYarn old = find(y.name());
            if (old != null) yarns.remove(old);
            yarns.add(y);
            Collator c = Collator.getInstance(T.lang.locale);
            yarns.sort((a, b) -> c.compare(a.name(), b.name()));
        }

        String uniqueName(String base) {
            if (find(base) == null) return base;
            for (int i = 2; ; i++) {
                String n = base + " (" + i + ")";
                if (find(n) == null) return n;
            }
        }
    }

    static final class Settings {
        Lang lang = Lang.fromSystem();
        int fontScale = 100;
        int decimals = 1;
        boolean guideSeen;
        Rectangle bounds;
        boolean maximized;
    }

    /** A fiber row in the project. Values are kept as typed, so half-finished input survives. */
    static final class ProjectFiber {
        String fiber = "", percent = "";
        ProjectFiber() {}
        ProjectFiber(String fiber, String percent) { this.fiber = fiber; this.percent = percent; }
    }

    static final class ProjectYarn {
        String name = "", grams = "";
        final List<ProjectFiber> fibers = new ArrayList<>();

        static ProjectYarn fromLibrary(LibraryYarn y) {
            ProjectYarn p = new ProjectYarn();
            p.name = y.name();
            p.setComposition(y.composition());
            return p;
        }

        void setComposition(List<FiberShare> shares) {
            fibers.clear();
            for (FiberShare s : shares) fibers.add(new ProjectFiber(s.fiber(), fmtFlex(s.percent())));
        }

        String label(int index) {
            String t = trf("card_title", index + 1);
            return blank(name) ? t : t + " (" + name.trim() + ")";
        }
    }

    static final class AppData {
        final Settings settings = new Settings();
        final Library library = new Library();
        final List<ProjectYarn> project = new ArrayList<>();

        static AppData fresh() {
            AppData d = new AppData();
            for (String f : T.commonFibers()) d.library.addFiber(f);
            boolean de = T.lang == Lang.DE;
            d.library.putYarn(new LibraryYarn(tr("ex_yarn"), List.of(
                    new FiberShare(de ? "Schurwolle" : "Virgin wool", 75),
                    new FiberShare(de ? "Polyamid" : "Polyamide", 25))));
            d.project.add(new ProjectYarn());
            return d;
        }
    }

    // =====================================================================
    // Calculation
    // =====================================================================

    enum Level { OK, WARN, ERROR }

    record Check(Level level, String message, boolean canFill) {}

    static final double EPS = 0.05;

    static final class Calc {

        static List<ProjectFiber> usedRows(List<ProjectFiber> fibers) {
            List<ProjectFiber> r = new ArrayList<>();
            for (ProjectFiber f : fibers) if (!blank(f.fiber) || !blank(f.percent)) r.add(f);
            return r;
        }

        static Check checkComposition(List<ProjectFiber> fibers) {
            List<ProjectFiber> rows = usedRows(fibers);
            if (rows.isEmpty()) return new Check(Level.WARN, tr("chk_no_fibers"), false);
            double sum = 0;
            List<ProjectFiber> blankRows = new ArrayList<>();
            for (ProjectFiber r : rows) {
                if (blank(r.fiber)) return new Check(Level.ERROR, tr("chk_name_missing"), false);
                if (blank(r.percent)) { blankRows.add(r); continue; }
                Double p = parseNum(r.percent);
                if (p == null) return new Check(Level.ERROR, trf("chk_percent_invalid", r.percent.trim(), r.fiber.trim()), false);
                if (p < 0 || p > 100) return new Check(Level.ERROR, trf("chk_percent_range", r.fiber.trim()), false);
                sum += p;
            }
            double rest = 100 - sum;
            if (!blankRows.isEmpty()) {
                if (blankRows.size() == 1 && rest >= -EPS)
                    return new Check(Level.WARN, trf("chk_percent_blank_fill", blankRows.get(0).fiber.trim(), pct(fmtFlex(Math.max(0, rest)))), true);
                return new Check(Level.WARN, tr("chk_percent_blank"), false);
            }
            if (Math.abs(rest) > EPS) {
                if (rest > 0) return new Check(Level.WARN, trf("chk_sum_low", pct(fmtFlex(sum)), pct(fmtFlex(rest))), true);
                double last = parseNum(rows.get(rows.size() - 1).percent);
                return new Check(Level.ERROR, trf("chk_sum_high", pct(fmtFlex(sum)), pct(fmtFlex(-rest))), last + rest >= 0);
            }
            return new Check(Level.OK, tr("chk_ok"), false);
        }

        static Check checkGrams(String grams) {
            if (blank(grams)) return new Check(Level.WARN, tr("chk_grams_blank"), false);
            Double g = parseNum(grams);
            if (g == null) return new Check(Level.ERROR, trf("chk_grams_invalid", grams.trim()), false);
            if (g < 0) return new Check(Level.ERROR, tr("chk_grams_negative"), false);
            return new Check(Level.OK, "", false);
        }

        /** The most important problem of a yarn; weight problems win ties because the field comes first. */
        static Check checkYarn(ProjectYarn y) {
            Check g = checkGrams(y.grams), c = checkComposition(y.fibers);
            if (g.level().ordinal() >= c.level().ordinal() && g.level() != Level.OK) return g;
            return c;
        }

        /** Shares of a valid composition (blank rows ignored), or null if invalid. */
        static List<FiberShare> toShares(List<ProjectFiber> fibers) {
            if (checkComposition(fibers).level() != Level.OK) return null;
            List<FiberShare> r = new ArrayList<>();
            for (ProjectFiber f : usedRows(fibers)) r.add(new FiberShare(f.fiber.trim(), parseNum(f.percent)));
            return r;
        }

        /**
         * Rounds the shares so that they always add up to exactly 100 (Hare-Niemeyer / largest remainder).
         * A fiber that is present but would round to 0 is shown with the smallest step instead (1 %, or 0.1 %
         * with one decimal). That amount is taken proportionally from the other fibers before they are rounded.
         * Units are whole percent (decimals = 0) or tenths of a percent (decimals = 1).
         */
        static long[] largestRemainder(double[] exactPercents, int decimals) {
            int n = exactPercents.length;
            long unit = decimals == 0 ? 1 : 10;
            long total = 100 * unit;
            boolean[] minimum = new boolean[n];
            long[] units = new long[n];
            while (true) {
                // Fibers held at the minimum get 1 unit each; the rest is shared among the others.
                long reserved = 0;
                double freeSum = 0;
                for (int i = 0; i < n; i++) {
                    if (minimum[i]) reserved++;
                    else freeSum += exactPercents[i];
                }
                if (freeSum <= 0) break;
                double scale = (total - reserved) / freeSum;
                double[] target = new double[n];
                for (int i = 0; i < n; i++) target[i] = minimum[i] ? 0 : exactPercents[i] * scale;
                distribute(target, minimum, total - reserved, units);

                // Any present fiber that still ended up at 0 is raised to the minimum, then everything is redone.
                boolean changed = false;
                for (int i = 0; i < n; i++) {
                    if (!minimum[i] && units[i] == 0 && exactPercents[i] > 0 && reserved + 1 < total) {
                        minimum[i] = true;
                        reserved++;
                        changed = true;
                    }
                }
                if (!changed) break;
            }
            for (int i = 0; i < n; i++) if (minimum[i]) units[i] = 1;
            return units;
        }

        /** Largest remainder: floors every target, then hands the leftover units to the biggest remainders. */
        private static void distribute(double[] target, boolean[] skip, long total, long[] units) {
            int n = target.length;
            double[] rem = new double[n];
            List<Integer> idx = new ArrayList<>();
            long sum = 0;
            for (int i = 0; i < n; i++) {
                units[i] = 0;
                if (skip[i]) continue;
                units[i] = (long) Math.floor(target[i] + 1e-9);
                rem[i] = target[i] - units[i];
                sum += units[i];
                idx.add(i);
            }
            idx.sort((a, b) -> rem[a] != rem[b] ? Double.compare(rem[b], rem[a]) : Double.compare(target[b], target[a]));
            for (int k = 0; k < total - sum && !idx.isEmpty(); k++) units[idx.get(k % idx.size())]++;
        }

        static Result compute(List<ProjectYarn> yarns, Library lib, int decimals) {
            List<String> problems = new ArrayList<>();
            for (int i = 0; i < yarns.size(); i++) {
                Check c = checkYarn(yarns.get(i));
                if (c.level() != Level.OK) problems.add(yarns.get(i).label(i) + ": " + c.message());
            }
            if (yarns.isEmpty() || !problems.isEmpty()) return new Result(yarns.size(), 0, problems, List.of(), new double[0], new long[0], decimals);

            Map<String, Double> grams = new LinkedHashMap<>();
            Map<String, String> names = new HashMap<>();
            double total = 0;
            for (ProjectYarn y : yarns) {
                double g = parseNum(y.grams);
                total += g;
                for (ProjectFiber f : usedRows(y.fibers)) {
                    double w = g * parseNum(f.percent) / 100.0;
                    String k = key(f.fiber);
                    grams.merge(k, w, Double::sum);
                    names.computeIfAbsent(k, kk -> {
                        String known = lib.findFiber(f.fiber);
                        return known != null ? known : capitalize(f.fiber);
                    });
                }
            }
            if (total <= 0) return new Result(yarns.size(), 0, List.of(tr("res_total_zero")), List.of(), new double[0], new long[0], decimals);

            List<Map.Entry<String, Double>> entries = new ArrayList<>();
            for (var e : grams.entrySet()) if (e.getValue() > 0) entries.add(e);
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            int n = entries.size();
            List<String> fiberNames = new ArrayList<>();
            double[] g = new double[n], exact = new double[n];
            for (int i = 0; i < n; i++) {
                fiberNames.add(names.get(entries.get(i).getKey()));
                g[i] = entries.get(i).getValue();
                exact[i] = g[i] / total * 100.0;
            }
            return new Result(yarns.size(), total, List.of(), fiberNames, g, largestRemainder(exact, decimals), decimals);
        }
    }

    record Result(int yarnCount, double totalGrams, List<String> problems, List<String> fibers, double[] grams, long[] units, int decimals) {
        String share(int i) { return fmt(units[i] / (decimals == 0 ? 1.0 : 10.0), decimals); }
        String labelText() {
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < fibers.size(); i++) {
                if (units[i] == 0) continue;
                if (b.length() > 0) b.append(", ");
                b.append(pct(share(i))).append(' ').append(fibers.get(i));
            }
            return b.toString();
        }
    }

    // =====================================================================
    // JSON (small, dependency-free)
    // =====================================================================

    static final class Json {
        static Object parse(String s) { return new Parser(s).top(); }

        private static final class Parser {
            final String s; int i;
            Parser(String s) { this.s = s.startsWith("\uFEFF") ? s.substring(1) : s; }

            Object top() {
                ws(); Object v = value(); ws();
                if (i != s.length()) throw err("unexpected content");
                return v;
            }
            IllegalArgumentException err(String m) { return new IllegalArgumentException("JSON: " + m + " (position " + i + ")"); }
            void ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
            boolean peek(char c) { return i < s.length() && s.charAt(i) == c; }
            void expect(char c) { if (!peek(c)) throw err("expected '" + c + "'"); i++; }

            Object value() {
                if (i >= s.length()) throw err("unexpected end");
                char c = s.charAt(i);
                switch (c) {
                    case '{': return object();
                    case '[': return array();
                    case '"': return string();
                    case 't': literal("true"); return Boolean.TRUE;
                    case 'f': literal("false"); return Boolean.FALSE;
                    case 'n': literal("null"); return null;
                    default: return number();
                }
            }
            void literal(String w) { if (!s.startsWith(w, i)) throw err("invalid literal"); i += w.length(); }

            Map<String, Object> object() {
                i++;
                Map<String, Object> m = new LinkedHashMap<>();
                ws();
                if (peek('}')) { i++; return m; }
                while (true) {
                    ws();
                    if (!peek('"')) throw err("expected a key");
                    String k = string();
                    ws(); expect(':'); ws();
                    m.put(k, value());
                    ws();
                    if (peek(',')) { i++; continue; }
                    expect('}');
                    return m;
                }
            }

            List<Object> array() {
                i++;
                List<Object> l = new ArrayList<>();
                ws();
                if (peek(']')) { i++; return l; }
                while (true) {
                    ws(); l.add(value()); ws();
                    if (peek(',')) { i++; continue; }
                    expect(']');
                    return l;
                }
            }

            String string() {
                i++;
                StringBuilder b = new StringBuilder();
                while (true) {
                    if (i >= s.length()) throw err("unterminated string");
                    char c = s.charAt(i++);
                    if (c == '"') return b.toString();
                    if (c != '\\') { b.append(c); continue; }
                    if (i >= s.length()) throw err("bad escape");
                    char e = s.charAt(i++);
                    switch (e) {
                        case '"', '\\', '/' -> b.append(e);
                        case 'b' -> b.append('\b');
                        case 'f' -> b.append('\f');
                        case 'n' -> b.append('\n');
                        case 'r' -> b.append('\r');
                        case 't' -> b.append('\t');
                        case 'u' -> {
                            if (i + 4 > s.length()) throw err("bad unicode escape");
                            b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                            i += 4;
                        }
                        default -> throw err("bad escape");
                    }
                }
            }

            Double number() {
                int st = i;
                while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
                if (st == i) throw err("unexpected character '" + s.charAt(i) + "'");
                try { return Double.parseDouble(s.substring(st, i)); }
                catch (NumberFormatException e) { throw err("invalid number"); }
            }
        }

        static String write(Object v) {
            StringBuilder b = new StringBuilder();
            write(b, v, 0);
            return b.append('\n').toString();
        }

        private static boolean isSimple(Object v) {
            if (!(v instanceof Map<?, ?> m) || m.size() > 3) return false;
            for (Object o : m.values()) if (o instanceof Map || o instanceof Collection) return false;
            return true;
        }

        private static void write(StringBuilder b, Object v, int ind) {
            if (v == null) b.append("null");
            else if (v instanceof String s) quote(b, s);
            else if (v instanceof Boolean) b.append(v);
            else if (v instanceof Number n) b.append(number(n.doubleValue()));
            else if (v instanceof Map<?, ?> m) {
                if (m.isEmpty()) { b.append("{}"); return; }
                boolean inline = isSimple(m);
                b.append(inline ? "{ " : "{\n");
                int k = 0;
                for (var e : m.entrySet()) {
                    if (!inline) indent(b, ind + 1);
                    quote(b, String.valueOf(e.getKey()));
                    b.append(": ");
                    write(b, e.getValue(), ind + 1);
                    if (++k < m.size()) b.append(inline ? ", " : ",");
                    if (!inline) b.append('\n');
                }
                if (inline) b.append(" }");
                else { indent(b, ind); b.append('}'); }
            } else if (v instanceof Collection<?> c) {
                if (c.isEmpty()) { b.append("[]"); return; }
                b.append("[\n");
                int k = 0;
                for (Object o : c) {
                    indent(b, ind + 1);
                    write(b, o, ind + 1);
                    if (++k < c.size()) b.append(',');
                    b.append('\n');
                }
                indent(b, ind);
                b.append(']');
            } else throw new IllegalArgumentException("Cannot write " + v.getClass());
        }

        private static String number(double d) {
            if (Double.isNaN(d) || Double.isInfinite(d)) return "0";
            return BigDecimal.valueOf(d).stripTrailingZeros().toPlainString();
        }

        private static void indent(StringBuilder b, int n) { b.append("  ".repeat(n)); }

        private static void quote(StringBuilder b, String s) {
            b.append('"');
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> b.append("\\\"");
                    case '\\' -> b.append("\\\\");
                    case '\n' -> b.append("\\n");
                    case '\r' -> b.append("\\r");
                    case '\t' -> b.append("\\t");
                    default -> {
                        if (c < 0x20) b.append(String.format("\\u%04x", (int) c));
                        else b.append(c);
                    }
                }
            }
            b.append('"');
        }

        static String str(Object o) { return o instanceof String s ? s : null; }
        static Double num(Object o) {
            if (o instanceof Double d) return d;
            if (o instanceof String s) return parseNum(s);
            return null;
        }
        static String text(Object o) {
            if (o instanceof String s) return s;
            if (o instanceof Double d) return fmtFlex(d);
            return "";
        }
    }

    // =====================================================================
    // Storage: one versioned data.json in the user's data folder
    // =====================================================================

    static final class Store {
        final Path dir, file, bak;
        private boolean backupDone;

        Store(Path dir) {
            this.dir = dir;
            this.file = dir.resolve("data.json");
            this.bak = dir.resolve("data.json.bak");
        }

        static Path defaultDir() {
            String override = System.getProperty("yarncalc.dir");
            if (!blank(override)) return Paths.get(override).toAbsolutePath();
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            Path home = Paths.get(System.getProperty("user.home"));
            if (os.contains("win")) {
                String appData = System.getenv("APPDATA");
                return (blank(appData) ? home.resolve("AppData").resolve("Roaming") : Paths.get(appData)).resolve("YarnCalculator");
            }
            if (os.contains("mac")) return home.resolve("Library").resolve("Application Support").resolve("YarnCalculator");
            String xdg = System.getenv("XDG_DATA_HOME");
            return (blank(xdg) ? home.resolve(".local").resolve("share") : Paths.get(xdg)).resolve("yarncalculator");
        }

        record Loaded(AppData data, String notice, boolean fresh) {}

        Loaded load() {
            if (Files.exists(file)) {
                try {
                    Map<?, ?> root = asDataFile(Json.parse(Files.readString(file, StandardCharsets.UTF_8)));
                    AppData d = fromJson(root);
                    String notice = Json.num(root.get("version")) > DATA_VERSION ? tr("notice_newer") : null;
                    return new Loaded(d, notice, false);
                } catch (Exception e) {
                    // Keep the damaged file, never overwrite the last good backup with it.
                    backupDone = true;
                    Path broken = dir.resolve("data.broken-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json");
                    try { Files.copy(file, broken, StandardCopyOption.REPLACE_EXISTING); } catch (IOException ignored) {}
                    if (Files.exists(bak)) {
                        try {
                            AppData d = fromJson(asDataFile(Json.parse(Files.readString(bak, StandardCharsets.UTF_8))));
                            return new Loaded(d, trf("notice_restored", broken), false);
                        } catch (Exception ignored) {}
                    }
                    return new Loaded(AppData.fresh(), trf("notice_broken", broken), false);
                }
            }
            // First start of this version: look for data of the old version (yarns.json / fibers.json).
            for (Path legacy : legacyDirs()) {
                if (!Files.exists(legacy.resolve("yarns.json")) && !Files.exists(legacy.resolve("fibers.json"))) continue;
                AppData d = new AppData();
                try {
                    ImportReport r = importInto(d.library, legacy);
                    d.project.add(new ProjectYarn());
                    String notice = trf("notice_migrated", r.added(), d.library.fibers.size(), legacy);
                    try { save(d); } catch (IOException e) { notice += "\n" + trf("st_save_error", e.getMessage()); }
                    return new Loaded(d, notice, false);
                } catch (Exception ignored) {
                    // unreadable old files: fall through to a fresh start
                }
            }
            return new Loaded(AppData.fresh(), null, true);
        }

        static List<Path> legacyDirs() {
            LinkedHashSet<Path> dirs = new LinkedHashSet<>();
            dirs.add(Paths.get("").toAbsolutePath());
            try {
                var cs = YarnCalculator.class.getProtectionDomain().getCodeSource();
                if (cs != null && cs.getLocation() != null) {
                    Path p = Paths.get(cs.getLocation().toURI());
                    Path d = Files.isDirectory(p) ? p : p.getParent();
                    if (d != null) dirs.add(d.toAbsolutePath());
                }
            } catch (Exception ignored) {}
            return new ArrayList<>(dirs);
        }

        void save(AppData d) throws IOException {
            Files.createDirectories(dir);
            String json = Json.write(toJson(d, true));
            if (!backupDone && Files.exists(file)) Files.copy(file, bak, StandardCopyOption.REPLACE_EXISTING);
            backupDone = true;
            Path tmp = dir.resolve("data.json.tmp");
            Files.writeString(tmp, json, StandardCharsets.UTF_8);
            try {
                Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }

        static void export(AppData d, Path target) throws IOException {
            Files.writeString(target, Json.write(toJson(d, true)), StandardCharsets.UTF_8);
        }

        static Map<?, ?> asDataFile(Object root) {
            if (root instanceof Map<?, ?> m && Json.num(m.get("version")) != null && (m.containsKey("yarns") || m.containsKey("fibers")))
                return m;
            throw new IllegalArgumentException("Not a Yarn Calculator data file");
        }

        static Map<String, Object> toJson(AppData d, boolean withProject) {
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("format", "yarncalculator");
            root.put("version", DATA_VERSION);
            root.put("savedAt", LocalDateTime.now().withNano(0).toString());

            Map<String, Object> s = new LinkedHashMap<>();
            s.put("language", d.settings.lang.code);
            s.put("textSize", d.settings.fontScale);
            s.put("decimals", d.settings.decimals);
            s.put("guideSeen", d.settings.guideSeen);
            if (d.settings.bounds != null) {
                Map<String, Object> w = new LinkedHashMap<>();
                w.put("x", d.settings.bounds.x);
                w.put("y", d.settings.bounds.y);
                w.put("width", d.settings.bounds.width);
                w.put("height", d.settings.bounds.height);
                w.put("maximized", d.settings.maximized);
                s.put("window", w);
            }
            root.put("settings", s);

            root.put("fibers", new ArrayList<>(d.library.fibers));

            List<Object> yarns = new ArrayList<>();
            for (LibraryYarn y : d.library.yarns) {
                Map<String, Object> ym = new LinkedHashMap<>();
                ym.put("name", y.name());
                List<Object> comp = new ArrayList<>();
                for (FiberShare f : y.composition()) {
                    Map<String, Object> fm = new LinkedHashMap<>();
                    fm.put("fiber", f.fiber());
                    fm.put("percent", f.percent());
                    comp.add(fm);
                }
                ym.put("composition", comp);
                yarns.add(ym);
            }
            root.put("yarns", yarns);

            if (withProject) {
                List<Object> py = new ArrayList<>();
                for (ProjectYarn y : d.project) {
                    Map<String, Object> ym = new LinkedHashMap<>();
                    ym.put("name", y.name);
                    ym.put("grams", y.grams);
                    List<Object> comp = new ArrayList<>();
                    for (ProjectFiber f : y.fibers) {
                        Map<String, Object> fm = new LinkedHashMap<>();
                        fm.put("fiber", f.fiber);
                        fm.put("percent", f.percent);
                        comp.add(fm);
                    }
                    ym.put("composition", comp);
                    py.add(ym);
                }
                Map<String, Object> project = new LinkedHashMap<>();
                project.put("yarns", py);
                root.put("project", project);
            }
            return root;
        }

        static AppData fromJson(Map<?, ?> m) {
            AppData d = new AppData();
            if (m.get("settings") instanceof Map<?, ?> s) {
                d.settings.lang = Lang.fromCode(Json.str(s.get("language")));
                Double ts = Json.num(s.get("textSize"));
                if (ts != null) d.settings.fontScale = Math.max(80, Math.min(250, ts.intValue()));
                Double dec = Json.num(s.get("decimals"));
                if (dec != null) d.settings.decimals = dec.intValue() == 0 ? 0 : 1;
                d.settings.guideSeen = Boolean.TRUE.equals(s.get("guideSeen"));
                if (s.get("window") instanceof Map<?, ?> w) {
                    Double x = Json.num(w.get("x")), y = Json.num(w.get("y")), wi = Json.num(w.get("width")), he = Json.num(w.get("height"));
                    if (x != null && y != null && wi != null && he != null)
                        d.settings.bounds = new Rectangle(x.intValue(), y.intValue(), wi.intValue(), he.intValue());
                    d.settings.maximized = Boolean.TRUE.equals(w.get("maximized"));
                }
            }
            if (m.get("fibers") instanceof List<?> l)
                for (Object o : l) if (o instanceof String f) d.library.addFiber(f);
            if (m.get("yarns") instanceof List<?> l) {
                for (Object o : l) {
                    if (!(o instanceof Map<?, ?> ym)) continue;
                    String name = Json.str(ym.get("name"));
                    List<FiberShare> comp = new ArrayList<>();
                    if (ym.get("composition") instanceof List<?> cl)
                        for (Object c : cl)
                            if (c instanceof Map<?, ?> cm && !blank(Json.str(cm.get("fiber"))) && Json.num(cm.get("percent")) != null)
                                comp.add(new FiberShare(Json.str(cm.get("fiber")).trim(), Json.num(cm.get("percent"))));
                    if (!blank(name) && !comp.isEmpty()) d.library.putYarn(new LibraryYarn(name.trim(), comp));
                }
            }
            if (m.get("project") instanceof Map<?, ?> p && p.get("yarns") instanceof List<?> l) {
                for (Object o : l) {
                    if (!(o instanceof Map<?, ?> ym)) continue;
                    ProjectYarn y = new ProjectYarn();
                    y.name = Json.text(ym.get("name"));
                    y.grams = Json.text(ym.get("grams"));
                    if (ym.get("composition") instanceof List<?> cl)
                        for (Object c : cl)
                            if (c instanceof Map<?, ?> cm) y.fibers.add(new ProjectFiber(Json.text(cm.get("fiber")), Json.text(cm.get("percent"))));
                    d.project.add(y);
                }
            }
            return d;
        }

        // ---- Import (new backups and files of the old version) ----

        record ImportReport(int added, int renamed, int skipped, int fibersAdded) {}

        /** Merges a backup file, an old yarns.json/fibers.json, or a folder containing them into the library. Never overwrites. */
        static ImportReport importInto(Library lib, Path source) throws IOException {
            List<LibraryYarn> yarns = new ArrayList<>();
            List<String> fibers = new ArrayList<>();
            Path legacyDir = null;
            if (Files.isDirectory(source)) legacyDir = source;
            else {
                String text = Files.readString(source, StandardCharsets.UTF_8);
                Object root = null;
                try { root = Json.parse(text); } catch (IllegalArgumentException ignored) {}
                if (root instanceof Map<?, ?> m && m.containsKey("version")) {
                    AppData d = fromJson(asDataFile(m));
                    yarns.addAll(d.library.yarns);
                    fibers.addAll(d.library.fibers);
                } else {
                    legacyDir = source.toAbsolutePath().getParent();
                    String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
                    if (!name.equals("yarns.json") && !name.equals("fibers.json")) {
                        // an old-format file with a different name: read just this one
                        if (text.trim().startsWith("[")) fibers.addAll(legacyFibers(text));
                        else yarns.addAll(legacyYarns(text));
                        legacyDir = null;
                    }
                }
            }
            if (legacyDir != null) {
                Path y = legacyDir.resolve("yarns.json"), f = legacyDir.resolve("fibers.json");
                if (Files.exists(y)) yarns.addAll(legacyYarns(Files.readString(y, StandardCharsets.UTF_8)));
                if (Files.exists(f)) fibers.addAll(legacyFibers(Files.readString(f, StandardCharsets.UTF_8)));
            }
            if (yarns.isEmpty() && fibers.isEmpty()) throw new IllegalArgumentException(tr("imp_nothing"));

            int added = 0, renamed = 0, skipped = 0, fibersAdded = 0;
            for (LibraryYarn y : yarns) {
                LibraryYarn existing = lib.find(y.name());
                if (existing == null) { lib.putYarn(y); added++; }
                else if (sameComposition(existing.composition(), y.composition())) skipped++;
                else { lib.putYarn(new LibraryYarn(lib.uniqueName(y.name()), y.composition())); renamed++; }
                for (FiberShare s : y.composition()) if (lib.addFiber(s.fiber())) fibersAdded++;
            }
            for (String f : fibers) if (lib.addFiber(f)) fibersAdded++;
            return new ImportReport(added, renamed, skipped, fibersAdded);
        }

        /** Old format: {"Name": {"f0": {"name": "Wool", "percentage": 75.00}, ...}, ...} */
        static List<LibraryYarn> legacyYarns(String text) {
            List<LibraryYarn> res = new ArrayList<>();
            Object root;
            try { root = Json.parse(text); } catch (IllegalArgumentException e) { return legacyYarnsLenient(text); }
            if (!(root instanceof Map<?, ?> m)) return res;
            for (var e : m.entrySet()) {
                if (!(e.getValue() instanceof Map<?, ?> fm)) continue;
                List<FiberShare> comp = new ArrayList<>();
                for (Object fo : fm.values()) {
                    if (!(fo instanceof Map<?, ?> f)) continue;
                    String n = Json.str(f.get("name"));
                    Double p = Json.num(f.get("percentage"));
                    if (!blank(n) && p != null) comp.add(new FiberShare(n.trim(), p));
                }
                String name = String.valueOf(e.getKey()).trim();
                if (!name.isEmpty() && !comp.isEmpty()) res.add(new LibraryYarn(name, comp));
            }
            return res;
        }

        /** The old version did not escape quotes in names; this reads such files the way the old version did. */
        static List<LibraryYarn> legacyYarnsLenient(String json) {
            List<LibraryYarn> res = new ArrayList<>();
            json = json.trim();
            if (json.startsWith("{") && json.endsWith("}")) json = json.substring(1, json.length() - 1);
            for (String block : splitTopLevel(json)) {
                int idx = block.indexOf(':');
                if (idx < 0) continue;
                String name = block.substring(0, idx).trim().replace("\"", "");
                String val = block.substring(idx + 1).trim();
                if (val.startsWith("{") && val.endsWith("}")) val = val.substring(1, val.length() - 1);
                List<FiberShare> comp = new ArrayList<>();
                for (String fb : splitTopLevel(val)) {
                    int i2 = fb.indexOf(':');
                    if (i2 < 0) continue;
                    String props = fb.substring(i2 + 1);
                    String n = extract(props, "name"), p = extract(props, "percentage");
                    if (n != null && p != null && parseNum(p) != null) comp.add(new FiberShare(n, parseNum(p)));
                }
                if (!name.isEmpty() && !comp.isEmpty()) res.add(new LibraryYarn(name, comp));
            }
            return res;
        }

        static List<String> legacyFibers(String text) {
            List<String> res = new ArrayList<>();
            try {
                if (Json.parse(text) instanceof List<?> l) {
                    for (Object o : l) if (o instanceof String s && !s.isBlank()) res.add(s.trim());
                    return res;
                }
            } catch (IllegalArgumentException ignored) {}
            String t = text.trim();
            if (t.startsWith("[") && t.endsWith("]")) {
                for (String part : t.substring(1, t.length() - 1).split(",")) {
                    String c = part.trim().replace("\"", "");
                    if (!c.isEmpty()) res.add(c);
                }
            }
            return res;
        }

        private static String extract(String s, String key) {
            Matcher m = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\"},]+)\"?").matcher(s);
            return m.find() ? m.group(1).trim() : null;
        }

        private static List<String> splitTopLevel(String s) {
            List<String> r = new ArrayList<>();
            int depth = 0;
            boolean quoted = false;
            StringBuilder b = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '"') quoted = !quoted;
                else if (!quoted) {
                    if (c == '{') depth++;
                    else if (c == '}') depth--;
                    else if (c == ',' && depth == 0) { r.add(b.toString().trim()); b.setLength(0); continue; }
                }
                b.append(c);
            }
            if (b.length() > 0) r.add(b.toString().trim());
            return r;
        }
    }

    // =====================================================================
    // UI helpers
    // =====================================================================

    static final class Ui {
        static final Color OK = new Color(0x2E7D32), WARN = new Color(0xC77700), ERR = new Color(0xC62828), MUTED = new Color(0x6E6E6E);
        static final Color ERR_BG = new Color(0xFDE4E4);
        static final Color CARD_BORDER = new Color(0xC9CDD4);
        static final Color[] PALETTE = {
                new Color(0x4E79A7), new Color(0xF28E2B), new Color(0x59A14F), new Color(0xE15759), new Color(0x76B7B2),
                new Color(0xEDC948), new Color(0xB07AA1), new Color(0xFF9DA7), new Color(0x9C755F), new Color(0xBAB0AC)};

        static void installLookAndFeel(int scale) {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            String cls = (os.contains("win") || os.contains("mac"))
                    ? UIManager.getSystemLookAndFeelClassName() : "javax.swing.plaf.nimbus.NimbusLookAndFeel";
            try { UIManager.setLookAndFeel(cls); }
            catch (Exception e) {
                try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
            }
            UIDefaults d = UIManager.getLookAndFeelDefaults();
            float f = scale / 100f;
            for (Object k : new ArrayList<>(d.keySet())) {
                Object v = d.get(k);
                if (v instanceof Font font) UIManager.put(k, new FontUIResource(font.deriveFont(font.getSize2D() * f)));
            }
            if (d.get("defaultFont") instanceof Font font) d.put("defaultFont", new FontUIResource(font.deriveFont(font.getSize2D() * f)));
            ToolTipManager.sharedInstance().setInitialDelay(400);
        }

        static void applyLanguage() {
            JComponent.setDefaultLocale(T.lang.locale);
            UIManager.put("OptionPane.yesButtonText", tr("yes"));
            UIManager.put("OptionPane.noButtonText", tr("no"));
            UIManager.put("OptionPane.cancelButtonText", tr("btn_cancel"));
            UIManager.put("OptionPane.okButtonText", tr("btn_ok"));
        }

        static int fontSize() { return UIManager.getFont("Label.font").getSize(); }

        static int px(int base) { return Math.round(base * fontSize() / 13f); }

        static JButton button(String text, Runnable action) {
            JButton b = new JButton(text);
            b.addActionListener(e -> action.run());
            return b;
        }

        static JButton iconButton(String text, String tooltip, Runnable action) {
            JButton b = button(text, action);
            b.setToolTipText(tooltip);
            b.setMargin(new Insets(0, px(6), 0, px(6)));
            b.setFont(b.getFont().deriveFont(Font.BOLD, b.getFont().getSize2D() * 1.15f));
            return b;
        }

        static void onChange(JTextComponent c, Runnable r) {
            c.getDocument().addDocumentListener(new DocumentListener() {
                public void insertUpdate(DocumentEvent e) { r.run(); }
                public void removeUpdate(DocumentEvent e) { r.run(); }
                public void changedUpdate(DocumentEvent e) { r.run(); }
            });
        }

        static void markInvalid(JTextField f, boolean bad) {
            Color base = (Color) f.getClientProperty("yc.baseBg");
            if (base == null) { base = f.getBackground(); f.putClientProperty("yc.baseBg", base); }
            f.setBackground(bad ? ERR_BG : base);
        }

        static void selectAllOnFocus(JTextComponent c) {
            c.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(c::selectAll); }
            });
        }

        static Color color(Level l) { return switch (l) { case OK -> OK; case WARN -> WARN; case ERROR -> ERR; }; }

        static String esc(String s) {
            return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
        }

        static JLabel title(String text, float scale) {
            JLabel l = new JLabel(text);
            l.setFont(l.getFont().deriveFont(Font.BOLD, l.getFont().getSize2D() * scale));
            return l;
        }

        static JLabel muted(String text) {
            JLabel l = new JLabel(text);
            l.setForeground(MUTED);
            return l;
        }

        static JLabel wrapped(String text, int width) {
            return new JLabel("<html><body style='width:" + width + "px'>" + esc(text).replace("\n", "<br>") + "</body></html>");
        }

        static void copy(String text) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(text), null);
        }

        static boolean confirm(Component parent, String message, String okLabel, boolean warning) {
            Object[] options = {okLabel, tr("btn_cancel")};
            int r = JOptionPane.showOptionDialog(parent, message, tr("app_title"), JOptionPane.OK_CANCEL_OPTION,
                    warning ? JOptionPane.WARNING_MESSAGE : JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
            return r == 0;
        }

        static void info(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, tr("app_title"), JOptionPane.INFORMATION_MESSAGE);
        }

        static void error(Component parent, String message) {
            JOptionPane.showMessageDialog(parent, message, tr("app_title"), JOptionPane.ERROR_MESSAGE);
        }

        static String ask(Component parent, String message, String initial) {
            Object r = JOptionPane.showInputDialog(parent, message, tr("app_title"), JOptionPane.QUESTION_MESSAGE, null, null, initial);
            return r == null || r.toString().isBlank() ? null : r.toString().trim();
        }

        static void open(URI uri, boolean browse) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop dt = Desktop.getDesktop();
                    if (browse && dt.isSupported(Desktop.Action.BROWSE)) { dt.browse(uri); return; }
                    if (!browse && dt.isSupported(Desktop.Action.OPEN)) { dt.open(Paths.get(uri).toFile()); return; }
                }
                String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
                String target = browse ? uri.toString() : Paths.get(uri).toString();
                if (os.contains("win")) new ProcessBuilder("explorer", target).start();
                else if (os.contains("mac")) new ProcessBuilder("open", target).start();
                else new ProcessBuilder("xdg-open", target).start();
            } catch (Exception ignored) {
                // nothing else we can do; the path / URL is shown in the UI anyway
            }
        }

        static void closeOnEscape(JDialog d) {
            d.getRootPane().registerKeyboardAction(e -> d.dispose(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
                    JComponent.WHEN_IN_FOCUSED_WINDOW);
        }

        static GridBagConstraints gbc(int x, int y) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = x; c.gridy = y;
            c.anchor = GridBagConstraints.WEST;
            c.insets = new Insets(px(2), 0, px(2), px(6));
            return c;
        }
    }

    /** Text field that shows a grey hint while empty. */
    static final class HintField extends JTextField {
        private final String hint;
        HintField(int columns, String hint) {
            super(columns);
            this.hint = hint;
            addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { repaint(); }
                @Override public void focusLost(FocusEvent e) { repaint(); }
            });
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!getText().isEmpty() || blank(hint) || isFocusOwner()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Ui.MUTED);
            g2.setFont(getFont().deriveFont(Font.ITALIC));
            Insets in = getInsets();
            FontMetrics fm = g2.getFontMetrics();
            int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
            int x = getHorizontalAlignment() == RIGHT || getHorizontalAlignment() == TRAILING
                    ? getWidth() - in.right - fm.stringWidth(hint) - 2 : in.left + 2;
            g2.drawString(hint, x, y);
            g2.dispose();
        }
    }

    /** Round status icon (check mark or exclamation mark); drawn, so it works with every font. */
    static final class StatusIcon implements Icon {
        private final Level level;
        private final int size;
        StatusIcon(Level level) { this.level = level; this.size = Math.max(12, Ui.fontSize() + 1); }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Ui.color(level));
            g2.fill(new Ellipse2D.Float(x, y, size - 1, size - 1));
            g2.setColor(Color.WHITE);
            float s = size;
            g2.setStroke(new BasicStroke(Math.max(1.6f, s / 7f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            if (level == Level.OK) {
                Path2D p = new Path2D.Float();
                p.moveTo(x + s * .27f, y + s * .52f);
                p.lineTo(x + s * .43f, y + s * .68f);
                p.lineTo(x + s * .72f, y + s * .34f);
                g2.draw(p);
            } else {
                g2.draw(new Line2D.Float(x + s / 2f - .5f, y + s * .24f, x + s / 2f - .5f, y + s * .54f));
                float d = s * .15f;
                g2.fill(new Ellipse2D.Float(x + s / 2f - .5f - d / 2, y + s * .66f, d, d));
            }
            g2.dispose();
        }
    }

    /** Small colored dot / square. */
    static final class ColorIcon implements Icon {
        private final Color color; private final boolean round; private final int size;
        ColorIcon(Color color, boolean round) { this.color = color; this.round = round; this.size = Math.max(8, Ui.fontSize() - 2); }
        public int getIconWidth() { return size; }
        public int getIconHeight() { return size; }
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            if (round) g2.fill(new Ellipse2D.Float(x, y, size - 1, size - 1));
            else g2.fill(new RoundRectangle2D.Float(x, y, size - 1, size - 1, 3, 3));
            g2.dispose();
        }
    }

    /** Autocompletion for an editable combo box: completes from the list while the user types. */
    static void installAutoComplete(JComboBox<String> combo) {
        JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
        editor.setDocument(new PlainDocument() {
            boolean busy;
            @Override public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                super.insertString(offs, str, a);
                if (busy || str == null || !editor.isFocusOwner() || offs + str.length() != getLength()) return;
                String typed = getText(0, getLength());
                if (typed.isEmpty()) return;
                String match = null;
                ComboBoxModel<String> m = combo.getModel();
                for (int i = 0; i < m.getSize(); i++) {
                    String item = m.getElementAt(i);
                    if (item == null) continue;
                    if (item.equalsIgnoreCase(typed)) return;
                    if (match == null && item.toLowerCase(Locale.ROOT).startsWith(typed.toLowerCase(Locale.ROOT))) match = item;
                }
                if (match == null) return;
                busy = true;
                try {
                    super.remove(0, getLength());
                    super.insertString(0, match, a);
                } finally { busy = false; }
                editor.setCaretPosition(getLength());
                editor.moveCaretPosition(typed.length());
            }
        });
    }

    /** Panel that always fits the width of its scroll pane (no horizontal scrolling). */
    static final class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager lm) { super(lm); }
        public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return Ui.px(24); }
        public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return r.height - Ui.px(24); }
        public boolean getScrollableTracksViewportWidth() { return true; }
        public boolean getScrollableTracksViewportHeight() {
            return getParent() instanceof JViewport v && v.getHeight() > getPreferredSize().height;
        }
    }

    // =====================================================================
    // Composition editor (used in project cards and in the library editor)
    // =====================================================================

    static final class CompositionEditor extends JPanel {
        private final List<ProjectFiber> fibers;
        private final Library lib;
        private final Runnable onChange;
        private final JPanel grid = new JPanel(new GridBagLayout());
        private final Map<ProjectFiber, JTextField> percentFields = new HashMap<>();
        private final List<JComboBox<String>> combos = new ArrayList<>();

        CompositionEditor(List<ProjectFiber> fibers, Library lib, Runnable onChange) {
            super(new BorderLayout());
            this.fibers = fibers;
            this.lib = lib;
            this.onChange = onChange;
            setOpaque(false);
            grid.setOpaque(false);
            if (fibers.isEmpty()) fibers.add(new ProjectFiber());
            buildRows();
            add(grid, BorderLayout.CENTER);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            actions.setOpaque(false);
            actions.add(Ui.button(tr("btn_add_fiber"), this::addRow));
            add(actions, BorderLayout.SOUTH);
        }

        private void buildRows() {
            grid.removeAll();
            percentFields.clear();
            combos.clear();
            GridBagConstraints h = Ui.gbc(0, 0);
            grid.add(Ui.muted(tr("hdr_fiber")), h);
            h = Ui.gbc(1, 0);
            h.gridwidth = 2;
            grid.add(Ui.muted(tr("hdr_share")), h);
            String[] items = lib.fibers.toArray(String[]::new);
            for (int i = 0; i < fibers.size(); i++) {
                ProjectFiber f = fibers.get(i);
                int row = i + 1;

                JComboBox<String> combo = new JComboBox<>(items);
                combo.setEditable(true);
                combo.setMaximumRowCount(16);
                combo.setPrototypeDisplayValue("XXXXXXXXXXXXXXXXXXXX");
                combo.setToolTipText(tr("tip_fiber"));
                installAutoComplete(combo);
                combo.setSelectedItem(f.fiber);
                JTextField editor = (JTextField) combo.getEditor().getEditorComponent();
                editor.setText(f.fiber);
                Ui.onChange(editor, () -> { f.fiber = editor.getText(); onChange.run(); });
                combos.add(combo);
                GridBagConstraints c = Ui.gbc(0, row);
                c.fill = GridBagConstraints.HORIZONTAL;
                grid.add(combo, c);

                JTextField percent = new JTextField(f.percent, 5);
                percent.setHorizontalAlignment(JTextField.RIGHT);
                Ui.selectAllOnFocus(percent);
                Ui.onChange(percent, () -> { f.percent = percent.getText(); markPercent(percent); onChange.run(); });
                markPercent(percent);
                percentFields.put(f, percent);
                c = Ui.gbc(1, row);
                c.insets.right = Ui.px(2);
                grid.add(percent, c);
                grid.add(new JLabel("%"), Ui.gbc(2, row));

                JButton remove = Ui.iconButton("×", tr("tip_remove_fiber"), () -> removeRow(f));
                grid.add(remove, Ui.gbc(3, row));
            }
            GridBagConstraints filler = Ui.gbc(4, 0);
            filler.weightx = 1;
            grid.add(Box.createHorizontalGlue(), filler);
            grid.revalidate();
            grid.repaint();
        }

        private static void markPercent(JTextField f) {
            String t = f.getText();
            Double p = parseNum(t);
            Ui.markInvalid(f, !blank(t) && (p == null || p < 0 || p > 100));
        }

        void addRow() {
            fibers.add(new ProjectFiber());
            buildRows();
            onChange.run();
            focusFiber(fibers.size() - 1);
        }

        void removeRow(ProjectFiber f) {
            fibers.remove(f);
            if (fibers.isEmpty()) fibers.add(new ProjectFiber());
            buildRows();
            onChange.run();
        }

        void focusFiber(int i) {
            if (i < 0 || i >= combos.size()) return;
            SwingUtilities.invokeLater(() -> combos.get(i).getEditor().getEditorComponent().requestFocusInWindow());
        }

        /** Puts the missing share into the empty row, or adjusts the last row. */
        void fillUp() {
            List<ProjectFiber> rows = Calc.usedRows(fibers);
            if (rows.isEmpty()) return;
            double sum = 0;
            ProjectFiber blankRow = null;
            for (ProjectFiber r : rows) {
                if (blank(r.percent)) { blankRow = r; continue; }
                Double p = parseNum(r.percent);
                if (p == null) return;
                sum += p;
            }
            double rest = 100 - sum;
            ProjectFiber target = blankRow != null ? blankRow : rows.get(rows.size() - 1);
            double base = blankRow != null ? 0 : parseNum(target.percent);
            double value = Math.round((base + rest) * 100) / 100.0;
            if (value < 0) return;
            JTextField field = percentFields.get(target);
            if (field != null) field.setText(fmtFlex(value));
            else { target.percent = fmtFlex(value); onChange.run(); }
        }
    }

    // =====================================================================
    // Project card (one yarn in the project)
    // =====================================================================

    static final class YarnCard extends JPanel {
        final ProjectYarn yarn;
        final int index;
        private final MainFrame app;
        final HintField name, grams;
        private final JLabel libState = new JLabel();
        private final JButton saveLib;
        private final CompositionEditor comp;
        private final JLabel status = new JLabel();
        private final JButton fill;
        private final int gap = Ui.px(10);

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            float arc = Ui.px(14);
            RoundRectangle2D shape = new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1.5f, getHeight() - gap - 1.5f, arc, arc);
            g2.setColor(getBackground());
            g2.fill(shape);
            g2.setColor(Ui.CARD_BORDER);
            g2.draw(shape);
            g2.dispose();
        }

        YarnCard(MainFrame app, ProjectYarn yarn, int index) {
            super(new BorderLayout(0, Ui.px(6)));
            this.app = app;
            this.yarn = yarn;
            this.index = index;
            Color bg = UIManager.getColor("TextField.background");
            setBackground(bg != null ? new Color(bg.getRGB()) : Color.WHITE);
            setOpaque(false);
            setBorder(new EmptyBorder(Ui.px(8), Ui.px(12), Ui.px(10) + gap, Ui.px(8)));

            // Header: title + remove button
            JPanel header = new JPanel(new BorderLayout());
            header.setOpaque(false);
            header.add(Ui.title(trf("card_title", index + 1), 1.15f), BorderLayout.WEST);
            header.add(Ui.iconButton("×", tr("tip_remove_yarn"), () -> app.removeYarn(index)), BorderLayout.EAST);
            add(header, BorderLayout.NORTH);

            JPanel body = new JPanel(new GridBagLayout());
            body.setOpaque(false);

            name = new HintField(18, tr("hint_name"));
            name.setText(yarn.name);
            Ui.onChange(name, () -> { yarn.name = name.getText(); app.changed(); });
            grams = new HintField(5, "0");
            grams.setHorizontalAlignment(JTextField.RIGHT);
            grams.setText(yarn.grams);
            Ui.selectAllOnFocus(grams);
            Ui.onChange(grams, () -> { yarn.grams = grams.getText(); markGrams(); app.changed(); });
            markGrams();

            body.add(new JLabel(tr("lbl_name")), Ui.gbc(0, 0));
            GridBagConstraints c = Ui.gbc(1, 0);
            c.fill = GridBagConstraints.HORIZONTAL;
            body.add(name, c);
            c = Ui.gbc(2, 0);
            c.insets.left = Ui.px(10);
            body.add(new JLabel(tr("lbl_weight")), c);
            c = Ui.gbc(3, 0);
            c.insets.right = Ui.px(2);
            body.add(grams, c);
            body.add(new JLabel(tr("unit_g")), Ui.gbc(4, 0));
            GridBagConstraints filler = Ui.gbc(5, 0);
            filler.weightx = 1;
            body.add(Box.createHorizontalGlue(), filler);

            JPanel libRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            libRow.setOpaque(false);
            JButton fromLib = Ui.button(tr("btn_from_lib"), () -> app.loadFromLibrary(index));
            fromLib.setToolTipText(tr("tip_from_lib"));
            saveLib = Ui.button(tr("btn_save_lib"), () -> app.saveToLibrary(index));
            saveLib.setToolTipText(tr("tip_save_lib"));
            libState.setIconTextGap(Ui.px(5));
            libRow.add(fromLib);
            libRow.add(Box.createHorizontalStrut(Ui.px(4)));
            libRow.add(saveLib);
            libRow.add(Box.createHorizontalStrut(Ui.px(10)));
            libRow.add(libState);
            c = Ui.gbc(1, 1);
            c.gridwidth = 5;
            body.add(libRow, c);

            comp = new CompositionEditor(yarn.fibers, app.data.library, app::changed);
            comp.setBorder(new EmptyBorder(Ui.px(6), 0, 0, 0));
            c = Ui.gbc(0, 2);
            c.gridwidth = 6;
            c.fill = GridBagConstraints.HORIZONTAL;
            body.add(comp, c);
            add(body, BorderLayout.CENTER);

            JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            statusRow.setOpaque(false);
            status.setIconTextGap(Ui.px(6));
            fill = Ui.button(tr("btn_fill"), comp::fillUp);
            fill.setToolTipText(tr("tip_fill"));
            statusRow.add(status);
            statusRow.add(Box.createHorizontalStrut(Ui.px(8)));
            statusRow.add(fill);
            add(statusRow, BorderLayout.SOUTH);

            refreshStatus();
        }

        private void markGrams() {
            Double g = parseNum(grams.getText());
            Ui.markInvalid(grams, !blank(grams.getText()) && (g == null || g < 0));
        }

        void refreshStatus() {
            Check check = Calc.checkYarn(yarn);
            status.setIcon(new StatusIcon(check.level()));
            status.setText(check.message());
            status.setForeground(check.level() == Level.OK ? Ui.OK : check.level() == Level.WARN ? Ui.WARN.darker() : Ui.ERR);
            Check compCheck = Calc.checkComposition(yarn.fibers);
            fill.setVisible(compCheck.canFill() && check.equals(compCheck));

            LibraryYarn saved = blank(yarn.name) ? null : app.data.library.find(yarn.name);
            List<FiberShare> shares = Calc.toShares(yarn.fibers);
            if (blank(yarn.name)) {
                libState.setText("");
                libState.setIcon(null);
                saveLib.setText(tr("btn_save_lib"));
                saveLib.setEnabled(true);
            } else if (saved == null) {
                libState.setText(tr("lib_state_new"));
                libState.setIcon(new ColorIcon(Ui.CARD_BORDER, true));
                libState.setForeground(Ui.MUTED);
                saveLib.setText(tr("btn_save_lib"));
                saveLib.setEnabled(true);
            } else if (shares != null && sameComposition(saved.composition(), shares)) {
                libState.setText(tr("lib_state_saved"));
                libState.setIcon(new ColorIcon(Ui.OK, true));
                libState.setForeground(Ui.MUTED);
                saveLib.setText(tr("btn_saved_lib"));
                saveLib.setEnabled(false);
            } else {
                libState.setText(tr("lib_state_changed"));
                libState.setIcon(new ColorIcon(Ui.WARN, true));
                libState.setForeground(Ui.WARN.darker());
                saveLib.setText(tr("btn_update_lib"));
                saveLib.setEnabled(true);
            }
        }

        void focusFirstEmpty() {
            JComponent target = blank(yarn.name) ? name : blank(yarn.grams) ? grams : null;
            if (target != null) SwingUtilities.invokeLater(target::requestFocusInWindow);
            else comp.focusFiber(0);
        }
    }

    // =====================================================================
    // Result panel
    // =====================================================================

    static final class StackBar extends JComponent {
        private double[] values = new double[0];
        StackBar() { setPreferredSize(new Dimension(Ui.px(100), Ui.px(26))); }
        void setValues(double[] v) { values = v; repaint(); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            double total = Arrays.stream(values).sum();
            g2.setClip(new RoundRectangle2D.Float(0, 0, w, h, Ui.px(10), Ui.px(10)));
            g2.setColor(Ui.CARD_BORDER);
            g2.fillRect(0, 0, w, h);
            if (total > 0) {
                double x = 0;
                for (int i = 0; i < values.length; i++) {
                    double sw = values[i] / total * w;
                    g2.setColor(Ui.PALETTE[i % Ui.PALETTE.length]);
                    g2.fill(new Rectangle.Double(x, 0, sw + 0.5, h));
                    x += sw;
                }
                g2.setColor(Color.WHITE);
                x = 0;
                for (int i = 0; i < values.length - 1; i++) {
                    x += values[i] / total * w;
                    g2.fill(new Rectangle.Double(x - 0.75, 0, 1.5, h));
                }
            }
            g2.dispose();
        }
    }

    static final class ResultPanel extends JPanel {
        private final MainFrame app;
        private final JLabel total = new JLabel(" ");
        private final CardLayout cards = new CardLayout();
        private final JPanel cardPanel = new JPanel(cards);
        private final JLabel todo = new JLabel();
        private final StackBar bar = new StackBar();
        private final JTextArea label = new JTextArea(3, 20);
        private final ResultTableModel model = new ResultTableModel();
        private Result last;

        ResultPanel(MainFrame app) {
            super(new BorderLayout(0, Ui.px(8)));
            this.app = app;
            setBorder(new EmptyBorder(Ui.px(12), Ui.px(12), Ui.px(12), Ui.px(12)));

            JPanel head = new JPanel(new GridLayout(0, 1));
            head.setOpaque(false);
            head.add(Ui.title(tr("res_title"), 1.35f));
            total.setForeground(Ui.MUTED);
            head.add(total);
            add(head, BorderLayout.NORTH);

            // empty
            JPanel empty = new JPanel(new GridBagLayout());
            JLabel emptyText = new JLabel("<html><div style='text-align:center'>"
                    + Ui.esc(tr("res_empty")).replace("\n", "<br>") + "</div></html>");
            emptyText.setForeground(Ui.MUTED);
            empty.add(emptyText);
            cardPanel.add(empty, "empty");

            // to-do list
            JPanel todoPanel = new JPanel(new BorderLayout());
            todo.setVerticalAlignment(SwingConstants.TOP);
            todoPanel.add(todo, BorderLayout.NORTH);
            cardPanel.add(new JScrollPane(todoPanel) {{ setBorder(null); }}, "todo");

            // result
            JPanel res = new JPanel(new BorderLayout(0, Ui.px(10)));
            res.add(bar, BorderLayout.NORTH);
            JTable table = new JTable(model);
            table.setRowHeight(Ui.fontSize() + Ui.px(10));
            table.setFillsViewportHeight(true);
            table.setFocusable(false);
            table.setRowSelectionAllowed(false);
            table.getTableHeader().setReorderingAllowed(false);
            table.setShowVerticalLines(false);
            table.setIntercellSpacing(new Dimension(0, 1));
            DefaultTableCellRenderer fiberRenderer = new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                    super.getTableCellRendererComponent(t, v, false, false, row, col);
                    setIcon(new ColorIcon(Ui.PALETTE[row % Ui.PALETTE.length], false));
                    setIconTextGap(Ui.px(8));
                    setBorder(new EmptyBorder(0, Ui.px(6), 0, 0));
                    return this;
                }
            };
            DefaultTableCellRenderer numRenderer = new DefaultTableCellRenderer() {
                @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int row, int col) {
                    super.getTableCellRendererComponent(t, v, false, false, row, col);
                    setHorizontalAlignment(RIGHT);
                    setBorder(new EmptyBorder(0, 0, 0, Ui.px(8)));
                    if (col == 1) setFont(getFont().deriveFont(Font.BOLD));
                    return this;
                }
            };
            table.getColumnModel().getColumn(0).setCellRenderer(fiberRenderer);
            table.getColumnModel().getColumn(1).setCellRenderer(numRenderer);
            table.getColumnModel().getColumn(2).setCellRenderer(numRenderer);
            table.getColumnModel().getColumn(0).setPreferredWidth(Ui.px(170));
            table.getColumnModel().getColumn(1).setPreferredWidth(Ui.px(95));
            table.getColumnModel().getColumn(2).setPreferredWidth(Ui.px(85));
            res.add(new JScrollPane(table), BorderLayout.CENTER);

            JPanel bottom = new JPanel();
            bottom.setLayout(new BoxLayout(bottom, BoxLayout.Y_AXIS));

            JPanel roundRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            JComboBox<String> round = new JComboBox<>(new String[]{tr("round_0"), tr("round_1")});
            round.setSelectedIndex(app.data.settings.decimals);
            round.addActionListener(e -> { app.data.settings.decimals = round.getSelectedIndex(); app.changed(); });
            roundRow.add(new JLabel(tr("lbl_round") + "  "));
            roundRow.add(round);
            roundRow.setAlignmentX(LEFT_ALIGNMENT);
            bottom.add(roundRow);
            bottom.add(Box.createVerticalStrut(Ui.px(12)));

            JLabel labelTitle = Ui.muted(tr("lbl_label_text"));
            labelTitle.setAlignmentX(LEFT_ALIGNMENT);
            bottom.add(labelTitle);
            bottom.add(Box.createVerticalStrut(Ui.px(3)));
            label.setEditable(false);
            label.setLineWrap(true);
            label.setWrapStyleWord(true);
            label.setFont(UIManager.getFont("Label.font"));
            label.setBorder(new EmptyBorder(Ui.px(6), Ui.px(8), Ui.px(6), Ui.px(8)));
            JScrollPane labelScroll = new JScrollPane(label);
            labelScroll.setAlignmentX(LEFT_ALIGNMENT);
            bottom.add(labelScroll);
            bottom.add(Box.createVerticalStrut(Ui.px(8)));

            JPanel buttons = new JPanel(new GridLayout(1, 2, Ui.px(6), 0));
            JButton copyTable = Ui.button(tr("btn_copy_table"), this::copyTable);
            copyTable.setToolTipText(tr("tip_copy_table"));
            buttons.add(copyTable);
            buttons.add(Ui.button(tr("btn_copy_label"), () -> {
                if (last == null) return;
                Ui.copy(last.labelText());
                app.status(tr("st_copied_label"), Ui.OK);
            }));
            buttons.setAlignmentX(LEFT_ALIGNMENT);
            bottom.add(buttons);
            res.add(bottom, BorderLayout.SOUTH);
            cardPanel.add(res, "result");

            add(cardPanel, BorderLayout.CENTER);
        }

        void show(Result r) {
            boolean untouched = app.data.project.stream().allMatch(y -> blank(y.name) && blank(y.grams)
                    && y.fibers.stream().allMatch(f -> blank(f.fiber) && blank(f.percent)));
            if (r.yarnCount() == 0 || untouched) {
                last = null;
                total.setText(" ");
                cards.show(cardPanel, "empty");
                return;
            }
            if (!r.problems().isEmpty()) {
                last = null;
                total.setText(" ");
                StringBuilder html = new StringBuilder("<html><body style='width:" + Ui.px(260) + "px'><p><b>")
                        .append(Ui.esc(tr("res_todo_title"))).append("</b></p><ul style='margin-left:16px'>");
                for (String p : r.problems()) html.append("<li style='margin-bottom:4px'>").append(Ui.esc(p)).append("</li>");
                html.append("</ul><p><font color='#6E6E6E'>").append(Ui.esc(tr("res_todo_hint"))).append("</font></p></body></html>");
                todo.setText(html.toString());
                cards.show(cardPanel, "todo");
                return;
            }
            last = r;
            total.setText(trf("res_total", fmtFlex(r.totalGrams()), r.yarnCount()));
            model.setResult(r);
            bar.setValues(r.grams());
            label.setText(r.labelText());
            cards.show(cardPanel, "result");
        }

        private void copyTable() {
            if (last == null) return;
            StringBuilder b = new StringBuilder();
            b.append(tr("col_fiber")).append('\t').append(tr("col_share")).append('\t').append(tr("col_grams")).append('\n');
            for (int i = 0; i < last.fibers().size(); i++)
                b.append(last.fibers().get(i)).append('\t').append(last.share(i)).append('\t').append(fmt(last.grams()[i], 1)).append('\n');
            b.append(tr("row_total")).append('\t').append(fmt(100, last.decimals())).append('\t').append(fmt(last.totalGrams(), 1)).append('\n');
            Ui.copy(b.toString());
            app.status(tr("st_copied_table"), Ui.OK);
        }

        static final class ResultTableModel extends AbstractTableModel {
            private Result r;
            void setResult(Result r) { this.r = r; fireTableDataChanged(); }
            public int getRowCount() { return r == null ? 0 : r.fibers().size(); }
            public int getColumnCount() { return 3; }
            @Override public String getColumnName(int c) { return tr(c == 0 ? "col_fiber" : c == 1 ? "col_share" : "col_grams"); }
            public Object getValueAt(int row, int col) {
                return switch (col) {
                    case 0 -> r.fibers().get(row);
                    case 1 -> pct(r.share(row));
                    default -> fmt(r.grams()[row], 1) + " g";
                };
            }
        }
    }

    // =====================================================================
    // Main window
    // =====================================================================

    static final class MainFrame extends JFrame {
        final AppData data;
        final Store store;
        private final List<YarnCard> cards = new ArrayList<>();
        private WidthTrackingPanel cardsPanel;
        private JScrollPane cardsScroll;
        private ResultPanel result;
        private JLabel statusLabel;
        private JButton undoButton;
        private final javax.swing.Timer saveTimer;
        private final javax.swing.Timer statusTimer;
        private ProjectYarn removedYarn;
        private int removedIndex;
        private boolean saveErrorShown;

        MainFrame(AppData data, Store store) {
            this.data = data;
            this.store = store;
            saveTimer = new javax.swing.Timer(700, e -> saveNow());
            saveTimer.setRepeats(false);
            statusTimer = new javax.swing.Timer(12000, e -> status(" ", Ui.MUTED));
            statusTimer.setRepeats(false);
            setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosing(WindowEvent e) { exit(); }
            });
            rebuildAll();
            restoreBounds();
        }

        // ---------- building ----------

        void rebuildAll() {
            T.lang = data.settings.lang;
            Ui.applyLanguage();
            setTitle(tr("app_title"));
            setJMenuBar(buildMenu());

            JPanel root = new JPanel(new BorderLayout());

            // Left: project
            JPanel left = new JPanel(new BorderLayout());
            JPanel top = new JPanel(new BorderLayout());
            top.setBorder(new EmptyBorder(Ui.px(12), Ui.px(12), Ui.px(8), Ui.px(12)));
            JPanel titles = new JPanel(new GridLayout(0, 1));
            titles.add(Ui.title(tr("proj_title"), 1.35f));
            titles.add(Ui.muted(tr("proj_hint")));
            top.add(titles, BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, Ui.px(6), 0));
            actions.add(Ui.button(tr("btn_add_yarn"), () -> addYarn(new ProjectYarn())));
            actions.add(Ui.button(tr("btn_add_from_lib"), this::addFromLibrary));
            JPanel actionsWrap = new JPanel(new GridBagLayout());
            actionsWrap.add(actions);
            top.add(actionsWrap, BorderLayout.EAST);
            left.add(top, BorderLayout.NORTH);

            cardsPanel = new WidthTrackingPanel(null);
            cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
            cardsPanel.setBorder(new EmptyBorder(0, Ui.px(12), Ui.px(12), Ui.px(12)));
            cardsScroll = new JScrollPane(cardsPanel);
            cardsScroll.setBorder(null);
            cardsScroll.getVerticalScrollBar().setUnitIncrement(Ui.px(24));
            left.add(cardsScroll, BorderLayout.CENTER);

            result = new ResultPanel(this);
            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, result);
            split.setResizeWeight(0.6);
            split.setBorder(null);
            split.setContinuousLayout(true);
            root.add(split, BorderLayout.CENTER);

            // Status bar
            JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT, Ui.px(8), Ui.px(3)));
            statusBar.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, Ui.CARD_BORDER));
            statusLabel = new JLabel(" ");
            undoButton = Ui.button(tr("btn_undo"), this::undoRemove);
            undoButton.setVisible(false);
            statusBar.add(statusLabel);
            statusBar.add(undoButton);
            root.add(statusBar, BorderLayout.SOUTH);

            setContentPane(root);
            rebuildProject();
            revalidate();
            repaint();
            SwingUtilities.invokeLater(() -> split.setDividerLocation(0.6));
        }

        private JMenuBar buildMenu() {
            int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
            JMenuBar bar = new JMenuBar();

            JMenu file = new JMenu(tr("menu_file"));
            file.add(item(tr("mi_import"), null, this::importData));
            file.add(item(tr("mi_export"), null, this::exportBackup));
            file.addSeparator();
            file.add(item(tr("mi_open_folder"), null, this::openDataFolder));
            file.addSeparator();
            file.add(item(tr("mi_exit"), KeyStroke.getKeyStroke(KeyEvent.VK_Q, mask), this::exit));
            bar.add(file);

            JMenu project = new JMenu(tr("menu_project"));
            project.add(item(tr("mi_add_yarn"), KeyStroke.getKeyStroke(KeyEvent.VK_N, mask), () -> addYarn(new ProjectYarn())));
            project.add(item(tr("mi_add_from_lib"), KeyStroke.getKeyStroke(KeyEvent.VK_L, mask), this::addFromLibrary));
            project.addSeparator();
            project.add(item(tr("mi_clear"), null, this::clearProject));
            bar.add(project);

            JMenu library = new JMenu(tr("menu_library"));
            library.add(item(tr("mi_manage_yarns"), null, () -> new LibraryDialog(this).setVisible(true)));
            library.add(item(tr("mi_manage_fibers"), null, () -> new FiberDialog(this).setVisible(true)));
            bar.add(library);

            JMenu view = new JMenu(tr("menu_view"));
            JMenu lang = new JMenu(tr("mi_language"));
            ButtonGroup lg = new ButtonGroup();
            for (Lang l : Lang.values()) {
                JRadioButtonMenuItem mi = new JRadioButtonMenuItem(l.label, l == data.settings.lang);
                mi.addActionListener(e -> setLanguage(l));
                lg.add(mi);
                lang.add(mi);
            }
            view.add(lang);
            JMenu size = new JMenu(tr("mi_text_size"));
            ButtonGroup sg = new ButtonGroup();
            for (int s : new int[]{100, 115, 130, 150, 175, 200}) {
                JRadioButtonMenuItem mi = new JRadioButtonMenuItem(s + " %", s == data.settings.fontScale);
                mi.addActionListener(e -> setFontScale(s));
                sg.add(mi);
                size.add(mi);
            }
            view.add(size);
            bar.add(view);

            JMenu help = new JMenu(tr("menu_help"));
            help.add(item(tr("mi_guide"), KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), () -> showGuide(null)));
            help.add(item(tr("mi_about"), null, () -> Ui.info(this, trf("about_text", VERSION, store.dir))));
            bar.add(help);
            return bar;
        }

        private static JMenuItem item(String text, KeyStroke key, Runnable action) {
            JMenuItem mi = new JMenuItem(text);
            if (key != null) mi.setAccelerator(key);
            mi.addActionListener(e -> action.run());
            return mi;
        }

        void rebuildProject() {
            int scroll = cardsScroll.getVerticalScrollBar().getValue();
            cardsPanel.removeAll();
            cards.clear();
            if (data.project.isEmpty()) {
                JPanel empty = new JPanel(new GridBagLayout());
                empty.setOpaque(false);
                JPanel box = new JPanel();
                box.setOpaque(false);
                box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
                JLabel t = Ui.title(tr("empty_title"), 1.2f);
                t.setAlignmentX(CENTER_ALIGNMENT);
                JLabel s = Ui.muted(tr("empty_text"));
                s.setAlignmentX(CENTER_ALIGNMENT);
                JPanel b = new JPanel(new FlowLayout(FlowLayout.CENTER, Ui.px(6), 0));
                b.setOpaque(false);
                b.add(Ui.button(tr("btn_add_yarn"), () -> addYarn(new ProjectYarn())));
                b.add(Ui.button(tr("btn_add_from_lib"), this::addFromLibrary));
                box.add(Box.createVerticalStrut(Ui.px(60)));
                box.add(t);
                box.add(Box.createVerticalStrut(Ui.px(6)));
                box.add(s);
                box.add(Box.createVerticalStrut(Ui.px(14)));
                box.add(b);
                empty.add(box);
                cardsPanel.add(empty);
            } else {
                for (int i = 0; i < data.project.size(); i++) {
                    YarnCard card = new YarnCard(this, data.project.get(i), i);
                    card.setAlignmentX(LEFT_ALIGNMENT);
                    card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
                    cards.add(card);
                    cardsPanel.add(card);
                }
            }
            cardsPanel.add(Box.createVerticalGlue());
            cardsPanel.revalidate();
            cardsPanel.repaint();
            SwingUtilities.invokeLater(() -> cardsScroll.getVerticalScrollBar().setValue(scroll));
            changed();
        }

        // ---------- model changes ----------

        /** Called after every edit: refreshes statuses and the result, and schedules saving. */
        void changed() {
            if (result == null) return;
            for (YarnCard c : cards) {
                c.refreshStatus();
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
            cardsPanel.revalidate();
            result.show(Calc.compute(data.project, data.library, data.settings.decimals));
            saveTimer.restart();
        }

        void saveNow() {
            saveTimer.stop();
            try {
                store.save(data);
            } catch (IOException e) {
                status(trf("st_save_error", e.getMessage()), Ui.ERR);
                if (!saveErrorShown) {
                    saveErrorShown = true;
                    Ui.error(this, trf("st_save_error", e.getMessage()) + "\n\n" + store.file);
                }
            }
        }

        void status(String text, Color color) {
            statusLabel.setText(text);
            statusLabel.setForeground(color);
            undoButton.setVisible(false);
            statusTimer.restart();
        }

        private void scrollToCard(int index) {
            SwingUtilities.invokeLater(() -> {
                if (index < 0 || index >= cards.size()) return;
                YarnCard c = cards.get(index);
                c.scrollRectToVisible(new Rectangle(0, 0, c.getWidth(), c.getHeight()));
                c.focusFirstEmpty();
            });
        }

        void addYarn(ProjectYarn y) {
            data.project.add(y);
            rebuildProject();
            scrollToCard(data.project.size() - 1);
        }

        void addFromLibrary() {
            LibraryYarn y = YarnPicker.pick(this, data.library);
            if (y == null) return;
            addYarn(ProjectYarn.fromLibrary(y));
            status(trf("st_added_to_project", y.name()), Ui.OK);
        }

        void removeYarn(int index) {
            removedYarn = data.project.remove(index);
            removedIndex = index;
            rebuildProject();
            String name = blank(removedYarn.name) ? trf("card_title", index + 1) : removedYarn.name.trim();
            status(trf("st_removed", name), Ui.MUTED);
            undoButton.setVisible(true);
        }

        private void undoRemove() {
            if (removedYarn == null) return;
            data.project.add(Math.min(removedIndex, data.project.size()), removedYarn);
            int idx = Math.min(removedIndex, data.project.size() - 1);
            removedYarn = null;
            status(" ", Ui.MUTED);
            rebuildProject();
            scrollToCard(idx);
        }

        void clearProject() {
            if (!Ui.confirm(this, tr("dlg_clear"), tr("btn_clear_confirm"), true)) return;
            data.project.clear();
            data.project.add(new ProjectYarn());
            removedYarn = null;
            rebuildProject();
            scrollToCard(0);
        }

        void loadFromLibrary(int index) {
            LibraryYarn y = YarnPicker.pick(this, data.library);
            if (y == null) return;
            ProjectYarn p = data.project.get(index);
            p.name = y.name();
            p.setComposition(y.composition());
            rebuildProject();
            scrollToCard(index);
        }

        void saveToLibrary(int index) {
            ProjectYarn y = data.project.get(index);
            Check c = Calc.checkComposition(y.fibers);
            if (c.level() != Level.OK) { Ui.info(this, trf("dlg_fix_first", c.message())); return; }
            List<FiberShare> shares = Calc.toShares(y.fibers);
            String name = y.name.trim();
            if (name.isEmpty()) {
                name = Ui.ask(this, tr("dlg_name_prompt"), "");
                if (name == null) return;
            }
            LibraryYarn existing = data.library.find(name);
            if (existing != null) {
                if (sameComposition(existing.composition(), shares)) { status(trf("st_already_saved", existing.name()), Ui.MUTED); return; }
                String msg = trf("dlg_replace", existing.name(), existing.summary(), compositionText(shares));
                if (!Ui.confirm(this, msg, tr("btn_replace"), false)) return;
                name = existing.name();
            }
            y.name = name;
            data.library.putYarn(new LibraryYarn(name, shares));
            for (FiberShare s : shares) data.library.addFiber(s.fiber());
            rebuildProject();
            saveNow();
            status(trf("st_saved_lib", name), Ui.OK);
        }

        // ---------- settings ----------

        void setLanguage(Lang l) {
            if (l == data.settings.lang) return;
            data.settings.lang = l;
            rebuildAll();
            saveNow();
        }

        void setFontScale(int scale) {
            if (scale == data.settings.fontScale) return;
            data.settings.fontScale = scale;
            Ui.installLookAndFeel(scale);
            SwingUtilities.updateComponentTreeUI(this);
            rebuildAll();
            saveNow();
        }

        private void restoreBounds() {
            Rectangle b = data.settings.bounds;
            boolean visible = false;
            if (b != null) {
                Point center = new Point(b.x + b.width / 2, b.y + b.height / 2);
                for (GraphicsDevice gd : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
                    if (gd.getDefaultConfiguration().getBounds().contains(center)) visible = true;
            }
            if (visible && b.width > 300 && b.height > 200) setBounds(b);
            else {
                setSize(Ui.px(1120), Ui.px(760));
                setLocationRelativeTo(null);
            }
            if (data.settings.maximized) setExtendedState(MAXIMIZED_BOTH);
        }

        void exit() {
            data.settings.maximized = (getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
            if (!data.settings.maximized) data.settings.bounds = getBounds();
            saveNow();
            dispose();
            System.exit(0);
        }

        // ---------- data menu ----------

        void importData() {
            JFileChooser fc = new JFileChooser(Paths.get("").toAbsolutePath().toFile());
            fc.setDialogTitle(tr("imp_title"));
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.setFileFilter(new FileNameExtensionFilter(tr("imp_filter"), "json", "bak"));
            if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path p = fc.getSelectedFile().toPath();
            try {
                Store.ImportReport r = Store.importInto(data.library, p);
                rebuildProject();
                saveNow();
                Ui.info(this, trf("imp_done", p, r.added(), r.renamed(), r.skipped(), r.fibersAdded()));
            } catch (Exception e) {
                Ui.error(this, trf("imp_error", e.getMessage()));
            }
        }

        void exportBackup() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle(tr("exp_title"));
            fc.setFileFilter(new FileNameExtensionFilter(tr("imp_filter"), "json"));
            fc.setSelectedFile(new java.io.File("yarncalculator-backup-" + LocalDate.now() + ".json"));
            if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
            Path p = fc.getSelectedFile().toPath();
            if (!p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json")) p = p.resolveSibling(p.getFileName() + ".json");
            if (Files.exists(p) && !Ui.confirm(this, trf("exp_overwrite", p.getFileName()), tr("btn_replace"), true)) return;
            try {
                Store.export(data, p);
                Ui.info(this, trf("exp_done", p));
            } catch (IOException e) {
                Ui.error(this, trf("st_save_error", e.getMessage()));
            }
        }

        void openDataFolder() {
            try { Files.createDirectories(store.dir); } catch (IOException ignored) {}
            JTextField path = new JTextField(store.dir.toString());
            path.setEditable(false);
            JPanel p = new JPanel(new BorderLayout(0, Ui.px(6)));
            p.add(new JLabel(tr("folder_text")), BorderLayout.NORTH);
            p.add(path, BorderLayout.CENTER);
            Ui.open(store.dir.toUri(), false);
            JOptionPane.showMessageDialog(this, p, tr("app_title"), JOptionPane.INFORMATION_MESSAGE);
        }

        // ---------- guide ----------

        void startupDialogs(Store.Loaded loaded) {
            if (!data.settings.guideSeen) {
                showGuide(loaded.notice());
                data.settings.guideSeen = true;
                saveNow();
            } else if (loaded.notice() != null) {
                JOptionPane.showMessageDialog(this, Ui.wrapped(loaded.notice(), Ui.px(420)), tr("notice_title"), JOptionPane.WARNING_MESSAGE);
            }
            if (!cards.isEmpty()) cards.get(0).focusFirstEmpty();
        }

        void showGuide(String notice) {
            JDialog d = new JDialog(this, tr("guide_title"), true);
            JEditorPane pane = new JEditorPane("text/html", "");
            pane.setEditable(false);
            pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            pane.setFont(UIManager.getFont("Label.font"));
            StringBuilder html = new StringBuilder("<html><head><style>"
                    + "h2 { margin-top: 4px; margin-bottom: 4px; } h3 { margin-top: 14px; margin-bottom: 2px; }"
                    + "p { margin-top: 4px; margin-bottom: 4px; } ul, ol { margin-top: 2px; margin-bottom: 4px; }"
                    + "li { margin-bottom: 3px; }</style></head><body style='margin:12px 16px'>");
            if (notice != null)
                html.append("<table width='100%' cellpadding='8' bgcolor='#E8F5E9'><tr><td>")
                        .append(Ui.esc(notice).replace("\n", "<br>")).append("</td></tr></table>");
            html.append(T.guideHtml(store.dir.toString())).append("</body></html>");
            pane.setText(html.toString());
            pane.setCaretPosition(0);
            pane.addHyperlinkListener(e -> {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && e.getURL() != null) {
                    try { Ui.open(e.getURL().toURI(), true); } catch (Exception ignored) {}
                }
            });
            JScrollPane sp = new JScrollPane(pane);
            sp.setBorder(null);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            JButton close = Ui.button(tr("btn_close"), d::dispose);
            buttons.add(close);
            d.add(sp, BorderLayout.CENTER);
            d.add(buttons, BorderLayout.SOUTH);
            d.getRootPane().setDefaultButton(close);
            Ui.closeOnEscape(d);
            d.setSize(Ui.px(700), Math.min(Ui.px(720), getHeight() + Ui.px(40)));
            d.setLocationRelativeTo(this);
            d.setVisible(true);
        }
    }

    // =====================================================================
    // Library dialogs
    // =====================================================================

    /** Searchable list of library yarns, showing name and composition. */
    static final class YarnList extends JPanel {
        private final Library lib;
        final HintField search = new HintField(20, tr("hint_search"));
        final DefaultListModel<LibraryYarn> model = new DefaultListModel<>();
        final JList<LibraryYarn> list = new JList<>(model);

        YarnList(Library lib) {
            super(new BorderLayout(0, Ui.px(6)));
            this.lib = lib;
            add(search, BorderLayout.NORTH);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(new DefaultListCellRenderer() {
                @Override public Component getListCellRendererComponent(JList<?> l, Object v, int i, boolean sel, boolean foc) {
                    LibraryYarn y = (LibraryYarn) v;
                    String color = sel ? "" : " color='#6E6E6E'";
                    super.getListCellRendererComponent(l, "<html><b>" + Ui.esc(y.name()) + "</b><br><font" + color + ">"
                            + Ui.esc(y.summary()) + "</font></html>", i, sel, foc);
                    setBorder(new EmptyBorder(Ui.px(4), Ui.px(8), Ui.px(4), Ui.px(8)));
                    return this;
                }
            });
            add(new JScrollPane(list), BorderLayout.CENTER);
            Ui.onChange(search, () -> refresh(null));
            search.addKeyListener(new KeyAdapter() {
                @Override public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_DOWN && model.size() > 0) {
                        list.requestFocusInWindow();
                        if (list.getSelectedIndex() < 0) list.setSelectedIndex(0);
                    }
                }
            });
            refresh(null);
        }

        void refresh(String select) {
            LibraryYarn prev = list.getSelectedValue();
            String q = key(search.getText());
            model.clear();
            for (LibraryYarn y : lib.yarns) {
                boolean match = q.isEmpty() || key(y.name()).contains(q);
                for (FiberShare s : y.composition()) if (key(s.fiber()).contains(q)) match = true;
                if (match) model.addElement(y);
            }
            String want = select != null ? select : prev != null ? prev.name() : null;
            int idx = 0;
            for (int i = 0; i < model.size(); i++) if (want != null && model.get(i).name().equalsIgnoreCase(want)) idx = i;
            if (model.size() > 0) {
                list.setSelectedIndex(idx);
                list.ensureIndexIsVisible(idx);
            }
        }

        LibraryYarn selected() { return list.getSelectedValue(); }

        void onActivate(Runnable r) {
            list.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && selected() != null) r.run(); }
            });
            list.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "activate");
            list.getActionMap().put("activate", new AbstractAction() {
                public void actionPerformed(ActionEvent e) { if (selected() != null) r.run(); }
            });
        }
    }

    static final class YarnPicker {
        static LibraryYarn pick(Component owner, Library lib) {
            if (lib.yarns.isEmpty()) { Ui.info(owner, tr("lib_empty")); return null; }
            Window w = owner instanceof Window win ? win : SwingUtilities.getWindowAncestor(owner);
            JDialog d = new JDialog(w, tr("picker_title"), Dialog.ModalityType.APPLICATION_MODAL);
            YarnList yl = new YarnList(lib);
            LibraryYarn[] chosen = {null};
            Runnable use = () -> { chosen[0] = yl.selected(); if (chosen[0] != null) d.dispose(); };
            yl.onActivate(use);
            yl.search.addActionListener(e -> use.run());
            JPanel content = new JPanel(new BorderLayout(0, Ui.px(10)));
            content.setBorder(new EmptyBorder(Ui.px(12), Ui.px(12), Ui.px(12), Ui.px(12)));
            content.add(yl, BorderLayout.CENTER);
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Ui.px(6), 0));
            JButton ok = Ui.button(tr("btn_use"), use);
            buttons.add(ok);
            buttons.add(Ui.button(tr("btn_cancel"), d::dispose));
            content.add(buttons, BorderLayout.SOUTH);
            d.setContentPane(content);
            d.getRootPane().setDefaultButton(ok);
            Ui.closeOnEscape(d);
            d.setSize(Ui.px(480), Ui.px(480));
            d.setLocationRelativeTo(owner);
            SwingUtilities.invokeLater(yl.search::requestFocusInWindow);
            d.setVisible(true);
            return chosen[0];
        }
    }

    static final class LibraryDialog extends JDialog {
        private final MainFrame app;
        private final YarnList yl;
        private final JLabel count = Ui.muted("");

        LibraryDialog(MainFrame app) {
            super(app, tr("lib_title"), true);
            this.app = app;
            yl = new YarnList(app.data.library);
            yl.onActivate(this::edit);

            JPanel side = new JPanel(new GridLayout(0, 1, 0, Ui.px(6)));
            side.add(Ui.button(tr("btn_new"), this::create));
            side.add(Ui.button(tr("btn_edit"), this::edit));
            side.add(Ui.button(tr("btn_duplicate"), this::duplicate));
            side.add(Ui.button(tr("btn_delete"), this::delete));
            side.add(new JLabel());
            side.add(Ui.button(tr("btn_to_project"), this::toProject));
            JPanel sideWrap = new JPanel(new BorderLayout());
            sideWrap.add(side, BorderLayout.NORTH);

            JPanel content = new JPanel(new BorderLayout(Ui.px(10), Ui.px(10)));
            content.setBorder(new EmptyBorder(Ui.px(12), Ui.px(12), Ui.px(12), Ui.px(12)));
            content.add(yl, BorderLayout.CENTER);
            content.add(sideWrap, BorderLayout.EAST);
            JPanel bottom = new JPanel(new BorderLayout());
            bottom.add(count, BorderLayout.WEST);
            JButton close = Ui.button(tr("btn_close"), this::dispose);
            bottom.add(close, BorderLayout.EAST);
            content.add(bottom, BorderLayout.SOUTH);
            setContentPane(content);
            Ui.closeOnEscape(this);
            updateCount();
            setSize(Ui.px(680), Ui.px(520));
            setLocationRelativeTo(app);
        }

        private void updateCount() { count.setText(trf("lib_count", app.data.library.yarns.size())); }

        private void committed(String select) {
            yl.refresh(select);
            updateCount();
            app.rebuildProject();
            app.saveNow();
        }

        private void create() {
            LibraryYarn y = YarnEditor.edit(this, app.data.library, null);
            if (y == null) return;
            app.data.library.putYarn(y);
            for (FiberShare s : y.composition()) app.data.library.addFiber(s.fiber());
            committed(y.name());
        }

        private void edit() {
            LibraryYarn old = yl.selected();
            if (old == null) return;
            LibraryYarn y = YarnEditor.edit(this, app.data.library, old);
            if (y == null) return;
            app.data.library.yarns.remove(old);
            app.data.library.putYarn(y);
            for (FiberShare s : y.composition()) app.data.library.addFiber(s.fiber());
            committed(y.name());
        }

        private void duplicate() {
            LibraryYarn old = yl.selected();
            if (old == null) return;
            String name = app.data.library.uniqueName(old.name() + " (" + tr("copy_suffix") + ")");
            app.data.library.putYarn(new LibraryYarn(name, old.composition()));
            committed(name);
        }

        private void delete() {
            LibraryYarn y = yl.selected();
            if (y == null) return;
            if (!Ui.confirm(this, trf("dlg_delete_yarn", y.name()), tr("btn_delete"), true)) return;
            app.data.library.yarns.remove(y);
            committed(null);
        }

        private void toProject() {
            LibraryYarn y = yl.selected();
            if (y == null) return;
            dispose();
            app.addYarn(ProjectYarn.fromLibrary(y));
            app.status(trf("st_added_to_project", y.name()), Ui.OK);
        }
    }

    /** Dialog to create or edit a library yarn. */
    static final class YarnEditor {
        static LibraryYarn edit(Window owner, Library lib, LibraryYarn existing) {
            JDialog d = new JDialog(owner, tr(existing == null ? "edit_title_new" : "edit_title"), Dialog.ModalityType.APPLICATION_MODAL);
            ProjectYarn tmp = existing == null ? new ProjectYarn() : ProjectYarn.fromLibrary(existing);
            HintField name = new HintField(24, tr("hint_name"));
            name.setText(tmp.name);
            JLabel status = new JLabel();
            status.setIconTextGap(Ui.px(6));
            CompositionEditor[] comp = {null};
            JButton fill = Ui.button(tr("btn_fill"), () -> comp[0].fillUp());
            fill.setToolTipText(tr("tip_fill"));
            Runnable refresh = () -> {
                Check c = Calc.checkComposition(tmp.fibers);
                fill.setVisible(c.canFill());
                status.setIcon(new StatusIcon(c.level()));
                status.setText(c.message());
                status.setForeground(c.level() == Level.OK ? Ui.OK : c.level() == Level.WARN ? Ui.WARN.darker() : Ui.ERR);
            };
            comp[0] = new CompositionEditor(tmp.fibers, lib, refresh);
            refresh.run();

            JPanel content = new JPanel(new BorderLayout(0, Ui.px(10)));
            content.setBorder(new EmptyBorder(Ui.px(12), Ui.px(14), Ui.px(12), Ui.px(14)));
            JPanel nameRow = new JPanel(new BorderLayout(Ui.px(8), 0));
            nameRow.add(new JLabel(tr("lbl_name")), BorderLayout.WEST);
            nameRow.add(name, BorderLayout.CENTER);
            content.add(nameRow, BorderLayout.NORTH);
            JPanel mid = new JPanel(new BorderLayout(0, Ui.px(8)));
            JPanel holder = new JPanel(new BorderLayout());
            holder.add(comp[0], BorderLayout.NORTH);
            JScrollPane sp = new JScrollPane(holder);
            sp.setBorder(null);
            mid.add(sp, BorderLayout.CENTER);
            JPanel statusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            statusRow.add(status);
            statusRow.add(Box.createHorizontalStrut(Ui.px(8)));
            statusRow.add(fill);
            mid.add(statusRow, BorderLayout.SOUTH);
            content.add(mid, BorderLayout.CENTER);

            LibraryYarn[] result = {null};
            JButton save = Ui.button(tr("btn_save"), () -> {
                String n = name.getText().trim();
                if (n.isEmpty()) { Ui.info(d, tr("err_name_blank")); name.requestFocusInWindow(); return; }
                LibraryYarn other = lib.find(n);
                if (other != null && other != existing) { Ui.info(d, trf("err_name_taken", other.name())); return; }
                Check c = Calc.checkComposition(tmp.fibers);
                if (c.level() != Level.OK) { Ui.info(d, trf("dlg_fix_first", c.message())); return; }
                result[0] = new LibraryYarn(n, Calc.toShares(tmp.fibers));
                d.dispose();
            });
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, Ui.px(6), 0));
            buttons.add(save);
            buttons.add(Ui.button(tr("btn_cancel"), d::dispose));
            content.add(buttons, BorderLayout.SOUTH);
            d.setContentPane(content);
            Ui.closeOnEscape(d);
            d.setSize(Ui.px(560), Ui.px(440));
            d.setLocationRelativeTo(owner);
            SwingUtilities.invokeLater(name::requestFocusInWindow);
            d.setVisible(true);
            return result[0];
        }
    }

    static final class FiberDialog extends JDialog {
        private final MainFrame app;
        private final DefaultListModel<String> model = new DefaultListModel<>();
        private final JList<String> list = new JList<>(model);
        private final HintField search = new HintField(16, tr("hint_search"));

        FiberDialog(MainFrame app) {
            super(app, tr("fib_title"), true);
            this.app = app;
            JPanel content = new JPanel(new BorderLayout(Ui.px(10), Ui.px(10)));
            content.setBorder(new EmptyBorder(Ui.px(12), Ui.px(12), Ui.px(12), Ui.px(12)));
            content.add(Ui.wrapped(tr("fib_info"), Ui.px(330)), BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(0, Ui.px(6)));
            center.add(search, BorderLayout.NORTH);
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            center.add(new JScrollPane(list), BorderLayout.CENTER);
            content.add(center, BorderLayout.CENTER);
            Ui.onChange(search, () -> refresh(null));

            JPanel side = new JPanel(new GridLayout(0, 1, 0, Ui.px(6)));
            side.add(Ui.button(tr("btn_add"), this::add));
            side.add(Ui.button(tr("btn_rename"), this::rename));
            side.add(Ui.button(tr("btn_delete"), this::delete));
            side.add(new JLabel());
            side.add(Ui.button(tr("btn_add_common"), this::addCommon));
            JPanel sideWrap = new JPanel(new BorderLayout());
            sideWrap.add(side, BorderLayout.NORTH);
            content.add(sideWrap, BorderLayout.EAST);

            JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            bottom.add(Ui.button(tr("btn_close"), this::dispose));
            content.add(bottom, BorderLayout.SOUTH);
            setContentPane(content);
            Ui.closeOnEscape(this);
            addWindowListener(new WindowAdapter() {
                @Override public void windowClosed(WindowEvent e) { app.rebuildProject(); app.saveNow(); }
            });
            refresh(null);
            setSize(Ui.px(520), Ui.px(520));
            setLocationRelativeTo(app);
        }

        private void refresh(String select) {
            String q = key(search.getText());
            model.clear();
            for (String f : app.data.library.fibers) if (q.isEmpty() || key(f).contains(q)) model.addElement(f);
            if (select != null) {
                list.setSelectedValue(select, true);
            }
        }

        private void add() {
            String n = Ui.ask(this, tr("dlg_fiber_name"), search.getText().trim());
            if (n == null) return;
            if (!app.data.library.addFiber(n)) { Ui.info(this, trf("msg_fiber_exists", app.data.library.findFiber(n))); return; }
            search.setText("");
            refresh(n);
        }

        private void rename() {
            String old = list.getSelectedValue();
            if (old == null) return;
            String n = Ui.ask(this, trf("dlg_fiber_rename", old), old);
            if (n == null || n.equals(old)) return;
            Library lib = app.data.library;
            String clash = lib.findFiber(n);
            if (clash != null && !key(clash).equals(key(old))) { Ui.info(this, trf("msg_fiber_exists", clash)); return; }
            lib.fibers.remove(old);
            lib.addFiber(n);
            for (LibraryYarn y : new ArrayList<>(lib.yarns)) {
                boolean hit = false;
                List<FiberShare> comp = new ArrayList<>();
                for (FiberShare s : y.composition()) {
                    if (key(s.fiber()).equals(key(old))) { comp.add(new FiberShare(n, s.percent())); hit = true; }
                    else comp.add(s);
                }
                if (hit) lib.putYarn(new LibraryYarn(y.name(), comp));
            }
            for (ProjectYarn y : app.data.project)
                for (ProjectFiber f : y.fibers) if (key(f.fiber).equals(key(old))) f.fiber = n;
            refresh(n);
        }

        private void delete() {
            String f = list.getSelectedValue();
            if (f == null) return;
            long used = app.data.library.yarns.stream()
                    .filter(y -> y.composition().stream().anyMatch(s -> key(s.fiber()).equals(key(f)))).count();
            String msg = used > 0 ? trf("dlg_delete_fiber_used", f, used) : trf("dlg_delete_fiber", f);
            if (!Ui.confirm(this, msg, tr("btn_delete"), true)) return;
            app.data.library.fibers.remove(f);
            refresh(null);
        }

        private void addCommon() {
            int n = 0;
            for (String f : T.commonFibers()) if (app.data.library.addFiber(f)) n++;
            refresh(null);
            Ui.info(this, trf("msg_common_added", n));
        }
    }
}
