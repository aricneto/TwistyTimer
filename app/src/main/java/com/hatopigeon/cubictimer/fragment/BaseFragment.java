package com.hatopigeon.cubictimer.fragment;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.res.TypedArray;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.appcompat.widget.Toolbar;

import android.graphics.PorterDuff;
import android.util.TypedValue;
import android.view.View;

import com.hatopigeon.cubicify.R;
import com.hatopigeon.cubictimer.activity.MainActivity;
import com.hatopigeon.cubictimer.utils.ThemeUtils;

/**
 * Created by Ari on 06/06/2015.
 */
public class BaseFragment extends Fragment {
    protected int getActionBarSize() {
        Activity activity = getActivity();
        if (activity == null) {
            return 0;
        }


        TypedValue typedValue = new TypedValue();
        int[] textSizeAttr = new int[]{R.attr.actionBarSize};
        int indexOfAttrTextSize = 0;
        TypedArray a = activity.obtainStyledAttributes(typedValue.data, textSizeAttr);
        int actionBarSize = a.getDimensionPixelSize(indexOfAttrTextSize, -1);
        a.recycle();
        return actionBarSize;
    }

    /**
     * This function should be called in every fragment that needs a toolbar
     * Every fragment has its own toolbar, and this function executes the
     * necessary steps to ensure the toolbar is correctly bound to the main
     * activity, which handles the rest (drawer and options menu)
     * <p/>
     * Also, a warning: always bind the toolbar title BEFORE calling this function
     * otherwise, it won't work.
     *
     * @param toolbar The toolbar present in the fragment
     */
    protected void setupToolbarForFragment(Toolbar toolbar) {
        toolbar.setNavigationIcon(R.drawable.ic_outline_settings_24px);
        toolbar.getNavigationIcon().setColorFilter(ThemeUtils.fetchAttrColor(getContext(), R.attr.colorTimerText), PorterDuff.Mode.SRC_IN);

        getMainActivity().setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getMainActivity().openDrawer();
            }
        });
    }

    protected MainActivity getMainActivity() {
        return ((MainActivity) getActivity());
    }

    protected FragmentManager getMainFragmentManager() {
        return getActivity().getFragmentManager();
    }

    protected int getScreenHeight() {
        Activity activity = getActivity();
        if (activity == null) {
            return 0;
        }
        return activity.findViewById(android.R.id.content).getHeight();
    }

}
