package dev.tornaco.torscreenrec.ui.tiles;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.widget.Toast;

import dev.nick.tiles.tile.EditTextTileView;
import dev.nick.tiles.tile.QuickTile;
import dev.tornaco.torscreenrec.R;
import dev.tornaco.torscreenrec.pref.SettingsProvider;

/**
 * Created by Tornaco on 2017/7/21.
 * Licensed with Apache.
 */

public class AudioBitRateTile extends QuickTile {
    public AudioBitRateTile(@NonNull final Context context) {
        super(context, null);

        this.titleRes = R.string.title_bitrate_rate;
        this.iconRes = R.drawable.ic_audiotrack_black_24dp;
        this.summary = String.valueOf(SettingsProvider.get().getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K) + "K");

        this.tileView = new EditTextTileView(context) {
            @Override
            protected int getInputType() {
                return InputType.TYPE_CLASS_NUMBER;
            }

            @Override
            protected CharSequence getHint() {
                return String.valueOf(SettingsProvider.get().getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K));
            }

            @Override
            protected CharSequence getDialogTitle() {
                return context.getString(R.string.title_bitrate_rate);
            }

            @Override
            protected void onPositiveButtonClick() {
                super.onPositiveButtonClick();
                String text = getEditText().getText().toString().trim();
                try {
                    int rate = Integer.parseInt(text);

                    if (rate > 1024) {
                        Toast.makeText(context, "<=1024 ~.~", Toast.LENGTH_LONG).show();
                        return;
                    }

                    SettingsProvider.get().putInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K, rate);
                } catch (Throwable e) {
                    Toast.makeText(context, Log.getStackTraceString(e), Toast.LENGTH_LONG).show();
                }

                getSummaryTextView().setText(String.valueOf(SettingsProvider.get().getInt(SettingsProvider.Key.AUDIO_BITRATE_RATE_K) + "K"));
            }
        };
    }
}
