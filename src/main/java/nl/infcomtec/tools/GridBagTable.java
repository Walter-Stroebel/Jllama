/*
 * Copyright (c) 2025 by Walter Stroebel and InfComTec.
 */
package nl.infcomtec.tools;

import com.formdev.flatlaf.FlatDarculaLaf;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.TreeMap;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Rules for any "Coding assistant":
 * <ul>
 * <li>do not use Lambda's</li>
 * <li>do not use Streams</li>
 * <li>use Java 1.7 code level</li>
 * <li>KISS</li>
 * <li>OOP</li>
 * <li>Favor <b>public</b> over get/set, developers are not children (or we
 * teach them to grow up)</li>
 * </ul>
 *
 * @author Walter (after fixing all the LLM introduced bugs).
 */
public class GridBagTable {

    /**
     * Demo
     *
     * @param args Not used.
     */
    public static void main(String[] args) {
        FlatDarculaLaf.setup();
        final String sn = "Stroebel";
        final String gn = "Walter";
        final String in = "W.E.R.";
        GridBagTable gbt = new GridBagTable();
        gbt.labelFont = gbt.labelFont.deriveFont(gbt.labelFont.getSize2D() * 1.1f);
        gbt.textFieldFont = gbt.textFieldFont.deriveFont(gbt.textFieldFont.getSize2D() * 1.1f);
        gbt.addCell("Surname:", true);
        gbt.addCell(new Cell() {
            @Override
            public String get() {
                return sn;
            }

            @Override
            public int spanCols() {
                return 3;
            }
        });
        gbt.newRow();
        gbt.addCell("Given name:", true);
        gbt.addCell(gn, false);
        gbt.addCell("Initials:", true);
        gbt.addCell(in, false);
        System.out.println(gbt.getHTML());
        JFrame frm = new JFrame("Demo");
        frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frm.add(gbt.getPanel());
        frm.pack();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                frm.setVisible(true);
            }
        });
    }

    private int col = 0, row = 0;

    /**
     * Yes: public. If the user needs it, here it is. If the user messes with
     * it, their problem. Row, Column, Cell. Can have null cells.
     */
    public final TreeMap<Integer, TreeMap<Integer, Cell>> table = new TreeMap<>();
    /**
     * Possible overrides for the adventurous user.
     */
    public Font labelFont = UIManager.getFont("Label.font");
    public Font textFieldFont = UIManager.getFont("TextField.font");

    public synchronized void addCell(Cell cell) {
        TreeMap<Integer, Cell> rowMap = table.get(row);
        if (null == rowMap) {
            table.put(row, rowMap = new TreeMap<>());
        }
        rowMap.put(col, cell);
        col++;
    }

    public synchronized void addCell(int r, int c, Cell cell) {
        TreeMap<Integer, Cell> rowMap = table.get(r);
        if (null == rowMap) {
            table.put(r, rowMap = new TreeMap<>());
        }
        rowMap.put(c, cell);
    }

    public synchronized void newRow() {
        col = 0;
        row++;
    }

    public JPanel getPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();

        for (Integer r : table.keySet()) {
            TreeMap<Integer, Cell> rowMap = table.get(r);
            for (Integer c : rowMap.keySet()) {
                Cell cell = rowMap.get(c);
                if (cell != null) {
                    gbc.gridx = c;
                    gbc.gridy = r;
                    gbc.gridwidth = cell.spanCols();
                    gbc.gridheight = cell.spanRows();
                    gbc.fill = GridBagConstraints.BOTH;
                    gbc.insets = new Insets(2, 2, 2, 2);

                    if (cell.isHeader()) {
                        JLabel label = new JLabel(cell.get());
                        label.setFont(labelFont);
                        if (cell.isRight()) {
                            label.setHorizontalAlignment(SwingConstants.RIGHT);
                        }
                        label.setFocusable(false);
                        panel.add(label, gbc);
                    } else {
                        JTextField field = new JTextField(cell.get());
                        field.setFont(textFieldFont);
                        if (cell.isRight()) {
                            field.setHorizontalAlignment(SwingConstants.RIGHT);
                        }
                        field.setFocusable(true);
                        field.setEditable(false);
                        panel.add(field, gbc);
                    }
                }
            }
        }
        return panel;
    }

    /**
     * For debugging or in a servlet or JSP.
     *
     * @return
     */
    public String getHTML() {
        StringBuilder sb = new StringBuilder();
        sb.append("<table border='1' cellspacing='0' cellpadding='5'>");

        for (Integer r : table.keySet()) {
            sb.append("<tr>");
            TreeMap<Integer, Cell> rowMap = table.get(r);
            for (Integer c : rowMap.keySet()) {
                Cell cell = rowMap.get(c);
                if (cell != null) {
                    sb.append("<td");
                    if (cell.spanCols() > 1) {
                        sb.append(" colspan='").append(cell.spanCols()).append("'");
                    }
                    if (cell.spanRows() > 1) {
                        sb.append(" rowspan='").append(cell.spanRows()).append("'");
                    }
                    if (cell.isRight()) {
                        sb.append(" style='text-align:right;'");
                    }
                    sb.append(">");
                    sb.append(cell.get().replace("&", "&amp;").replace("<", "&lt;"));
                    sb.append("</td>");
                }
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    public synchronized void addCell(String value, boolean isHeader) {
        addCell(new Cell() {
            @Override
            public String get() {
                return value;
            }

            @Override
            public boolean isHeader() {
                return isHeader;
            }
        });
    }

    /**
     * This defines a cell, like one in a HTML Table.
     */
    public static abstract class Cell {

        public abstract String get();

        public int spanRows() {
            return 1;
        }

        public int spanCols() {
            return 1;
        }

        public boolean isHeader() {
            return false;
        }

        /**
         * @return True for e.g. numbers
         */
        public boolean isRight() {
            return false;
        }
    }
}
