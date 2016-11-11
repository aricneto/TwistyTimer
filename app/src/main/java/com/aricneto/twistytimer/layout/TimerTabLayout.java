package com.aricneto.twistytimer.layout;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.util.AttributeSet;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.fragment.TimerFragmentMain;

/**
 * Wrapper class for {@code TabLayout} that intercepts {@code addTab} calls and adds an icon to
 * the tab.
 */
// NOTE: Android Support Library 23.2.0 changed the behaviour when setting tabs from a view
// pager. If the view pager changes, the tabs are removed and re-created, so this is the only
// way to keep adding back the icons short of dispensing with "ViewPager" and using something
// else. See https://code.google.com/p/android/issues/detail?id=202402 for more.
public class TimerTabLayout extends TabLayout {
    public TimerTabLayout(Context context) {
        super(context);
    }

    public TimerTabLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimerTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void addTab(@NonNull Tab tab, int position, boolean setSelected) {
        switch (position) {
            case TimerFragmentMain.TIMER_PAGE:
                tab.setIcon(R.drawable.ic_timer_white_24dp);
                break;
            case TimerFragmentMain.LIST_PAGE:
                tab.setIcon(R.drawable.ic_format_list_bulleted_white_24dp);
                break;
            case TimerFragmentMain.GRAPH_PAGE:
                tab.setIcon(R.drawable.ic_timeline_white_24dp);
                break;
        }

        super.addTab(tab, position, setSelected);
    }
}
