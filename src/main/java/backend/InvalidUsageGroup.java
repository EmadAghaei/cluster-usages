package backend;

import com.intellij.openapi.vcs.FileStatus;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InvalidUsageGroup implements UsageGroup {
    @Nullable
    @Override
    public Icon getIcon(boolean isOpen) {
        return null;
    }

    @NotNull
    @Override
    public String getText(@Nullable UsageView view) {
        return null;
    }

    @Nullable
    @Override
    public FileStatus getFileStatus() {
        return null;
    }

    @Override
    public boolean isValid() {
        return false;
    }

    @Override
    public void update() {

    }

    @Override
    public void navigate(boolean requestFocus) {

    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }

    @Override
    public int compareTo(@NotNull UsageGroup o) {
        return 0;
    }
}


