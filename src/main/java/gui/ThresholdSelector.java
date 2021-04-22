package gui;//package edu.gmu.swe.intellij.uniqueusages.gui;
//
//import com.intellij.openapi.project.Project;
//import com.intellij.openapi.ui.DialogWrapper;
//import org.jetbrains.annotations.Nullable;
//
//import javax.swing.*;
//import java.awt.event.ActionEvent;
//import java.beans.PropertyChangeListener;
//
//public class ThresholdSelector extends DialogWrapper implements Action {
//
//    private JTextArea txtReleaseNote;
//    private JPanel panelWrapper;
//
//    protected ThresholdSelector(@Nullable Project project) {
//        super(project);
//    }
//
//
//    protected JComponent createCenterPanel() {
//
//        return panelWrapper;
//    }
//
//    @Override
//    protected Action getOKAction() {
//        return this;
//    }
//
//    @Override
//    public void actionPerformed(ActionEvent e) {
//        // save value to project state
//        super.doOKAction();
//    }
//
//    @Override
//    public Object getValue(String key) {
//        return null;
//    }
//
//    @Override
//    public void putValue(String key, Object value) {
//
//    }
//
//    @Override
//    public void setEnabled(boolean b) {
//
//    }
//
//    @Override
//    public boolean isEnabled() {
//        return false;
//    }
//
//    @Override
//    public void addPropertyChangeListener(PropertyChangeListener listener) {
//
//    }
//
//    @Override
//    public void removePropertyChangeListener(PropertyChangeListener listener) {
//
//    }
//
//
//}
