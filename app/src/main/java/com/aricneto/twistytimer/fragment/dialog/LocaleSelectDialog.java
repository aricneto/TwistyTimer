package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.SettingsActivity;
import com.aricneto.twistytimer.utils.LocaleUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Dialog used to select application language
 */

public class LocaleSelectDialog extends DialogFragment {

    @BindView(R.id.en)    TextView en;
    @BindView(R.id.en_US) TextView enUS;
    @BindView(R.id.es)    TextView es;
    @BindView(R.id.de)    TextView de;
    @BindView(R.id.fr)    TextView fr;
    @BindView(R.id.ru)    TextView ru;
    @BindView(R.id.pt_BR) TextView ptBR;
    @BindView(R.id.cs)    TextView cs;
    @BindView(R.id.lt)    TextView lt;
    @BindView(R.id.pl)    TextView pl;
    @BindView(R.id.cn)    TextView cn;
    @BindView(R.id.ca)    TextView ca;
    @BindView(R.id.hu)    TextView hu;
    @BindView(R.id.in)    TextView in;
    @BindView(R.id.iw)    TextView iw;
    @BindView(R.id.nl)    TextView nl;
    @BindView(R.id.sv)    TextView sv;
    @BindView(R.id.val)   TextView val;
    @BindView(R.id.eo)    TextView eo;
    @BindView(R.id.it)    TextView it;

    private Unbinder mUnbinder;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            final String oldLocale = LocaleUtils.getLocale();
            final String newLocale;

            switch (view.getId()) {
                case R.id.en:     newLocale = LocaleUtils.ENGLISH; break;
                case R.id.en_US:  newLocale = LocaleUtils.ENGLISH_USA; break;
                case R.id.es:     newLocale = LocaleUtils.SPANISH; break;
                case R.id.de:     newLocale = LocaleUtils.GERMAN; break;
                case R.id.fr:     newLocale = LocaleUtils.FRENCH; break;
                case R.id.ru:     newLocale = LocaleUtils.RUSSIAN; break;
                case R.id.pt_BR:  newLocale = LocaleUtils.PORTUGUESE_BRAZIL; break;
                case R.id.cs:     newLocale = LocaleUtils.CZECH; break;
                case R.id.lt:     newLocale = LocaleUtils.LITHUANIAN; break;
                case R.id.pl:     newLocale = LocaleUtils.POLISH; break;
                case R.id.cn:     newLocale = LocaleUtils.CHINESE; break;
                case R.id.ca:     newLocale = LocaleUtils.CATALAN; break;
                case R.id.hu:     newLocale = LocaleUtils.HUNGARIAN; break;
                case R.id.in:     newLocale = LocaleUtils.INDONESIAN; break;
                case R.id.iw:     newLocale = LocaleUtils.HEBREW; break;
                case R.id.nl:     newLocale = LocaleUtils.DUTCH; break;
                case R.id.sv:     newLocale = LocaleUtils.SWEDISH; break;
                case R.id.val:    newLocale = LocaleUtils.VALENCIAN; break;
                case R.id.eo:     newLocale = LocaleUtils.ESPERANTO; break;
                case R.id.it:     newLocale = LocaleUtils.ITALIAN; break;
                default:          newLocale = oldLocale;
            }

            Log.d("LocaleSelectDialog", "Selected locale: " + newLocale);

            // If the locale has been changed, then the activity will need to be recreated. The
            // locale can only be applied properly during the inflation of the layouts, so it has
            // to go back to "Activity.updateLocale()" to do that.
            if (!newLocale.equals(oldLocale)) {
                LocaleUtils.setLocale(newLocale);

                ((SettingsActivity) getActivity()).onRecreateRequired();
            }

            dismiss();

        }
    };

    public static LocaleSelectDialog newInstance() {
        return new LocaleSelectDialog();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_settings_change_locale, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        return dialogView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        en.setOnClickListener(clickListener);
        enUS.setOnClickListener(clickListener);
        es.setOnClickListener(clickListener);
        de.setOnClickListener(clickListener);
        fr.setOnClickListener(clickListener);
        ru.setOnClickListener(clickListener);
        ptBR.setOnClickListener(clickListener);
        cs.setOnClickListener(clickListener);
        lt.setOnClickListener(clickListener);
        pl.setOnClickListener(clickListener);
        ca.setOnClickListener(clickListener);
        hu.setOnClickListener(clickListener);
        in.setOnClickListener(clickListener);
        iw.setOnClickListener(clickListener);
        nl.setOnClickListener(clickListener);
        sv.setOnClickListener(clickListener);
        val.setOnClickListener(clickListener);
        eo.setOnClickListener(clickListener);
        cn.setOnClickListener(clickListener);
        it.setOnClickListener(clickListener);


        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
