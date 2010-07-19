package com.fsck.k9.activity.setup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Vector;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.view.KeyEvent;

import com.fsck.k9.K9;
import com.fsck.k9.Preferences;
import com.fsck.k9.R;
import com.fsck.k9.activity.Accounts;
import com.fsck.k9.activity.DateFormatter;
import com.fsck.k9.activity.K9PreferenceActivity;
import com.fsck.k9.service.MailService;

public class Prefs extends K9PreferenceActivity
{
    private static final String PREFERENCE_LANGUAGE = "language";
    private static final String PREFERENCE_THEME = "theme";
    private static final String PREFERENCE_FONT_SIZE = "font_size";
    private static final String PREFERENCE_DATE_FORMAT = "dateFormat";
    private static final String PREFERENCE_BACKGROUND_OPS = "background_ops";
    private static final String PREFERENCE_DEBUG_LOGGING = "debug_logging";
    private static final String PREFERENCE_SENSITIVE_LOGGING = "sensitive_logging";

    private static final String PREFERENCE_ANIMATIONS = "animations";
    private static final String PREFERENCE_GESTURES = "gestures";
    private static final String PREFERENCE_MANAGE_BACK = "manage_back";
    private static final String PREFERENCE_START_INTEGRATED_INBOX = "start_integrated_inbox";
    private static final String PREFERENCE_MESSAGELIST_STARS = "messagelist_stars";
    private static final String PREFERENCE_MESSAGELIST_CHECKBOXES = "messagelist_checkboxes";
    private static final String PREFERENCE_MESSAGELIST_TOUCHABLE = "messagelist_touchable";

    private static final String PREFERENCE_MESSAGEVIEW_FIXEDWIDTH = "messageview_fixedwidth_font";
    private static final String PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST = "messageview_return_to_list";

    private static final String PREFERENCE_MEASURE_ACCOUNTS = "measure_accounts";
    private static final String PREFERENCE_COUNT_SEARCH = "count_search";
    private static final String PREFERENCE_GALLERY_BUG_WORKAROUND = "use_gallery_bug_workaround";
    private ListPreference mLanguage;
    private ListPreference mTheme;
    private ListPreference mDateFormat;
    private ListPreference mBackgroundOps;
    private CheckBoxPreference mDebugLogging;
    private CheckBoxPreference mSensitiveLogging;
    private CheckBoxPreference mGestures;
    private CheckBoxPreference mManageBack;
    private CheckBoxPreference mStartIntegratedInbox;
    private CheckBoxPreference mAnimations;
    private CheckBoxPreference mStars;
    private CheckBoxPreference mCheckboxes;
    private CheckBoxPreference mTouchable;

    private CheckBoxPreference mFixedWidth;
    private CheckBoxPreference mReturnToList;

    private CheckBoxPreference mMeasureAccounts;
    private CheckBoxPreference mCountSearch;
    private CheckBoxPreference mUseGalleryBugWorkaround;


    private String initBackgroundOps;


    public static void actionPrefs(Context context)
    {
        Intent i = new Intent(context, Prefs.class);
        context.startActivity(i);
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);


        addPreferencesFromResource(R.xml.global_preferences);

        mLanguage = (ListPreference) findPreference(PREFERENCE_LANGUAGE);
        Vector<CharSequence> entryVector = new Vector<CharSequence>(Arrays.asList(mLanguage.getEntries()));
        Vector<CharSequence> entryValueVector = new Vector<CharSequence>(Arrays.asList(mLanguage.getEntryValues()));
        String supportedLanguages[] = getResources().getStringArray(R.array.supported_languages);
        HashSet<String> supportedLanguageSet = new HashSet<String>(Arrays.asList(supportedLanguages));
        for (int i = entryVector.size() - 1; i > -1; --i)
        {
            if (!supportedLanguageSet.contains(entryValueVector.get(i)))
            {
                entryVector.remove(i);
                entryValueVector.remove(i);
            }
        }
        CharSequence dummy[] = new CharSequence[0];
        mLanguage.setEntries(entryVector.toArray(dummy));
        mLanguage.setEntryValues(entryValueVector.toArray(dummy));
        mLanguage.setValue(K9.getK9Language());
        mLanguage.setSummary(mLanguage.getEntry());
        mLanguage.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mLanguage.findIndexOfValue(summary);
                mLanguage.setSummary(mLanguage.getEntries()[index]);
                mLanguage.setValue(summary);
                return false;
            }
        });

        mTheme = (ListPreference) findPreference(PREFERENCE_THEME);
        mTheme.setValue(String.valueOf(K9.getK9Theme() == android.R.style.Theme ? "dark" : "light"));
        mTheme.setSummary(mTheme.getEntry());
        mTheme.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mTheme.findIndexOfValue(summary);
                mTheme.setSummary(mTheme.getEntries()[index]);
                mTheme.setValue(summary);
                return false;
            }
        });

        findPreference(PREFERENCE_FONT_SIZE).setOnPreferenceClickListener(
            new Preference.OnPreferenceClickListener()
        {
            public boolean onPreferenceClick(Preference preference)
            {
                onFontSizeSettings();
                return true;
            }
        });

        mDateFormat = (ListPreference) findPreference(PREFERENCE_DATE_FORMAT);
        String[] formats = DateFormatter.getFormats(this);
        CharSequence[] entries = new CharSequence[formats.length];
        CharSequence[] values = new CharSequence[formats.length];
        for (int i = 0 ; i < formats.length; i++)
        {
            String format = formats[i];
            entries[i] = DateFormatter.getSampleDate(this, format);;
            values[i] = format;
        }
        mDateFormat.setEntries(entries);
        mDateFormat.setEntryValues(values);

        mDateFormat.setValue(DateFormatter.getFormat(this));
        mDateFormat.setSummary(mDateFormat.getEntry());
        mDateFormat.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mDateFormat.findIndexOfValue(summary);
                mDateFormat.setSummary(mDateFormat.getEntries()[index]);
                mDateFormat.setValue(summary);
                return false;
            }
        });

        mBackgroundOps = (ListPreference) findPreference(PREFERENCE_BACKGROUND_OPS);
        initBackgroundOps = K9.getBackgroundOps().toString();
        mBackgroundOps.setValue(initBackgroundOps);
        mBackgroundOps.setSummary(mBackgroundOps.getEntry());
        mBackgroundOps.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener()
        {
            public boolean onPreferenceChange(Preference preference, Object newValue)
            {
                final String summary = newValue.toString();
                int index = mBackgroundOps.findIndexOfValue(summary);
                mBackgroundOps.setSummary(mBackgroundOps.getEntries()[index]);
                mBackgroundOps.setValue(summary);
                return false;
            }
        });

        mDebugLogging = (CheckBoxPreference)findPreference(PREFERENCE_DEBUG_LOGGING);
        mSensitiveLogging = (CheckBoxPreference)findPreference(PREFERENCE_SENSITIVE_LOGGING);

        mDebugLogging.setChecked(K9.DEBUG);
        mSensitiveLogging.setChecked(K9.DEBUG_SENSITIVE);

        mAnimations = (CheckBoxPreference)findPreference(PREFERENCE_ANIMATIONS);
        mAnimations.setChecked(K9.showAnimations());
        mGestures = (CheckBoxPreference)findPreference(PREFERENCE_GESTURES);
        mGestures.setChecked(K9.gesturesEnabled());

        mManageBack = (CheckBoxPreference)findPreference(PREFERENCE_MANAGE_BACK);
        mManageBack.setChecked(K9.manageBack());

        mStartIntegratedInbox = (CheckBoxPreference)findPreference(PREFERENCE_START_INTEGRATED_INBOX);
        mStartIntegratedInbox.setChecked(K9.startIntegratedInbox());


        mStars = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_STARS);
        mStars.setChecked(K9.messageListStars());

        mCheckboxes = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_CHECKBOXES);
        mCheckboxes.setChecked(K9.messageListCheckboxes());

        mTouchable = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGELIST_TOUCHABLE);
        mTouchable.setChecked(K9.messageListTouchable());

        mFixedWidth = (CheckBoxPreference)findPreference(PREFERENCE_MESSAGEVIEW_FIXEDWIDTH);
        mFixedWidth.setChecked(K9.messageViewFixedWidthFont());

        mReturnToList = (CheckBoxPreference) findPreference(PREFERENCE_MESSAGEVIEW_RETURN_TO_LIST);
        mReturnToList.setChecked(K9.messageViewReturnToList());

        mMeasureAccounts = (CheckBoxPreference)findPreference(PREFERENCE_MEASURE_ACCOUNTS);
        mMeasureAccounts.setChecked(K9.measureAccounts());

        mCountSearch = (CheckBoxPreference)findPreference(PREFERENCE_COUNT_SEARCH);
        mCountSearch.setChecked(K9.countSearchMessages());

        mUseGalleryBugWorkaround = (CheckBoxPreference)findPreference(PREFERENCE_GALLERY_BUG_WORKAROUND);
        mUseGalleryBugWorkaround.setChecked(K9.useGalleryBugWorkaround());
    }

    @Override
    public void onResume()
    {
        super.onResume();
    }

    private void saveSettings()
    {
        SharedPreferences preferences = Preferences.getPreferences(this).getPreferences();
        K9.setK9Language(mLanguage.getValue());
        K9.setK9Theme(mTheme.getValue().equals("dark") ? android.R.style.Theme : android.R.style.Theme_Light);
        K9.DEBUG = mDebugLogging.isChecked();
        K9.DEBUG_SENSITIVE = mSensitiveLogging.isChecked();
        boolean needsRefresh = K9.setBackgroundOps(mBackgroundOps.getValue());

        K9.setAnimations(mAnimations.isChecked());
        K9.setGesturesEnabled(mGestures.isChecked());
        K9.setManageBack(mManageBack.isChecked());
        K9.setStartIntegratedInbox(mStartIntegratedInbox.isChecked());
        K9.setMessageListStars(mStars.isChecked());
        K9.setMessageListCheckboxes(mCheckboxes.isChecked());
        K9.setMessageListTouchable(mTouchable.isChecked());

        K9.setMessageViewFixedWidthFont(mFixedWidth.isChecked());
        K9.setMessageViewReturnToList(mReturnToList.isChecked());

        K9.setMeasureAccounts(mMeasureAccounts.isChecked());
        K9.setCountSearchMessages(mCountSearch.isChecked());

        K9.setUseGalleryBugWorkaround(mUseGalleryBugWorkaround.isChecked());

        Editor editor = preferences.edit();
        K9.save(editor);
        DateFormatter.setDateFormat(editor, mDateFormat.getValue());
        editor.commit();
        if (needsRefresh)
        {
            MailService.actionReset(this, null);
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if (keyCode == KeyEvent.KEYCODE_BACK)
        {
            saveSettings();
            if (K9.manageBack())
            {
                Accounts.listAccounts(this);
                finish();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void onFontSizeSettings()
    {
        FontSizeSettings.actionEditSettings(this);
    }

}
