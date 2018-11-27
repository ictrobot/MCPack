package ethanjones.mcpack;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class UpdateData implements TableModel {

  public static final UpdateData INSTANCE = new UpdateData();

  static enum ChangeType {
    ONLY_SERVER ("Downloaded"),
    CHANGED ("Modified"),
    ONLY_LOCAL ("Deleted"),
    IGNORED ("Ignored"),
    MATCHES ("Matches");

    private final String displayName;

    private ChangeType(String displayName) {
      this.displayName = displayName;
    }

    @Override
    public String toString() {
      return displayName;
    }
  }

  static class Entry implements Comparable<Entry> {
    private final String name;
    private final ChangeType change;

    public Entry(String name, ChangeType change) {
      this.name = name;
      this.change = change;
    }

    @Override
    public int compareTo(Entry e) {
      int changeType = -change.compareTo(e.change);
      return changeType != 0 ? changeType : -name.toLowerCase().compareTo(e.name.toLowerCase());
    }
  }

  private TreeSet<Entry> entries = new TreeSet<>();
  private HashMap<String, Entry> lookup = new HashMap<>();

  private AtomicBoolean changed = new AtomicBoolean(false);
  private ArrayList<TableModelListener> listeners = new ArrayList<>();


  private UpdateData() {}

  public void resetChanges() {
    synchronized (this) {
      entries.clear();
      lookup.clear();
    }
  }

  public void addChange(String name, ChangeType change) {
    name = name.trim();
    synchronized (this) {
      Entry entry = lookup.get(name);
      if (entry != null)
        entries.remove(entry);
      entry = new Entry(name, change);
      entries.add(entry);
      lookup.put(name, entry);
    }

    if (changed.compareAndSet(false, true)) {
      // need to fire listeners
      SwingUtilities.invokeLater(() -> {
        synchronized (UpdateData.this) {
          if (changed.compareAndSet(true, false)) {
            listeners.forEach(l -> l.tableChanged(new TableModelEvent(UpdateData.this)));
          }
        }
      });
    }
  }

  @Override
  public int getRowCount() {
    return entries.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public String getColumnName(int columnIndex) {
    switch (columnIndex) {
      case 0:
        return "Filename";
      case 1:
        return "Action";
      default:
        throw new RuntimeException("Unknown column: " + columnIndex);
    }
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
      case 0:
        return String.class;
      case 1:
        return ChangeType.class;
      default:
        throw new RuntimeException("Unknown column: " + columnIndex);
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return false;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Iterator<Entry> iterator = entries.descendingIterator();
    for (int i = 0; i < rowIndex; i++) iterator.next();
    Entry entry = iterator.next();
    switch (columnIndex) {
      case 0:
        return entry.name;
      case 1:
        return entry.change;
      default:
        throw new RuntimeException("Unknown column: " + columnIndex);
    }
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void addTableModelListener(TableModelListener l) {
    synchronized (this) {
      if (!listeners.contains(l)) listeners.add(l);
    }
  }

  @Override
  public void removeTableModelListener(TableModelListener l) {
    synchronized (this) {
      listeners.remove(l);
    }
  }
}
