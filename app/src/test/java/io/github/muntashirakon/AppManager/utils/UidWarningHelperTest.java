// SPDX-License-Identifier: GPL-3.0-or-later

package io.github.muntashirakon.AppManager.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.os.UserHandleHidden;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.github.muntashirakon.AppManager.compat.ProcessCompat;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 33)
public class UidWarningHelperTest {

    @Test
    public void testIsSystemUid() {
        // Negative / Invalid UIDs
        assertFalse(UidWarningHelper.isSystemUid(-1));
        assertFalse(UidWarningHelper.isSystemUid(-100));

        // Core system UIDs (AppId < 10000)
        assertTrue(UidWarningHelper.isSystemUid(0)); // root
        assertTrue(UidWarningHelper.isSystemUid(1000)); // system (shared by framework, settings, etc.)
        assertTrue(UidWarningHelper.isSystemUid(1001)); // phone
        assertTrue(UidWarningHelper.isSystemUid(1027)); // nfc
        assertTrue(UidWarningHelper.isSystemUid(ProcessCompat.FIRST_APPLICATION_UID - 1));

        // Normal Application UIDs (AppId >= 10000)
        assertFalse(UidWarningHelper.isSystemUid(ProcessCompat.FIRST_APPLICATION_UID));
        assertFalse(UidWarningHelper.isSystemUid(10001));
        assertFalse(UidWarningHelper.isSystemUid(10150));

        // Multi-user system UIDs (e.g. User 10, UID 1000 -> 1001000)
        int user10SystemUid = UserHandleHidden.getUid(10, 1000);
        assertTrue(UidWarningHelper.isSystemUid(user10SystemUid));

        // Multi-user app UIDs (e.g. User 10, UID 10050 -> 1010050)
        int user10AppUid = UserHandleHidden.getUid(10, 10050);
        assertFalse(UidWarningHelper.isSystemUid(user10AppUid));
    }

    @Test
    public void testWarnIfSharedUid_negativeUidExecutesImmediately() {
        boolean[] executed = new boolean[]{false};
        Context context = ContextUtils.getContext();
        UidWarningHelper.warnIfSharedUid(context, -1, () -> executed[0] = true);
        assertTrue(executed[0]);
    }
}
