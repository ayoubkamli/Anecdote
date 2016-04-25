package io.gresse.hugo.anecdote.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.preference.PreferenceFragmentCompat;

import io.gresse.hugo.anecdote.R;
import io.gresse.hugo.anecdote.event.BusProvider;
import io.gresse.hugo.anecdote.event.ChangeTitleEvent;
import io.gresse.hugo.anecdote.event.UpdateAnecdoteFragmentEvent;
import io.gresse.hugo.anecdote.util.FabricUtils;

/**
 * Anecdote preferences fragment
 *
 * Created by Hugo Gresse on 06/03/16.
 */
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.pref_general);
    }

    @Override
    public void onResume() {
        super.onResume();
        BusProvider.getInstance().post(new ChangeTitleEvent(getString(R.string.action_settings), null));
        FabricUtils.trackFragmentView(this, null);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        BusProvider.getInstance().post(new UpdateAnecdoteFragmentEvent());
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /***************************
     *  implements SharedPreferences.OnSharedPreferenceChangeListener
     ***************************/

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        String value = "";
        if(key.equals(getString(R.string.pref_rowstriping_key))){
            value = String.valueOf(sharedPreferences.getBoolean(key, false));
        } else if(key.equals(getString(R.string.pref_textsize_key))){
            value = String.valueOf(sharedPreferences.getString(key, null));
        }

        FabricUtils.trackSettingChange(key, value);
    }

}
