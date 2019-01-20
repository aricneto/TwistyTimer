package com.aricneto.twistytimer.fragment.dialog;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.aricneto.twistify.R;
import com.aricneto.twistytimer.activity.MainActivity;
import com.aricneto.twistytimer.utils.DefaultPrefs;
import com.aricneto.twistytimer.utils.Prefs;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;

/**
 * Dialog that allows a user to select the faces where the cross hints will be shown
 */

public class CrossHintFaceSelectDialog extends DialogFragment {
    private Unbinder mUnbinder;

    @BindView(R.id.top)   View top;
    @BindView(R.id.left)  View left;
    @BindView(R.id.front) View front;
    @BindView(R.id.right) View right;
    @BindView(R.id.back)  View back;
    @BindView(R.id.down)  View down;
    @BindView(R.id.done)  TextView done;

    public static CrossHintFaceSelectDialog newInstance() {
        return new CrossHintFaceSelectDialog();
    }

    // Number of faces that have hint enabled.
    // The code checks if it is >= 1 so the user can't have 0 faces enabled
    // (which would result in a counter-intuitive empty hint)
    int facesSelected = 0;

    private View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View view) {

            boolean isHintOn = true;

            switch (view.getId()) {
                case R.id.top:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled));
                    break;
                case R.id.left:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled));
                    break;
                case R.id.front:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled));
                    break;
                case R.id.right:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled));
                    break;
                case R.id.back:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled));
                    break;
                case R.id.down:
                    isHintOn = Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled));
                    break;
            }

            isHintOn = !isHintOn;

            if (!(!isHintOn && facesSelected == 1)) {
                switch (view.getId()) {
                    case R.id.top:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_top_enabled, isHintOn).apply();
                        toggleSelected(top, isHintOn);
                        break;
                    case R.id.left:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_left_enabled, isHintOn).apply();
                        toggleSelected(left, isHintOn);
                        break;
                    case R.id.front:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_front_enabled, isHintOn).apply();
                        toggleSelected(front, isHintOn);
                        break;
                    case R.id.right:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_right_enabled, isHintOn).apply();
                        toggleSelected(right, isHintOn);
                        break;
                    case R.id.back:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_back_enabled, isHintOn).apply();
                        toggleSelected(back, isHintOn);
                        break;
                    case R.id.down:
                        Prefs.edit().putBoolean(R.string.pk_cross_hint_down_enabled, isHintOn).apply();
                        toggleSelected(down, isHintOn);
                        break;
                }
            }

        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View dialogView = inflater.inflate(R.layout.dialog_cross_hint_face_select, container);
        mUnbinder = ButterKnife.bind(this, dialogView);

        // Color cube
        setColor(top, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_top_color, "FFFFFF")));
        setColor(left, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_left_color, "FF8B24")));
        setColor(front, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_front_color, "02D040")));
        setColor(right, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_right_color, "EC0000")));
        setColor(back, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_back_color, "304FFE")));
        setColor(down, Color.parseColor("#" + Prefs.getString(R.string.pk_cube_down_color, "FDD835")));

        // Set click listeners
        top.setOnClickListener(clickListener);
        left.setOnClickListener(clickListener);
        front.setOnClickListener(clickListener);
        right.setOnClickListener(clickListener);
        back.setOnClickListener(clickListener);
        down.setOnClickListener(clickListener);

        // Set face transparency based on current preference
        initSelected(top, Prefs.getBoolean(R.string.pk_cross_hint_top_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintTopEnabled)));
        initSelected(left, Prefs.getBoolean(R.string.pk_cross_hint_left_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintLeftEnabled)));
        initSelected(front, Prefs.getBoolean(R.string.pk_cross_hint_front_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintFrontEnabled)));
        initSelected(right, Prefs.getBoolean(R.string.pk_cross_hint_right_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintRightEnabled)));
        initSelected(back, Prefs.getBoolean(R.string.pk_cross_hint_back_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintBackEnabled)));
        initSelected(down, Prefs.getBoolean(R.string.pk_cross_hint_down_enabled, DefaultPrefs.getBoolean(R.bool.default_crossHintDownEnabled)));

        done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).onRecreateRequired();
                }
                dismiss();
            }
        });

        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialogView;
    }

    /**
     * Initializes the selected faces and facesSelected count
     * Since facesSelected starts at 0, adding 1 to each face that has hints on will start
     * facesSelected on the correct number. If you were to initialize with toggleSelected, the count
     * will probably be wrong (or negative even)
     * @param face
     *      The face to toggle alpha
     * @param isHintOn
     *      True if face has hints enabled
     */
    private void initSelected(View face, boolean isHintOn) {
        if (isHintOn) {
            face.setAlpha(1f);
            facesSelected++;
        } else {
            face.setAlpha(0.2f);
        }
    }

    /**
     * Toggles alpha value on face and changes faceSelected counter
     *
     * @param face
     *      The face to toggle alpha
     * @param isHintOn
     *      True if face has hints enabled
     */
    private void toggleSelected(View face, boolean isHintOn) {
        if (isHintOn) {
            face.setAlpha(1f);
            facesSelected++;
        } else {
            face.setAlpha(0.2f);
            facesSelected--;
        }
    }

    private void setColor(View view, int color) {
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.square);
        Drawable wrap = DrawableCompat.wrap(drawable);
        DrawableCompat.setTint(wrap, color);
        DrawableCompat.setTintMode(wrap, PorterDuff.Mode.MULTIPLY);
        wrap = wrap.mutate();
        view.setBackground(wrap);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mUnbinder.unbind();
    }
}
