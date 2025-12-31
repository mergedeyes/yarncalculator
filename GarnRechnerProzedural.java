import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GarnRechnerProzedural extends JFrame {

    private final JPanel yarnListContainer = new JPanel();
    private final List<YarnPanel> yarnPanels = new ArrayList<>();
    
    // UI Elements that need translation updates
    private final JButton addYarnBtn = new JButton();
    private final JButton calcBtn = new JButton();
    
    // Global Data
    private static Map<String, List<FiberDef>> loadedYarns = new LinkedHashMap<>();
    private static Set<String> loadedFibers = new TreeSet<>();
    
    private static GarnRechnerProzedural instance;

    public GarnRechnerProzedural() {
        instance = this;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 750);
        
        // Layout Setup
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        setContentPane(mainPanel);

        // --- Top Bar (Language Selection) ---
        JPanel topBar = new JPanel(new BorderLayout());
        JPanel langPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        
        JButton btnDe = new JButton("DE");
        JButton btnEn = new JButton("EN");
        
        btnDe.addActionListener(e -> Text.setLanguage(Lang.DE));
        btnEn.addActionListener(e -> Text.setLanguage(Lang.EN));
        
        langPanel.add(btnDe);
        langPanel.add(btnEn);
        topBar.add(langPanel, BorderLayout.EAST);
        
        mainPanel.add(topBar, BorderLayout.NORTH);

        // --- Center (Yarn List) ---
        yarnListContainer.setLayout(new BoxLayout(yarnListContainer, BoxLayout.Y_AXIS));
        
        JPanel listWrapper = new JPanel(new BorderLayout());
        listWrapper.add(yarnListContainer, BorderLayout.NORTH);
        
        mainPanel.add(new JScrollPane(listWrapper), BorderLayout.CENTER);

        // --- Bottom (Actions) ---
        addYarnBtn.addActionListener(e -> addYarn());
        calcBtn.addActionListener(e -> calculateTotals());

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottom.add(addYarnBtn);
        bottom.add(calcBtn);

        mainPanel.add(bottom, BorderLayout.SOUTH);

        // Initial setup
        Text.addListener(this::updateTexts);
        addYarn();
        updateTexts(); 
    }

    public static GarnRechnerProzedural getInstance() {
        return instance;
    }

    private void updateTexts() {
        setTitle(Text.get("app_title"));
        addYarnBtn.setText(Text.get("btn_add_yarn"));
        calcBtn.setText(Text.get("btn_calc"));
        
        // Trigger updates in children and refresh indices to keep titles correct
        updateYarnIndices();
        revalidate();
        repaint();
    }

    private void addYarn() {
        // Create panel with a callback to remove itself correctly
        YarnPanel yp = new YarnPanel(this::removeYarn);
        
        yarnPanels.add(yp);
        yarnListContainer.add(yp);
        
        updateYarnIndices(); // Update titles (Yarn 1, Yarn 2...)
        
        yarnListContainer.revalidate();
        yarnListContainer.repaint();
    }

    private void removeYarn(YarnPanel yp) {
        yarnPanels.remove(yp);
        yarnListContainer.remove(yp);
        updateYarnIndices(); // Re-number remaining yarns
        yarnListContainer.revalidate();
        yarnListContainer.repaint();
    }

    private void updateYarnIndices() {
        for (int i = 0; i < yarnPanels.size(); i++) {
            yarnPanels.get(i).setYarnIndex(i + 1);
        }
    }

    public void refreshYarnDropdowns() {
        for (YarnPanel yp : yarnPanels) {
            yp.reloadYarnDropdown();
        }
    }

    public void refreshFiberDropdowns() {
        for (YarnPanel yp : yarnPanels) {
            for (FiberRow row : yp.getRawFiberRows()) {
                row.reloadFiberDropdown();
            }
        }
    }

    private void calculateTotals() {
        if (yarnPanels.isEmpty()) {
            JOptionPane.showMessageDialog(this, Text.get("msg_add_one_yarn"));
            return;
        }

        Map<String, Double> fiberWeight = new LinkedHashMap<>();
        double totalWeight = 0.0;
        Locale loc = Text.current.locale;

        try {
            for (YarnPanel yp : yarnPanels) {
                if (!yp.isDisplayableOrAttached()) continue;

                double grams = yp.getGrams();
                if (grams < 0) throw new IllegalArgumentException(Text.get("err_grams_neg"));
                totalWeight += grams;

                double percentSum = yp.getPercentSum();
                if (Math.abs(percentSum - 100.0) > 0.09) {
                    throw new IllegalArgumentException(String.format(loc, Text.get("err_sum_mismatch"), percentSum));
                }

                for (FiberRow row : yp.getFiberRows()) {
                    String name = row.getFiberName().trim();
                    if (name.isEmpty()) throw new IllegalArgumentException(Text.get("err_fiber_empty"));

                    double p = row.getPercent();
                    if (p < 0) throw new IllegalArgumentException(Text.get("err_percent_neg"));

                    double w = grams * (p / 100.0);
                    fiberWeight.merge(normalizeName(name), w, Double::sum);
                }
            }

            if (totalWeight <= 0.0) {
                JOptionPane.showMessageDialog(this, Text.get("msg_total_zero"));
                return;
            }

            // --- LARGEST REMAINDER METHOD ALGORITHM ---
            
            // 1. Prepare Data
            List<Map.Entry<String, Double>> entries = new ArrayList<>(fiberWeight.entrySet());
            entries.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

            int n = entries.size();
            long[] tenths = new long[n];
            double[] exactPercents = new double[n];
            long sumTenths = 0;

            // 2. Initial Rounding
            for (int i = 0; i < n; i++) {
                double grams = entries.get(i).getValue();
                double exact = (grams / totalWeight) * 100.0;
                exactPercents[i] = exact;
                tenths[i] = Math.round(exact * 10.0);
                sumTenths += tenths[i];
            }

            // 3. Calculate Error
            long diff = 1000 - sumTenths; 
            
            // 4. Distribute difference
            if (diff != 0) {
                Integer[] indices = new Integer[n];
                for (int i = 0; i < n; i++) indices[i] = i;

                if (diff > 0) {
                    Arrays.sort(indices, (i1, i2) -> Double.compare(
                        (exactPercents[i2] * 10.0 - tenths[i2]), 
                        (exactPercents[i1] * 10.0 - tenths[i1])
                    ));
                    for (int k = 0; k < diff && k < n; k++) tenths[indices[k]]++;
                } else {
                    Arrays.sort(indices, (i1, i2) -> Double.compare(
                        (tenths[i2] - exactPercents[i2] * 10.0),
                        (tenths[i1] - exactPercents[i1] * 10.0)
                    ));
                    long toRemove = Math.abs(diff);
                    for (int k = 0; k < toRemove && k < n; k++) tenths[indices[k]]--;
                }
            }

            // 5. Output
            String[] cols = {Text.get("col_fiber"), Text.get("col_share")};
            Object[][] data = new Object[n][2];
            
            for (int i = 0; i < n; i++) {
                data[i][0] = entries.get(i).getKey();
                data[i][1] = String.format(loc, "%.1f", tenths[i] / 10.0);
            }

            JTable table = new JTable(data, cols);
            table.setEnabled(false);
            JScrollPane sp = new JScrollPane(table);
            sp.setPreferredSize(new Dimension(400, 250));

            Object[] options = {Text.get("btn_ok"), Text.get("btn_copy")};
            int result = JOptionPane.showOptionDialog(
                    this,
                    sp,
                    String.format(loc, Text.get("title_result"), totalWeight),
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.INFORMATION_MESSAGE,
                    null,
                    options,
                    options[0]
            );

            if (result == 1) { // Copy
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < cols.length; i++) {
                    sb.append(cols[i]);
                    if (i < cols.length - 1) sb.append("\t");
                }
                sb.append("\n");
                for (Object[] row : data) {
                    for (int i = 0; i < row.length; i++) {
                        sb.append(row[i]);
                        if (i < row.length - 1) sb.append("\t");
                    }
                    sb.append("\n");
                }
                StringSelection selection = new StringSelection(sb.toString());
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
                JOptionPane.showMessageDialog(this, Text.get("msg_copied"));
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, Text.get("err_number_format") + "\n" + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private static String normalizeName(String s) {
        s = s.trim();
        if (s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }

    private static void loadData() {
        loadedYarns = DataLoader.loadYarnsFromFile("yarns.json");
        loadedFibers = DataLoader.loadFibersFromFile("fibers.json");
    }
    
    public static void saveYarnsToDisk() {
        DataLoader.saveYarnsToFile(loadedYarns, "yarns.json");
    }
    
    public static void saveFibersToDisk() {
        DataLoader.saveFibersToFile(loadedFibers, "fibers.json");
    }

    public static Map<String, List<FiberDef>> getLoadedYarns() { return loadedYarns; }
    public static Set<String> getLoadedFibers() { return loadedFibers; }

    public static void main(String[] args) {
        Text.init();
        ensureExampleFiles();
        loadData();
        SwingUtilities.invokeLater(() -> new GarnRechnerProzedural().setVisible(true));
    }

    private static void ensureExampleFiles() {
        File fY = new File("yarns.json");
        if (!fY.exists()) {
            Map<String, List<FiberDef>> initial = new LinkedHashMap<>();
            List<FiberDef> sock = new ArrayList<>();
            sock.add(new FiberDef("Schurwolle", 75));
            sock.add(new FiberDef("Polyamid", 25));
            initial.put("Sockenwolle Klassik", sock);
            DataLoader.saveYarnsToFile(initial, "yarns.json");
        }
        
        File fF = new File("fibers.json");
        if (!fF.exists()) {
            Set<String> initial = new TreeSet<>(Arrays.asList("Baumwolle", "Schurwolle", "Polyacryl", "Polyamid", "Seide", "Kaschmir"));
            DataLoader.saveFibersToFile(initial, "fibers.json");
        }
    }
}

// --- Language Infrastructure ---

enum Lang {
    DE(Locale.GERMANY), EN(Locale.US);
    final Locale locale;
    Lang(Locale l) { this.locale = l; }
}

class Text {
    static Lang current = Lang.DE;
    private static final Map<String, String[]> dict = new HashMap<>();
    private static final List<Runnable> listeners = new ArrayList<>();

    static void init() {
        // [German, English]
        put("app_title", "Garnzusammensetzung berechnen", "Yarn Composition Calculator");
        put("btn_add_yarn", "Garn hinzufügen", "Add Yarn");
        put("btn_calc", "Berechnen", "Calculate");
        
        // Updated to use numbering format
        put("border_yarn", "Garn %d", "Yarn %d");
        
        put("lbl_yarn", "Garn:", "Yarn:");
        put("lbl_grams", "Gramm:", "Grams:");
        put("btn_edit", "Garn Bearbeiten", "Edit Yarn");
        put("btn_save", "Garn Speichern", "Save Yarn");
        put("btn_delete", "Garn Löschen", "Delete Yarn");
        put("btn_add_fiber", "Faser hinzufügen", "Add Fiber");
        put("btn_fill_rest", "Rest auf 100%", "Fill Rest to 100%");
        put("btn_remove_yarn", "Garn Entfernen", "Remove Yarn");
        put("btn_remove_fiber", "Faser Entfernen", "Remove Fiber");
        put("sum_prefix", "Summe: ", "Total: ");
        put("sum_ok", "OK", "OK");
        put("sum_low", "Zu wenig", "Too low");
        put("sum_high", "Zu viel", "Too high");
        
        put("lbl_fiber", "Faser:", "Fiber:");
        put("lbl_percent", "%:", "%:");
        put("tip_save_fiber", "Faserart speichern", "Save fiber type");
        put("tip_del_fiber", "Faserart löschen", "Delete fiber type");
        put("tip_rem_row", "Zeile entfernen", "Remove row");
        
        put("custom_yarn", "Neu", "New");
        put("new_fiber_def", "Neue Faser", "New Fiber");
        
        // Messages
        put("msg_add_one_yarn", "Bitte mindestens ein Garn hinzufügen.", "Please add at least one yarn.");
        put("msg_total_zero", "Gesamtgewicht ist 0g. Bitte Grammwerte eintragen.", "Total weight is 0g. Please enter gram values.");
        put("msg_copied", "Tabelle wurde in die Zwischenablage kopiert.", "Table copied to clipboard.");
        put("title_result", "Gesamtzusammensetzung (Gesamt: %.2f g)", "Total Composition (Total: %.2f g)");
        put("col_fiber", "Faser", "Fiber");
        put("col_share", "Anteil (%)", "Share (%)");
        put("btn_ok", "Ok", "Ok");
        put("btn_copy", "Kopieren", "Copy");
        
        put("err_grams_neg", "Gramm darf nicht negativ sein.", "Grams must not be negative.");
        put("err_sum_mismatch", "Bei einem Garn ergeben die Prozente nicht 100% (sondern %.2f%%).", "Percentages for a yarn do not equal 100% (but %.2f%%).");
        put("err_fiber_empty", "Fasername darf nicht leer sein.", "Fiber name must not be empty.");
        put("err_percent_neg", "Prozent darf nicht negativ sein.", "Percentage must not be negative.");
        put("err_number_format", "Bitte nur Zahlen in Prozent/Gramm-Feldern eingeben.", "Please enter only numbers in percent/gram fields.");
        put("msg_no_fibers", "Keine Fasern vorhanden.", "No fibers available.");
        put("msg_sum_100", "Summe muss 100% sein. (Aktuell: %s%%)", "Total must be 100%. (Current: %s%%)");
        put("dlg_name_input", "Bitte Namen für das Garn eingeben:", "Please enter a name for the yarn:");
        put("dlg_overwrite", "Garn '%s' existiert bereits. Überschreiben?", "Yarn '%s' already exists. Overwrite?");
        put("dlg_save_changes", "Änderungen an '%s' speichern (überschreiben)?", "Save changes to '%s' (overwrite)?");
        put("msg_saved", "Garn '%s' gespeichert!", "Yarn '%s' saved!");
        put("dlg_delete", "Möchten Sie '%s' wirklich löschen?", "Do you really want to delete '%s'?");
        put("dlg_title_del", "Löschen", "Delete");
        put("dlg_title_warn", "Warnung", "Warning");
        put("dlg_fiber_exists", "Faser '%s' existiert bereits.", "Fiber '%s' already exists.");
        put("dlg_del_fiber", "Faserart '%s' wirklich löschen?", "Really delete fiber type '%s'?");
    }

    private static void put(String key, String de, String en) {
        dict.put(key, new String[]{de, en});
    }

    static String get(String key) {
        String[] vals = dict.get(key);
        if (vals == null) return "MISSING:" + key;
        return current == Lang.DE ? vals[0] : vals[1];
    }
    
    static void setLanguage(Lang l) {
        if (current == l) return;
        current = l;
        notifyListeners();
    }
    
    static void addListener(Runnable r) { listeners.add(r); }
    private static void notifyListeners() { listeners.forEach(Runnable::run); }
}

class FiberDef {
    String name;
    double percentage;
    FiberDef(String n, double p) { this.name = n; this.percentage = p; }
}

class YarnPanel extends JPanel {

    private final JComboBox<String> yarnSelector = new JComboBox<>();
    private final JButton editBtn = new JButton();
    private final JButton saveBtn = new JButton();
    private final JButton deleteBtn = new JButton();
    private final JTextField gramsField = new JTextField("100", 5);

    private final JLabel lblYarn = new JLabel();
    private final JLabel lblGrams = new JLabel();
    private final JLabel sumLabel = new JLabel();
    private final JLabel sumHint = new JLabel();

    private final JPanel fiberList = new JPanel();
    private final List<FiberRow> fiberRows = new ArrayList<>();

    private final JButton addFiber = new JButton();
    private final JButton fillRest = new JButton();
    private final JButton removeYarn = new JButton();

    private final java.util.function.Consumer<YarnPanel> onRemove;
    private boolean isInternalChange = false;
    private boolean isEditingSavedYarn = false;
    private int yarnIndex = 1;

    YarnPanel(java.util.function.Consumer<YarnPanel> onRemove) {
        this.onRemove = onRemove;

        // Border will be set in updateTexts
        setLayout(new BorderLayout(5, 5));
        
        JPanel headerLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        
        reloadYarnDropdown();
        
        yarnSelector.addActionListener(e -> onYarnSelectionChanged());
        editBtn.addActionListener(e -> onEditClicked());
        saveBtn.addActionListener(e -> onSaveClicked());
        deleteBtn.addActionListener(e -> onDeleteClicked());
        
        saveBtn.setForeground(new Color(0, 100, 0)); 
        deleteBtn.setForeground(Color.RED);
        
        UIHelper.addSelectAllOnFocus(gramsField, false);

        headerLine.add(lblYarn);
        headerLine.add(yarnSelector);
        headerLine.add(editBtn);
        headerLine.add(saveBtn);
        headerLine.add(deleteBtn);
        headerLine.add(Box.createHorizontalStrut(15));
        headerLine.add(lblGrams);
        headerLine.add(gramsField);

        JPanel bottomLine = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        addFiber.addActionListener(e -> addFiberRow(Text.get("new_fiber_def"), 0));
        fillRest.addActionListener(e -> fillRestTo100());
        removeYarn.addActionListener(e -> removeSelf());

        bottomLine.add(addFiber);
        bottomLine.add(fillRest);
        bottomLine.add(removeYarn);
        bottomLine.add(Box.createHorizontalStrut(10));
        bottomLine.add(sumLabel);
        bottomLine.add(Box.createHorizontalStrut(5));
        bottomLine.add(sumHint);

        JPanel topContainer = new JPanel();
        topContainer.setLayout(new BoxLayout(topContainer, BoxLayout.Y_AXIS));
        topContainer.add(headerLine);
        topContainer.add(Box.createVerticalStrut(4));
        topContainer.add(bottomLine);
        
        add(topContainer, BorderLayout.NORTH);

        fiberList.setLayout(new BoxLayout(fiberList, BoxLayout.Y_AXIS));
        add(fiberList, BorderLayout.CENTER);

        addFiberRow(Text.get("new_fiber_def"), 100);
        updateSumUI();
        UIHelper.attachDocListener(gramsField, this::updateSumUI);
        
        updateButtonState();
        checkLockState();
        updateTexts();
    }
    
    // Called externally to set numbering
    void setYarnIndex(int i) {
        this.yarnIndex = i;
        updateTexts();
    }
    
    void updateTexts() {
        // Create thicker border with sequential title
        Border line = BorderFactory.createLineBorder(Color.GRAY, 2);
        TitledBorder title = BorderFactory.createTitledBorder(line, String.format(Text.get("border_yarn"), yarnIndex));
        setBorder(title);

        lblYarn.setText(Text.get("lbl_yarn"));
        lblGrams.setText(Text.get("lbl_grams"));
        editBtn.setText(Text.get("btn_edit"));
        saveBtn.setText(Text.get("btn_save"));
        deleteBtn.setText(Text.get("btn_delete"));
        addFiber.setText(Text.get("btn_add_fiber"));
        fillRest.setText(Text.get("btn_fill_rest"));
        removeYarn.setText(Text.get("btn_remove_yarn"));
        
        if (yarnSelector.getItemCount() > 0) {
            isInternalChange = true;
            String current = (String) yarnSelector.getSelectedItem();
            yarnSelector.removeItemAt(0);
            yarnSelector.insertItemAt(Text.get("custom_yarn"), 0);
            
            boolean wasCustom = current == null || current.equals("Benutzerdefiniert") || current.equals("Custom") || current.equals("Neu") || current.equals("New");
            if (wasCustom) yarnSelector.setSelectedIndex(0);
            else yarnSelector.setSelectedItem(current);
            isInternalChange = false;
        }

        for(FiberRow r : fiberRows) r.updateTexts();
        updateSumUI();
        repaint();
    }
    
    @Override
    public Dimension getMaximumSize() {
        Dimension d = super.getPreferredSize();
        d.width = Integer.MAX_VALUE;
        return d;
    }

    public void reloadYarnDropdown() {
        isInternalChange = true;
        Object currentItem = yarnSelector.getSelectedItem();
        String currentName = (currentItem != null) ? currentItem.toString() : Text.get("custom_yarn");

        yarnSelector.removeAllItems();
        yarnSelector.addItem(Text.get("custom_yarn"));
        
        for (String k : GarnRechnerProzedural.getLoadedYarns().keySet()) {
            yarnSelector.addItem(k);
        }
        
        boolean found = false;
        if (currentName.equals("Benutzerdefiniert") || currentName.equals("Custom") || currentName.equals("Neu") || currentName.equals("New")) {
            yarnSelector.setSelectedIndex(0);
            found = true;
        } else {
            for(int i=0; i<yarnSelector.getItemCount(); i++) {
                if (Objects.equals(yarnSelector.getItemAt(i), currentName)) {
                    yarnSelector.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
        }
        if (!found) yarnSelector.setSelectedIndex(0);

        isInternalChange = false;
        
        String newSelection = (String) yarnSelector.getSelectedItem();
        if (found && !Text.get("custom_yarn").equals(newSelection)) {
             List<FiberDef> defs = GarnRechnerProzedural.getLoadedYarns().get(newSelection);
             if (defs != null) {
                 loadFibersFromDef(defs);
                 setFibersLocked(true);
             }
        }
    }

    private void onYarnSelectionChanged() {
        if (isInternalChange) return;
        
        isEditingSavedYarn = false;
        String selected = (String) yarnSelector.getSelectedItem();
        
        if (Text.get("custom_yarn").equals(selected)) {
            setFibersLocked(false);
        } else {
            List<FiberDef> defs = GarnRechnerProzedural.getLoadedYarns().get(selected);
            if (defs != null) {
                loadFibersFromDef(defs);
                setFibersLocked(true);
            }
        }
        updateButtonState();
    }
    
    private void updateButtonState() {
        String selected = (String) yarnSelector.getSelectedItem();
        boolean isCustom = Text.get("custom_yarn").equals(selected);
        
        if (isCustom) {
            editBtn.setVisible(false);
            saveBtn.setVisible(true);
            deleteBtn.setVisible(false);
        } else {
            deleteBtn.setVisible(true);
            if (isEditingSavedYarn) {
                editBtn.setVisible(false);
                saveBtn.setVisible(true);
                setFibersLocked(false);
            } else {
                editBtn.setVisible(true);
                saveBtn.setVisible(false);
                setFibersLocked(true);
            }
        }
        revalidate();
        repaint();
    }

    private void onEditClicked() {
        isEditingSavedYarn = true;
        updateButtonState();
    }
    
    private void onSaveClicked() {
        List<FiberRow> rows;
        try { rows = getFiberRows(); } 
        catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, Text.get("msg_no_fibers"));
            return;
        }
        
        double sum = getPercentSumSafe();
        if (Math.abs(sum - 100.0) > 0.09) {
            JOptionPane.showMessageDialog(this, String.format(Text.current.locale, Text.get("msg_sum_100"), String.format(Text.current.locale, "%.1f", sum)));
            return;
        }

        String currentSelection = (String) yarnSelector.getSelectedItem();
        String saveName;
        boolean overwrite = false;

        if (Text.get("custom_yarn").equals(currentSelection)) {
            String input = JOptionPane.showInputDialog(this, Text.get("dlg_name_input"));
            if (input == null || input.trim().isEmpty()) return;
            saveName = input.trim();
            if (GarnRechnerProzedural.getLoadedYarns().containsKey(saveName)) {
                int conf = JOptionPane.showConfirmDialog(this, 
                    String.format(Text.get("dlg_overwrite"), saveName), 
                    Text.get("dlg_title_warn"), JOptionPane.YES_NO_OPTION);
                if (conf != JOptionPane.YES_OPTION) return;
                overwrite = true;
            }
        } else {
            int conf = JOptionPane.showConfirmDialog(this, 
                String.format(Text.get("dlg_save_changes"), currentSelection), 
                Text.get("dlg_title_warn"), JOptionPane.YES_NO_OPTION);
            if (conf != JOptionPane.YES_OPTION) return;
            saveName = currentSelection;
            overwrite = true;
        }
        
        List<FiberDef> newDef = new ArrayList<>();
        for (FiberRow r : rows) {
            newDef.add(new FiberDef(r.getFiberName().trim(), r.getPercentSafe()));
        }
        
        GarnRechnerProzedural.getLoadedYarns().put(saveName, newDef);
        GarnRechnerProzedural.saveYarnsToDisk();
        
        isEditingSavedYarn = false;
        GarnRechnerProzedural.getInstance().refreshYarnDropdowns();
        yarnSelector.setSelectedItem(saveName);
        updateButtonState(); 
        
        if(!overwrite) {
             JOptionPane.showMessageDialog(this, String.format(Text.get("msg_saved"), saveName));
        }
    }
    
    private void onDeleteClicked() {
        String selected = (String) yarnSelector.getSelectedItem();
        if (selected == null || Text.get("custom_yarn").equals(selected)) return;
        
        int confirm = JOptionPane.showConfirmDialog(this, 
                String.format(Text.get("dlg_delete"), selected), 
                Text.get("dlg_title_del"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            GarnRechnerProzedural.getLoadedYarns().remove(selected);
            GarnRechnerProzedural.saveYarnsToDisk();
            GarnRechnerProzedural.getInstance().refreshYarnDropdowns();
        }
    }

    private void loadFibersFromDef(List<FiberDef> defs) {
        fiberRows.clear();
        fiberList.removeAll();
        for (FiberDef fd : defs) {
            FiberRow row = new FiberRow(fd.name, fd.percentage, this::removeFiberRow, this::updateSumUI);
            fiberRows.add(row);
            fiberList.add(row);
        }
        fiberList.revalidate();
        fiberList.repaint();
        // Trigger a re-layout up the chain
        if (getParent() != null) getParent().revalidate();
        updateSumUI();
    }

    private void setFibersLocked(boolean locked) {
        addFiber.setEnabled(!locked);
        fillRest.setEnabled(!locked);
        for (FiberRow row : fiberRows) {
            row.setRowEnabled(!locked);
        }
    }
    
    private void checkLockState() {
        String selected = (String) yarnSelector.getSelectedItem();
        setFibersLocked(!Text.get("custom_yarn").equals(selected));
    }

    boolean isDisplayableOrAttached() { return getParent() != null; }

    private void removeSelf() {
        onRemove.accept(this);
    }

    void addFiberRow(String name, double percent) {
        FiberRow row = new FiberRow(name, percent, this::removeFiberRow, this::updateSumUI);
        boolean isLocked = !isEditingSavedYarn && !Text.get("custom_yarn").equals(yarnSelector.getSelectedItem());
        row.setRowEnabled(!isLocked);
        fiberRows.add(row);
        fiberList.add(row);
        fiberList.revalidate();
        fiberList.repaint();
        if(getParent()!=null) getParent().revalidate();
        updateSumUI();
    }

    private void removeFiberRow(FiberRow row) {
        fiberRows.remove(row);
        fiberList.remove(row);
        fiberList.revalidate();
        fiberList.repaint();
        if(getParent()!=null) getParent().revalidate();
        updateSumUI();
    }

    private void fillRestTo100() {
        if (fiberRows.isEmpty()) return;
        double sum = getPercentSumSafe();
        double rest = 100.0 - sum;
        if (Math.abs(rest) < 0.09) { updateSumUI(); return; }
        FiberRow last = fiberRows.get(fiberRows.size() - 1);
        last.setPercent(last.getPercentSafe() + rest);
        updateSumUI();
    }

    double getGrams() { return UIHelper.parseDouble(gramsField.getText()); }

    List<FiberRow> getFiberRows() {
        if (fiberRows.isEmpty()) throw new IllegalArgumentException(Text.get("msg_no_fibers"));
        return fiberRows;
    }
    
    List<FiberRow> getRawFiberRows() { return fiberRows; }

    double getPercentSum() {
        double sum = 0.0;
        for (FiberRow r : getFiberRows()) sum += r.getPercent();
        return sum;
    }

    private double getPercentSumSafe() {
        double sum = 0.0;
        for (FiberRow r : fiberRows) sum += r.getPercentSafe();
        return sum;
    }

    private void updateSumUI() {
        double sum = getPercentSumSafe();
        sumLabel.setText(Text.get("sum_prefix") + String.format(Text.current.locale, "%.1f %%", sum));
        boolean ok = Math.abs(sum - 100.0) < 0.09;
        if (ok) {
            sumHint.setText(Text.get("sum_ok"));
            sumHint.setForeground(new Color(0, 128, 0));
        } else if (sum < 100.0) {
            sumHint.setText(Text.get("sum_low"));
            sumHint.setForeground(new Color(200, 120, 0));
        } else {
            sumHint.setText(Text.get("sum_high"));
            sumHint.setForeground(new Color(180, 0, 0));
        }
    }
}

class FiberRow extends JPanel {
    private final JComboBox<String> fiberSelector = new JComboBox<>();
    private final JTextField percent = new JTextField(4);
    
    private final JButton saveFiberBtn = new JButton("S");
    private final JButton delFiberBtn = new JButton("D");
    private final JButton removeRowBtn = new JButton(); 
    
    private final JLabel lblFiber = new JLabel();
    private final JLabel lblPerc = new JLabel();

    FiberRow(String name, double p,
             java.util.function.Consumer<FiberRow> onRemove,
             Runnable onAnyChange) {
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 2, 0));
        
        fiberSelector.setEditable(true);
        fiberSelector.setPreferredSize(new Dimension(130, 24));
        reloadFiberDropdown();
        fiberSelector.setSelectedItem(name);
        
        Component editorComp = fiberSelector.getEditor().getEditorComponent();
        if (editorComp instanceof JTextField) {
             JTextField tf = (JTextField)editorComp;
             tf.setDisabledTextColor(Color.DARK_GRAY);
             UIHelper.addSelectAllOnFocus(tf, true);
             UIHelper.attachDocListener(tf, onAnyChange);
        }

        percent.setText(formatSimple(p));
        percent.setDisabledTextColor(Color.DARK_GRAY);
        UIHelper.addSelectAllOnFocus(percent, false);

        setupSmallBtn(saveFiberBtn, Color.BLUE);
        setupSmallBtn(delFiberBtn, Color.RED);
        
        removeRowBtn.setForeground(Color.DARK_GRAY);
        removeRowBtn.setMargin(new Insets(0, 4, 0, 4));

        saveFiberBtn.addActionListener(e -> onSaveFiber());
        delFiberBtn.addActionListener(e -> onDelFiber());
        removeRowBtn.addActionListener(e -> onRemove.accept(this));

        add(lblFiber);
        add(fiberSelector);
        add(saveFiberBtn);
        add(delFiberBtn);
        add(Box.createHorizontalStrut(5));
        add(lblPerc);
        add(percent);
        add(removeRowBtn);

        UIHelper.attachDocListener(percent, onAnyChange);
        updateTexts();
    }
    
    void updateTexts() {
        lblFiber.setText(Text.get("lbl_fiber"));
        lblPerc.setText(Text.get("lbl_percent"));
        saveFiberBtn.setToolTipText(Text.get("tip_save_fiber"));
        delFiberBtn.setToolTipText(Text.get("tip_del_fiber"));
        removeRowBtn.setToolTipText(Text.get("tip_rem_row"));
        removeRowBtn.setText(Text.get("btn_remove_fiber"));
    }
    
    private void setupSmallBtn(JButton b, Color fg) {
        b.setMargin(new Insets(0,2,0,2));
        b.setPreferredSize(new Dimension(20, 24));
        b.setForeground(fg);
        b.setFont(new Font("Monospaced", Font.BOLD, 10));
    }
    
    public void reloadFiberDropdown() {
        Object current = fiberSelector.getSelectedItem();
        fiberSelector.removeAllItems();
        for (String f : GarnRechnerProzedural.getLoadedFibers()) {
            fiberSelector.addItem(f);
        }
        if (current != null) fiberSelector.setSelectedItem(current);
    }
    
    private void onSaveFiber() {
        String name = getFiberName();
        if (name.isEmpty()) return;
        
        if (GarnRechnerProzedural.getLoadedFibers().contains(name)) {
            JOptionPane.showMessageDialog(this, String.format(Text.get("dlg_fiber_exists"), name));
            return;
        }
        
        GarnRechnerProzedural.getLoadedFibers().add(name);
        GarnRechnerProzedural.saveFibersToDisk();
        GarnRechnerProzedural.getInstance().refreshFiberDropdowns();
        fiberSelector.setSelectedItem(name);
    }
    
    private void onDelFiber() {
        String name = getFiberName();
        if (!GarnRechnerProzedural.getLoadedFibers().contains(name)) return;
        
        int r = JOptionPane.showConfirmDialog(this, String.format(Text.get("dlg_del_fiber"), name), 
                                              Text.get("dlg_title_del"), JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            GarnRechnerProzedural.getLoadedFibers().remove(name);
            GarnRechnerProzedural.saveFibersToDisk();
            GarnRechnerProzedural.getInstance().refreshFiberDropdowns();
        }
    }
    
    void setRowEnabled(boolean enabled) {
        fiberSelector.setEnabled(enabled);
        percent.setEnabled(enabled);
        saveFiberBtn.setEnabled(enabled);
        delFiberBtn.setEnabled(enabled);
        removeRowBtn.setEnabled(enabled);
    }

    String getFiberName() {
        Object item = fiberSelector.getSelectedItem();
        return (item == null) ? "" : item.toString().trim();
    }
    
    double getPercent() { return UIHelper.parseDouble(percent.getText()); }
    double getPercentSafe() { try { return getPercent(); } catch (Exception ignored) { return 0.0; } }
    void setPercent(double value) { percent.setText(formatSimple(value)); }

    private static String formatSimple(double d) {
        if (Math.abs(d - Math.rint(d)) < 0.0000001) return String.valueOf((int) Math.rint(d));
        return String.format(Text.current.locale, "%.1f", d);
    }
}

class UIHelper {
    static void addSelectAllOnFocus(JTextField field, boolean onlyIfNewFiber) {
        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    if (onlyIfNewFiber) {
                        String txt = field.getText();
                        if (txt.equals(Text.get("new_fiber_def")) || txt.equals("Neue Faser") || txt.equals("New Fiber")) {
                            field.selectAll();
                        }
                    } else {
                        field.selectAll();
                    }
                });
            }
        });
    }

    static void attachDocListener(JTextField field, Runnable onChange) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void removeUpdate(DocumentEvent e) { onChange.run(); }
            @Override public void changedUpdate(DocumentEvent e) { onChange.run(); }
        });
    }

    static double parseDouble(String s) {
        try { 
            String clean = s.trim().replace(',', '.');
            return Double.parseDouble(clean); 
        }
        catch (Exception e) { return 0.0; }
    }
}

class DataLoader {
    
    public static void saveYarnsToFile(Map<String, List<FiberDef>> data, String filename) {
        StringBuilder sb = new StringBuilder("{\n");
        int count = 0;
        for (Map.Entry<String, List<FiberDef>> entry : data.entrySet()) {
            sb.append("  \"").append(entry.getKey()).append("\": {\n");
            List<FiberDef> fibers = entry.getValue();
            for (int i = 0; i < fibers.size(); i++) {
                FiberDef f = fibers.get(i);
                sb.append("    \"f").append(i).append("\": { \"name\": \"").append(f.name)
                  .append("\", \"percentage\": ").append(String.format(Locale.ROOT, "%.2f", f.percentage)).append(" }");
                if (i < fibers.size() - 1) sb.append(",");
                sb.append("\n");
            }
            sb.append("  }");
            if (count++ < data.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("}");
        write(filename, sb.toString());
    }
    
    public static void saveFibersToFile(Set<String> fibers, String filename) {
        StringBuilder sb = new StringBuilder("[\n");
        int c = 0;
        for (String f : fibers) {
            sb.append("  \"").append(f).append("\"");
            if (c++ < fibers.size() - 1) sb.append(",");
            sb.append("\n");
        }
        sb.append("]");
        write(filename, sb.toString());
    }
    
    private static void write(String f, String c) {
        try { Files.write(new File(f).toPath(), c.getBytes(StandardCharsets.UTF_8)); } 
        catch (IOException e) { e.printStackTrace(); }
    }
    
    public static Map<String, List<FiberDef>> loadYarnsFromFile(String filename) {
        Map<String, List<FiberDef>> res = new LinkedHashMap<>();
        String json = read(filename);
        if (json.isEmpty()) return res;
        
        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) json = json.substring(1, json.length()-1);
        
        List<String> blocks = splitByTopLevel(json);
        for (String b : blocks) {
            int idx = b.indexOf(':');
            if(idx < 0) continue;
            String key = clean(b.substring(0, idx));
            String val = b.substring(idx+1).trim();
            if(val.startsWith("{")) val = val.substring(1, val.length()-1);
            
            List<FiberDef> list = new ArrayList<>();
            for(String fb : splitByTopLevel(val)) {
                int i2 = fb.indexOf(':');
                if(i2 < 0) continue;
                String props = fb.substring(i2+1).trim();
                String n = extract(props, "name");
                String p = extract(props, "percentage");
                if(n!=null && p!=null) list.add(new FiberDef(n, Double.parseDouble(p)));
            }
            if(!list.isEmpty()) res.put(key, list);
        }
        return res;
    }
    
    public static Set<String> loadFibersFromFile(String filename) {
        Set<String> res = new TreeSet<>();
        String json = read(filename).trim();
        if (json.startsWith("[") && json.endsWith("]")) {
            json = json.substring(1, json.length()-1);
            String[] parts = json.split(",");
            for(String p : parts) {
                String c = clean(p);
                if(!c.isEmpty()) res.add(c);
            }
        }
        return res;
    }
    
    private static String read(String f) {
        File file = new File(f);
        if(!file.exists()) return "";
        try { return Files.readString(file.toPath(), StandardCharsets.UTF_8); } 
        catch (Exception e) { return ""; }
    }
    
    private static String extract(String s, String key) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"?([^\"},]+)\"?");
        Matcher m = p.matcher(s);
        return m.find() ? m.group(1).trim() : null;
    }
    
    private static String clean(String s) { return s.trim().replace("\"", ""); }
    
    private static List<String> splitByTopLevel(String s) {
        List<String> r = new ArrayList<>();
        int d = 0; boolean q = false; StringBuilder b = new StringBuilder();
        for(char c : s.toCharArray()) {
            if(c=='"') q=!q;
            else if(!q) {
                if(c=='{') d++; else if(c=='}') d--;
                else if(c==',' && d==0) { r.add(b.toString().trim()); b.setLength(0); continue; }
            }
            b.append(c);
        }
        if(b.length()>0) r.add(b.toString().trim());
        return r;
    }
}