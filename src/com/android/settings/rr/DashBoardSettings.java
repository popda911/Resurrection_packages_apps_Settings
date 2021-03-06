/*Copyright (C) 2015 The ResurrectionRemix Project
     Licensed under the Apache License, Version 2.0 (the "License");
     you may not use this file except in compliance with the License.
     You may obtain a copy of the License at

          http://www.apache.org/licenses/LICENSE-2.0

     Unless required by applicable law or agreed to in writing, software
     distributed under the License is distributed on an "AS IS" BASIS,
     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     See the License for the specific language governing permissions and
     limitations under the License.
*/
package com.android.settings.rr;

import android.app.AlertDialog;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.os.Process;
import android.content.Context;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.Intent;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.PreferenceCategory;
import androidx.preference.Preference.OnPreferenceChangeListener;
import androidx.preference.SwitchPreference;
import android.provider.Settings;
import android.text.Spannable;
import android.text.TextUtils;
import android.widget.EditText;
import android.net.Uri;
import android.os.UserHandle;
import android.util.Log;

import android.provider.SearchIndexableResource;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.rr.Preferences.*;
import com.android.settings.rr.utils.RRUtils;
import com.android.settings.R;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.search.Indexable;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settingslib.search.SearchIndexable;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
@SearchIndexable
public class DashBoardSettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener, Indexable {
    private static final String TAG = "DashBoardSettings";
    private static final String RR_CONFIG = "rr_config_style";
    private static final String ONE_UI = "settings_spacer";
    private static final String ANIMATION = "rr_config_anim";
    private static final String STYLE = "settings_spacer_style";
    private static final String FONT = "settings_spacer_font_style";
    private static final String SIZE = "settings_display_anim";
    private static final String IMAGE = "settings_spacer_image_style";
    private static final String SEARCHBAR = "settings_searchbar_color";
    private static final String FILE_SPACER_SELECT = "file_spacer_select";
    private static final String CROP = "settings_spacer_image_crop";
    private static final String SEARCHBAR_TINT = "settings_searchbar_tint";
    private static final int REQUEST_PICK_IMAGE = 0;

    private ListPreference mConfig;
    private SystemSettingSwitchPreference mUI;
    private SystemSettingSwitchPreference mSearchTint;
    private ListPreference mAnim;
    private ListPreference mHomeStyle;
    private ListPreference mHomeFont;
    private ListPreference mSize;
    private SystemSettingListPreference mImage;
    private SystemSettingListPreference mImageSize;
    private SystemSettingListPreference mSearchbarColor;
    private Preference mSpacerImage;
    private int mDefaultGradientColor;
    private int mDefaultAccentColor;

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.RESURRECTED;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.rr_dashboard_settings);
        final ContentResolver resolver = getActivity().getContentResolver();
        mConfig = (ListPreference) findPreference(RR_CONFIG);
        mConfig.setValue(String.valueOf(Settings.System.getInt(
                getContentResolver(), Settings.System.RR_CONFIG_STYLE, 0)));
        mConfig.setSummary(mConfig.getEntry());
        mConfig.setOnPreferenceChangeListener(this);
        mDefaultAccentColor = getResources().getColor(
                       com.android.internal.R.color.accent_device_default_light);
        mDefaultGradientColor = getResources().getColor(
                       com.android.internal.R.color.gradient_device_default);
        mUI = (SystemSettingSwitchPreference) findPreference(ONE_UI);
        mSearchTint = (SystemSettingSwitchPreference) findPreference(SEARCHBAR_TINT);

        mSpacerImage = findPreference(FILE_SPACER_SELECT);
        int imagetype = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_IMAGE_STYLE, 0);
        int size = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_IMAGE_CROP, 1);
        mImage = (SystemSettingListPreference) findPreference(IMAGE);
        mImage.setOnPreferenceChangeListener(this);

        mImageSize = (SystemSettingListPreference) findPreference(CROP);
        mImageSize.setOnPreferenceChangeListener(this);

        int accentColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.ACCENT_COLOR, mDefaultGradientColor, UserHandle.USER_CURRENT);

        int gradientColor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.GRADIENT_COLOR_PROP, mDefaultGradientColor, UserHandle.USER_CURRENT);
        String[] defaultgrad = getResources().getStringArray(
                R.array.searchbar_color_entries);
        String[] defaultgradentries = getResources().getStringArray(
                R.array.searchbar_color_values);
        String[] systementries = getResources().getStringArray(
                R.array.searchbar_color_2_entries);
        String[] systemvalues = getResources().getStringArray(
                R.array.searchbar_color_2_values);
        int searchcolor = Settings.System.getIntForUser(getContext().getContentResolver(),
                Settings.System.SETTINGS_SEARCHBAR_COLOR, 0, UserHandle.USER_CURRENT);
        mSearchbarColor = (SystemSettingListPreference) findPreference(SEARCHBAR);
        mSearchbarColor.setOnPreferenceChangeListener(this);
        if (accentColor == gradientColor) {
            mSearchbarColor.setEntries(systementries);
            mSearchbarColor.setEntryValues(systemvalues);
        } else {
            mSearchbarColor.setEntries(defaultgrad);
            mSearchbarColor.setEntryValues(defaultgradentries);
        }

        mAnim = (ListPreference) findPreference(ANIMATION);
        mAnim.setOnPreferenceChangeListener(this);
        int style = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_STYLE, 0);
        mHomeStyle = (ListPreference) findPreference(STYLE);
        mHomeStyle.setOnPreferenceChangeListener(this);
        mHomeFont = (ListPreference) findPreference(FONT);
        mSize = (ListPreference) findPreference(SIZE);
        updatePrefs(style);
        updateImagePrefs(imagetype, style);
        updateSummaries(size);
        updateSearchbar(searchcolor);
        int anim = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.RR_CONFIG_ANIM, 0);
        try {
            if (anim == 0) {
                removePreference("animation");
            } else if (anim == 1) {
                removePreference("preview");
            } else if (anim == 2) {
                removePreference("animation");
                removePreference("preview");
            }
        } catch (Exception e) {}
        mFooterPreferenceMixin.createFooterPreference().setTitle(R.string.switch_ui_warning);
       

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object objValue) {
      if (preference == mConfig) {
            int style = Integer.parseInt((String) objValue);
            Settings.System.putInt(getContentResolver(), Settings.System.RR_CONFIG_STYLE,
            Integer.valueOf((String) objValue));
            mConfig.setValue(String.valueOf(objValue));
            mConfig.setSummary(mConfig.getEntry());
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle(getString(R.string.rr_dashboard_ui));
             alertDialog.setMessage(getString(R.string.rr_tools_message));
             alertDialog.setButton(getString(R.string.rr_reset_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                               Intent fabIntent = new Intent();
                               fabIntent.setClassName("com.android.settings", 
                                     "com.android.settings.Settings$MainSettingsLayoutActivity");
                                startActivity(fabIntent);
                       }
                    });
              alertDialog.setButton(Dialog.BUTTON_NEGATIVE ,getString(R.string.rr_reset_cancel), new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                            return;
                         }
                  });
             alertDialog.show();
            return true;
         } else if (preference == mHomeStyle) {
             int val = Integer.parseInt((String) objValue);
             updatePrefs(val);
             return true;
         } else if (preference == mSearchbarColor) {
             int val = Integer.parseInt((String) objValue);
             updateSearchbar(val);
             return true;
         } else if (preference == mImage) {
             int value = Integer.parseInt((String) objValue);
             int spacer = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_STYLE, 0);
             updateImagePrefs(value, spacer);
             return true;
         }  else if (preference == mImageSize) {
             int value = Integer.parseInt((String) objValue);
             updateSummaries(value);
            return true;
         }  else if (preference == mAnim) {
             AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
             alertDialog.setTitle(getString(R.string.rr_dashboard_ui));
             alertDialog.setMessage(getString(R.string.rr_tools_message));
             alertDialog.setButton(getString(R.string.rr_reset_yes), new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                               Intent fabIntent = new Intent();
                               fabIntent.setClassName("com.android.settings", 
                                     "com.android.settings.Settings$MainSettingsLayoutActivity");
                                startActivity(fabIntent);
                       }
                    });
              alertDialog.setButton(Dialog.BUTTON_NEGATIVE ,getString(R.string.rr_reset_cancel), new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int which) {
                            return;
                         }
                  });
             alertDialog.show();
            return true;
         }
        return false;
    }

     @Override
     public boolean onPreferenceTreeClick(Preference preference) {
        if (preference == mSpacerImage) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_PICK_IMAGE);
            return true;
        }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == REQUEST_PICK_IMAGE) {
            if (resultCode != Activity.RESULT_OK) {
                return;
            }
            final Uri imageUri = result.getData();
            Settings.System.putString(getContentResolver(), Settings.System.SETTINGS_SPACER_CUSTOM, imageUri.toString());
            int style = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_IMAGE_STYLE, 0);
            int spacer = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_STYLE, 0);
            updateImagePrefs(style, spacer);
        }
    }

    private void updateSummaries(int style) {
        if (style == 0) {
            mSpacerImage.setSummary(R.string.file_spacer_select_summary_fill);
        } else {
            mSpacerImage.setSummary(R.string.file_spacer_select_summary);
        }
    }

    private void updateSearchbar(int style) {
        if (style == 0) {
            mSearchTint.setEnabled(false);
        } else {
            mSearchTint.setEnabled(true);
        }
    }


    private void updateImagePrefs(int style, int spacer) {
        String imageUri = Settings.System.getStringForUser(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_CUSTOM,
                UserHandle.USER_CURRENT);
        Log.d(TAG, "SPACER IMAGE STYLE IS!!!"+ style);
        Log.d(TAG, "SPACER  STYLE IS!!!"+ spacer);
        if (spacer == 0) {
            if (style == 3) {
                mSpacerImage.setEnabled(true);
                if (imageUri == null) {
                    mImageSize.setEnabled(false);
                } else {
                   mImageSize.setEnabled(true);
                }
            } else {
                mSpacerImage.setEnabled(false);
                mImageSize.setEnabled(false);
            }
        }
    }

    private void updatePrefs(int which) {
        int style = Settings.System.getInt(getActivity().getContentResolver(),
                Settings.System.SETTINGS_SPACER_IMAGE_STYLE, 0);
        if (which == 2) {
            mHomeFont.setEnabled(true);
            mSize.setEnabled(true);
            mImage.setEnabled(false);
            mSpacerImage.setEnabled(false);
            mImageSize.setEnabled(false);
        } else if (which == 0) {
            mImage.setEnabled(true);
            mHomeFont.setEnabled(false);
            mSize.setEnabled(false);
            updateImagePrefs(style, which);
        } else if (which == 1) {
            mImage.setEnabled(false);
            mHomeFont.setEnabled(false);
            mSize.setEnabled(false);
            mSpacerImage.setEnabled(false);
            mImageSize.setEnabled(false);
        }  else {
            mImage.setEnabled(false);
            mHomeFont.setEnabled(false);
            mSize.setEnabled(false);
            mSpacerImage.setEnabled(false);
            mImageSize.setEnabled(false);
        }
    }
    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
        new BaseSearchIndexProvider() {
            @Override
            public List<SearchIndexableResource> getXmlResourcesToIndex(Context context, boolean enabled) {
                ArrayList<SearchIndexableResource> result =
                    new ArrayList<SearchIndexableResource>();
                    SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.rr_dashboard_settings;
                    result.add(sir);
                    return result;
            }

            @Override
            public List<String> getNonIndexableKeys(Context context) {
                List<String> keys = super.getNonIndexableKeys(context);
                return keys;
            }
        };
}
