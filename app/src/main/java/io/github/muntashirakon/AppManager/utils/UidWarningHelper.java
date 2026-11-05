// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.utils;

import android.app.Activity;
import android.content.Context;
import android.os.UserHandleHidden;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import io.github.muntashirakon.AppManager.R;
import io.github.muntashirakon.AppManager.compat.PackageManagerCompat;
import io.github.muntashirakon.AppManager.compat.ProcessCompat;
import io.github.muntashirakon.AppManager.types.UserPackagePair;

public class UidWarningHelper {

    /**
     * Checks if a given UID belongs to the system (App ID < 10000).
     * Correctly handles multi-user userOffsets by checking the App ID.
     */
    public static boolean isSystemUid(int uid) {
        if (uid < 0) return false;
        int appId = UserHandleHidden.getAppId(uid);
        return appId < ProcessCompat.FIRST_APPLICATION_UID;
    }

    /**
     * Warns before performing AppOps operations on a single package if its UID is shared.
     */
    public static void warnIfSharedUid(@NonNull Context context, int uid, @NonNull Runnable onProceed) {
        warnIfSharedUid(context, uid, null, onProceed);
    }

    /**
     * Warns before performing AppOps operations on a single package if its UID is shared.
     *
     * @param currentPackageName The package currently being viewed/edited. If provided,
     *                           the warning dialog will highlight another package sharing the UID
     *                           rather than displaying the current package itself.
     */
    public static void warnIfSharedUid(@NonNull Context context, int uid,
                                       @Nullable String currentPackageName,
                                       @NonNull Runnable onProceed) {
        if (uid < 0) {
            onProceed.run();
            return;
        }
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
        String[] packages = null;
        try {
            packages = PackageManagerCompat.getPackageManager().getPackagesForUid(uid);
        } catch (Exception ignored) {
        }
        if (packages != null && packages.length > 1) {
            String samplePackage = packages[0];
            if (currentPackageName != null) {
                for (String pkg : packages) {
                    if (!pkg.equals(currentPackageName)) {
                        samplePackage = pkg;
                        break;
                    }
                }
            }
            boolean system = isSystemUid(uid);
            int titleRes = system ? R.string.warning_system_shared_uid_title : R.string.are_you_sure;
            String message = system
                    ? context.getString(R.string.warning_system_shared_uid_single, uid, packages.length - 1, samplePackage)
                    : context.getString(R.string.warning_shared_uid_single, uid, packages.length - 1, samplePackage);

            new MaterialAlertDialogBuilder(context)
                    .setTitle(titleRes)
                    .setMessage(message)
                    .setPositiveButton(R.string.yes, (dialog, which) -> onProceed.run())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            onProceed.run();
        }
    }

    /**
     * Warns before performing batch AppOps operations if any package in the list shares a UID.
     */
    public static void warnIfSharedUids(@NonNull Context context, @NonNull List<String> packageNames, @NonNull Runnable onProceed) {
        if (context instanceof Activity) {
            Activity activity = (Activity) context;
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
        }
        boolean hasSystemShared = false;
        boolean hasRegularShared = false;
        int userId = UserHandleHidden.myUserId();

        for (String pkg : packageNames) {
            try {
                int uid = PackageUtils.getAppUid(new UserPackagePair(pkg, userId));
                if (uid >= 0) {
                    String[] packages = PackageManagerCompat.getPackageManager().getPackagesForUid(uid);
                    if (packages != null && packages.length > 1) {
                        if (isSystemUid(uid)) {
                            hasSystemShared = true;
                            break; // Worst case encountered, stop early
                        } else {
                            hasRegularShared = true;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (hasSystemShared) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.warning_system_shared_uid_title)
                    .setMessage(R.string.warning_system_shared_uid_multiple)
                    .setPositiveButton(R.string.yes, (dialog, which) -> onProceed.run())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else if (hasRegularShared) {
            new MaterialAlertDialogBuilder(context)
                    .setTitle(R.string.are_you_sure)
                    .setMessage(R.string.warning_shared_uid_multiple)
                    .setPositiveButton(R.string.yes, (dialog, which) -> onProceed.run())
                    .setNegativeButton(R.string.cancel, null)
                    .show();
        } else {
            onProceed.run();
        }
    }
}
