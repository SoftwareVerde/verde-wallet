package com.softwareverde.android.util;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

public class FragmentUtil {
    protected FragmentUtil() { }

    /**
     * <p>Replaces the current fragment on <code>containerId</code> with <code>newFragment</code>.  This
     * state is also added to the back stack with name <code>name</code> (which may be null).</p>
     *
     * <p>Does nothing if the provided fragment manager is null.</p>
     * @param fragmentManager
     * @param newFragment
     * @param containerId
     * @param name
     */
    public static void pushFragment(final FragmentManager fragmentManager, final Fragment newFragment, final int containerId, final String name) {
        if (fragmentManager != null) {
            if ( fragmentManager.isStateSaved() || fragmentManager.isDestroyed() ) { return; }

            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentTransaction.replace(containerId, newFragment);
            fragmentTransaction.addToBackStack(name);
            fragmentTransaction.commit();
        }
    }

    /**
     * <p>Replaces the current fragment on the back stack with the provided fragment.  That is,
     * pressing back while the new fragment is displayed will return to the same state that pressing
     * it before this call.</p>
     * @param fragmentManager
     * @param newFragment
     * @param containerId
     * @param name
     */
    public static void replaceCurrentFragment(final FragmentManager fragmentManager, final Fragment newFragment, final int containerId, final String name) {
        if (fragmentManager != null) {
            if ( fragmentManager.isStateSaved() || fragmentManager.isDestroyed() ) { return; }

            final FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            fragmentManager.popBackStack();
            fragmentTransaction.replace(containerId, newFragment);
            fragmentTransaction.addToBackStack(name);
            fragmentTransaction.commit();
        }
    }

    /**
     * <p>Pops the most current fragment off of the back stack.</p>
     *
     * <p>Does nothing if the provided fragment manager is null.</p>
     * @param fragmentManager
     */
    public static void popFragment(final FragmentManager fragmentManager) {
        if (fragmentManager != null) {
            if ( fragmentManager.isStateSaved() || fragmentManager.isDestroyed() ) { return; }

            fragmentManager.popBackStack();
        }
    }
}
