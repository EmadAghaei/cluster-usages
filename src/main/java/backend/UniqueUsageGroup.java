package backend;

import com.intellij.codeInsight.editorActions.JoinLinesHandler;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.execution.NotSupportedException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageView;
import com.intellij.usages.impl.rules.UsageGroupBase;
import com.sun.tools.javac.util.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class UniqueUsageGroup implements UsageGroup {
    private static final Logger LOG = Logger.getInstance(UniqueUsageGroup.class);

    private String usageDisplayed;
    private final AtomicInteger count = new AtomicInteger(0);

    public UniqueUsageGroup() {
        super();
        throw new NotSupportedException("Must call parameterized ctor.");
    }

    public UniqueUsageGroup(String usageDisplayed) {
        this.usageDisplayed = usageDisplayed;
        this.count.getAndIncrement();
    }
    public String getUsageDisplayed() {
        return usageDisplayed;
    }
    public void setUsageDisplayed(String displayed) {
         this.usageDisplayed=displayed;
    }

    void incrementUsageCount() {
        count.getAndIncrement();
    }


    @Nullable
    @Override
    public Icon getIcon(boolean isOpen) {
        return null;
    }

    @NotNull
    @Override
    public String getText(@Nullable UsageView view) {
//        return String.valueOf(count);
        return usageDisplayed;
    }

    @Nullable
    @Override
    public FileStatus getFileStatus() {
        return null;
    }

    @Override
    public boolean isValid() {
        return true;
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
        synchronized (o) {
            if (!(o instanceof UniqueUsageGroup)) {
                return -1;
            }
            UniqueUsageGroup that = (UniqueUsageGroup) o;
            int same = this.getText(null).compareToIgnoreCase(that.getText(null));
//            if (same == 0) {
                return same;
//            }
//            int order = Comparing.compare(that.count.get(), this.count.get());
//            return order;
        }

    }


    ////        if(that.equals(this)) return 0;
//        int order = Comparing.compare(this.count.get(), that.count.get());
//        return order;
//        if (order != 0) {
//            return -1*order;
//        }
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UniqueUsageGroup that = (UniqueUsageGroup) o;
        return Objects.equals(usageDisplayed, that.usageDisplayed) && Objects.equals(count, that.count);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usageDisplayed, count);
    }
}

