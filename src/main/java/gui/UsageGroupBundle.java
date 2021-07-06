package gui;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.util.function.Supplier;

public class UsageGroupBundle extends DynamicBundle {


    @NonNls
    private static final String BUNDLE = "messages.MyBundle";

    private static final UsageGroupBundle INSTANCE = new UsageGroupBundle();

    private UsageGroupBundle() { super(BUNDLE); }

    @NotNull
    public static String message(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    @NotNull
    public static Supplier<String> messagePointer(@NotNull @PropertyKey(resourceBundle = BUNDLE) String key, Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

//    @SuppressWarnings({"AutoBoxing"})
//    public static String getOccurencesString(int usagesCount, int filesCount) {
//        return " (" + message("occurence.info.occurence", usagesCount, filesCount) + ")";
//    }
//
//    @SuppressWarnings({"AutoBoxing"})
//    public static String getReferencesString(int usagesCount, int filesCount) {
//        return " (" + message("occurence.info.reference", usagesCount, filesCount) + ")";
//    }
}
