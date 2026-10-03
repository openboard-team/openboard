/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dslul.openboard.inputmethod.latin.suggestions;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.dslul.openboard.inputmethod.accessibility.AccessibilityUtils;
import org.dslul.openboard.inputmethod.keyboard.Keyboard;
import org.dslul.openboard.inputmethod.keyboard.KeyboardSwitcher;
import org.dslul.openboard.inputmethod.keyboard.MainKeyboardView;
import org.dslul.openboard.inputmethod.keyboard.MoreKeysPanel;
import org.dslul.openboard.inputmethod.latin.AudioAndHapticFeedbackManager;
import org.dslul.openboard.inputmethod.latin.R;
import org.dslul.openboard.inputmethod.latin.SuggestedWords;
import org.dslul.openboard.inputmethod.latin.SuggestedWords.SuggestedWordInfo;
import org.dslul.openboard.inputmethod.latin.ciphers.A1Z26Cipher;
import org.dslul.openboard.inputmethod.latin.ciphers.AesCbcHmacCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.AesGcmCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.AffineCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.AtbashCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.AutokeyCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.BeaufortCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.BifidCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.BaconianCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.CaesarCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.ColumnarTranspositionCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.GronsfeldCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.EnigmaCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.DiplomaticRedCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.MessageCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.MorseCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.PlayfairCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.PolybiusSquareCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.PurpleCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.QuagmireCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.RailFenceCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.ScytaleCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.Rot47Cipher;
import org.dslul.openboard.inputmethod.latin.ciphers.TrithemiusCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.VigenereCipher;
import org.dslul.openboard.inputmethod.latin.ciphers.ZalgoCipher;
import org.dslul.openboard.inputmethod.latin.common.Constants;
import org.dslul.openboard.inputmethod.latin.define.DebugFlags;
import org.dslul.openboard.inputmethod.latin.settings.Settings;
import org.dslul.openboard.inputmethod.latin.settings.SettingsValues;
import org.dslul.openboard.inputmethod.latin.utils.DialogUtils;
import org.dslul.openboard.inputmethod.latin.suggestions.MoreSuggestionsView.MoreSuggestionsListener;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.core.view.ViewCompat;

public final class SuggestionStripView extends RelativeLayout implements OnClickListener,
        OnLongClickListener {
    public interface Listener {
        void pickSuggestionManually(SuggestedWordInfo word);
        void onCodeInput(int primaryCode, int x, int y, boolean isKeyRepeat);
        void onTextInput(final String rawText);
        CharSequence getSelection();
    }

    static final boolean DBG = DebugFlags.DEBUG_ENABLED;
    private static final float DEBUG_INFO_TEXT_SIZE_IN_DIP = 6.0f;

    private final ViewGroup mSuggestionsStrip;
    private final ImageButton mVoiceKey;
    private final ImageButton mClipboardKey;
    private final Button mCipherKey;
    private final ImageButton mOtherKey;
    private EditText mCipherFocusedInput;
    private PopupWindow mCipherPopupWindow;
    private int mCipherTaskGeneration;
    private final Handler mCipherMainHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mCipherExecutor;
    MainKeyboardView mMainKeyboardView;

    private final View mMoreSuggestionsContainer;
    private final MoreSuggestionsView mMoreSuggestionsView;
    private final MoreSuggestions.Builder mMoreSuggestionsBuilder;

    private final ArrayList<TextView> mWordViews = new ArrayList<>();
    private final ArrayList<TextView> mDebugInfoViews = new ArrayList<>();
    private final ArrayList<View> mDividerViews = new ArrayList<>();

    Listener mListener;
    private SuggestedWords mSuggestedWords = SuggestedWords.getEmptyInstance();
    private int mStartIndexOfMoreSuggestions;

    private final SuggestionStripLayoutHelper mLayoutHelper;
    private final StripVisibilityGroup mStripVisibilityGroup;

    private static class StripVisibilityGroup {
        private final View mSuggestionStripView;
        private final View mSuggestionsStrip;

        public StripVisibilityGroup(final View suggestionStripView,
                final ViewGroup suggestionsStrip) {
            mSuggestionStripView = suggestionStripView;
            mSuggestionsStrip = suggestionsStrip;
            showSuggestionsStrip();
        }

        public void setLayoutDirection(final boolean isRtlLanguage) {
            final int layoutDirection = isRtlLanguage ? ViewCompat.LAYOUT_DIRECTION_RTL
                    : ViewCompat.LAYOUT_DIRECTION_LTR;
            ViewCompat.setLayoutDirection(mSuggestionStripView, layoutDirection);
            ViewCompat.setLayoutDirection(mSuggestionsStrip, layoutDirection);
        }

        public void showSuggestionsStrip() {
            mSuggestionsStrip.setVisibility(VISIBLE);
        }

    }

    /**
     * Construct a {@link SuggestionStripView} for showing suggestions to be picked by the user.
     * @param context
     * @param attrs
     */
    public SuggestionStripView(final Context context, final AttributeSet attrs) {
        this(context, attrs, R.attr.suggestionStripViewStyle);
    }

    public SuggestionStripView(final Context context, final AttributeSet attrs,
            final int defStyle) {
        super(context, attrs, defStyle);

        final LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.suggestions_strip, this);

        mSuggestionsStrip = findViewById(R.id.suggestions_strip);
        mVoiceKey = findViewById(R.id.suggestions_strip_voice_key);
        mClipboardKey = findViewById(R.id.suggestions_strip_clipboard_key);
        mCipherKey = findViewById(R.id.suggestions_strip_cipher_key);
        mOtherKey = findViewById(R.id.suggestions_strip_other_key);
        mStripVisibilityGroup = new StripVisibilityGroup(this, mSuggestionsStrip);

        for (int pos = 0; pos < SuggestedWords.MAX_SUGGESTIONS; pos++) {
            final TextView word = new TextView(context, null, R.attr.suggestionWordStyle);
            word.setContentDescription(getResources().getString(R.string.spoken_empty_suggestion));
            word.setOnClickListener(this);
            word.setOnLongClickListener(this);
            mWordViews.add(word);
            final View divider = inflater.inflate(R.layout.suggestion_divider, null);
            mDividerViews.add(divider);
            final TextView info = new TextView(context, null, R.attr.suggestionWordStyle);
            info.setTextColor(Color.WHITE);
            info.setTextSize(TypedValue.COMPLEX_UNIT_DIP, DEBUG_INFO_TEXT_SIZE_IN_DIP);
            mDebugInfoViews.add(info);
        }

        mLayoutHelper = new SuggestionStripLayoutHelper(
                context, attrs, defStyle, mWordViews, mDividerViews, mDebugInfoViews);

        mMoreSuggestionsContainer = inflater.inflate(R.layout.more_suggestions, null);
        mMoreSuggestionsView = mMoreSuggestionsContainer
                .findViewById(R.id.more_suggestions_view);
        mMoreSuggestionsBuilder = new MoreSuggestions.Builder(context, mMoreSuggestionsView);

        final Resources res = context.getResources();
        mMoreSuggestionsModalTolerance = res.getDimensionPixelOffset(
                R.dimen.config_more_suggestions_modal_tolerance);
        mMoreSuggestionsSlidingDetector = new GestureDetector(
                context, mMoreSuggestionsSlidingListener);

        final TypedArray keyboardAttr = context.obtainStyledAttributes(attrs,
                R.styleable.Keyboard, defStyle, R.style.SuggestionStripView);
        final Drawable iconVoice = keyboardAttr.getDrawable(R.styleable.Keyboard_iconShortcutKey);
        final Drawable iconIncognito = keyboardAttr.getDrawable(R.styleable.Keyboard_iconIncognitoKey);
        final Drawable iconClipboard = keyboardAttr.getDrawable(R.styleable.Keyboard_iconClipboardNormalKey);
        keyboardAttr.recycle();
        mVoiceKey.setImageDrawable(iconVoice);
        mVoiceKey.setOnClickListener(this);
        mClipboardKey.setImageDrawable(iconClipboard);
        mClipboardKey.setOnClickListener(this);
        mClipboardKey.setOnLongClickListener(this);
        mCipherKey.setOnClickListener(this);

        mOtherKey.setImageDrawable(iconIncognito);
    }

    /**
     * A connection back to the input method.
     * @param listener
     */
    public void setListener(final Listener listener, final View inputView) {
        mListener = listener;
        mMainKeyboardView = inputView.findViewById(R.id.keyboard_view);
    }

    public void updateVisibility(final boolean shouldBeVisible, final boolean isFullscreenMode) {
        final int visibility = shouldBeVisible ? VISIBLE : (isFullscreenMode ? GONE : INVISIBLE);
        setVisibility(visibility);
        final SettingsValues currentSettingsValues = Settings.getInstance().getCurrent();
        mVoiceKey.setVisibility(currentSettingsValues.mShowsVoiceInputKey ? VISIBLE : GONE);
        mClipboardKey.setVisibility(currentSettingsValues.mShowsClipboardKey ? VISIBLE : (mVoiceKey.getVisibility() == GONE ? INVISIBLE : GONE));
        mCipherKey.setVisibility(currentSettingsValues.mShowsCipherKey ? VISIBLE : GONE);
        mOtherKey.setVisibility(currentSettingsValues.mIncognitoModeEnabled ? VISIBLE : INVISIBLE);
    }

    public void setSuggestions(final SuggestedWords suggestedWords, final boolean isRtlLanguage) {
        clear();
        mStripVisibilityGroup.setLayoutDirection(isRtlLanguage);
        mSuggestedWords = suggestedWords;
        mStartIndexOfMoreSuggestions = mLayoutHelper.layoutAndReturnStartIndexOfMoreSuggestions(
                getContext(), mSuggestedWords, mSuggestionsStrip, this);
        mStripVisibilityGroup.showSuggestionsStrip();
    }

    public void setMoreSuggestionsHeight(final int remainingHeight) {
        mLayoutHelper.setMoreSuggestionsHeight(remainingHeight);
    }

    public void clear() {
        mSuggestionsStrip.removeAllViews();
        removeAllDebugInfoViews();
        mStripVisibilityGroup.showSuggestionsStrip();
        dismissMoreSuggestionsPanel();
    }

    private void removeAllDebugInfoViews() {
        // The debug info views may be placed as children views of this {@link SuggestionStripView}.
        for (final View debugInfoView : mDebugInfoViews) {
            final ViewParent parent = debugInfoView.getParent();
            if (parent instanceof ViewGroup) {
                ((ViewGroup)parent).removeView(debugInfoView);
            }
        }
    }

    private final MoreSuggestionsListener mMoreSuggestionsListener = new MoreSuggestionsListener() {
        @Override
        public void onSuggestionSelected(final SuggestedWordInfo wordInfo) {
            mListener.pickSuggestionManually(wordInfo);
            dismissMoreSuggestionsPanel();
        }

        @Override
        public void onCancelInput() {
            dismissMoreSuggestionsPanel();
        }
    };

    private final MoreKeysPanel.Controller mMoreSuggestionsController =
            new MoreKeysPanel.Controller() {
        @Override
        public void onDismissMoreKeysPanel() {
            mMainKeyboardView.onDismissMoreKeysPanel();
        }

        @Override
        public void onShowMoreKeysPanel(final MoreKeysPanel panel) {
            mMainKeyboardView.onShowMoreKeysPanel(panel);
        }

        @Override
        public void onCancelMoreKeysPanel() {
            dismissMoreSuggestionsPanel();
        }
    };

    public boolean isShowingMoreSuggestionPanel() {
        return mMoreSuggestionsView.isShowingInParent();
    }

    public void dismissMoreSuggestionsPanel() {
        mMoreSuggestionsView.dismissMoreKeysPanel();
    }

    @Override
    public boolean onLongClick(final View view) {
        if (view == mClipboardKey) {
            ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clipData = clipboardManager.getPrimaryClip();
            if (clipData != null && clipData.getItemCount() > 0 && clipData.getItemAt(0) != null) {
                String clipString = clipData.getItemAt(0).coerceToText(getContext()).toString();
                if (clipString.length() == 1) {
                    mListener.onTextInput(clipString);
                } else if (clipString.length() > 1) {
                    //awkward workaround
                    mListener.onTextInput(clipString.substring(0, clipString.length() - 1));
                    mListener.onTextInput(clipString.substring(clipString.length() - 1));
                }
            }
            AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                    Constants.NOT_A_CODE, this);
            return true;
        }
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.NOT_A_CODE, this);
        return showMoreSuggestions();
    }

    boolean showMoreSuggestions() {
        final Keyboard parentKeyboard = mMainKeyboardView.getKeyboard();
        if (parentKeyboard == null) {
            return false;
        }
        final SuggestionStripLayoutHelper layoutHelper = mLayoutHelper;
        if (mSuggestedWords.size() <= mStartIndexOfMoreSuggestions) {
            return false;
        }
        final int stripWidth = getWidth();
        final View container = mMoreSuggestionsContainer;
        final int maxWidth = stripWidth - container.getPaddingLeft() - container.getPaddingRight();
        final MoreSuggestions.Builder builder = mMoreSuggestionsBuilder;
        builder.layout(mSuggestedWords, mStartIndexOfMoreSuggestions, maxWidth,
                (int)(maxWidth * layoutHelper.mMinMoreSuggestionsWidth),
                layoutHelper.getMaxMoreSuggestionsRow(), parentKeyboard);
        mMoreSuggestionsView.setKeyboard(builder.build());
        container.measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        final MoreKeysPanel moreKeysPanel = mMoreSuggestionsView;
        final int pointX = stripWidth / 2;
        final int pointY = -layoutHelper.mMoreSuggestionsBottomGap;
        moreKeysPanel.showMoreKeysPanel(this, mMoreSuggestionsController, pointX, pointY,
                mMoreSuggestionsListener);
        mOriginX = mLastX;
        mOriginY = mLastY;
        for (int i = 0; i < mStartIndexOfMoreSuggestions; i++) {
            mWordViews.get(i).setPressed(false);
        }
        return true;
    }

    // Working variables for {@link onInterceptTouchEvent(MotionEvent)} and
    // {@link onTouchEvent(MotionEvent)}.
    private int mLastX;
    private int mLastY;
    private int mOriginX;
    private int mOriginY;
    private final int mMoreSuggestionsModalTolerance;
    private boolean mNeedsToTransformTouchEventToHoverEvent;
    private boolean mIsDispatchingHoverEventToMoreSuggestions;
    private final GestureDetector mMoreSuggestionsSlidingDetector;
    private final GestureDetector.OnGestureListener mMoreSuggestionsSlidingListener =
            new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent down, MotionEvent me, float deltaX, float deltaY) {
            final float dy = me.getY() - down.getY();
            if (deltaY > 0 && dy < 0) {
                return showMoreSuggestions();
            }
            return false;
        }
    };

    @Override
    public boolean onInterceptTouchEvent(final MotionEvent me) {
        // Detecting sliding up finger to show {@link MoreSuggestionsView}.
        if (!mMoreSuggestionsView.isShowingInParent()) {
            mLastX = (int)me.getX();
            mLastY = (int)me.getY();
            return mMoreSuggestionsSlidingDetector.onTouchEvent(me);
        }
        if (mMoreSuggestionsView.isInModalMode()) {
            return false;
        }

        final int action = me.getAction();
        final int index = me.getActionIndex();
        final int x = (int)me.getX(index);
        final int y = (int)me.getY(index);
        if (Math.abs(x - mOriginX) >= mMoreSuggestionsModalTolerance
                || mOriginY - y >= mMoreSuggestionsModalTolerance) {
            // Decided to be in the sliding suggestion mode only when the touch point has been moved
            // upward. Further {@link MotionEvent}s will be delivered to
            // {@link #onTouchEvent(MotionEvent)}.
            mNeedsToTransformTouchEventToHoverEvent =
                    AccessibilityUtils.Companion.getInstance().isTouchExplorationEnabled();
            mIsDispatchingHoverEventToMoreSuggestions = false;
            return true;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            // Decided to be in the modal input mode.
            mMoreSuggestionsView.setModalMode();
        }
        return false;
    }

    @Override
    public boolean dispatchPopulateAccessibilityEvent(final AccessibilityEvent event) {
        // Don't populate accessibility event with suggested words and voice key.
        return true;
    }

    @Override
    public boolean onTouchEvent(final MotionEvent me) {
        if (!mMoreSuggestionsView.isShowingInParent()) {
            // Ignore any touch event while more suggestions panel hasn't been shown.
            // Detecting sliding up is done at {@link #onInterceptTouchEvent}.
            return true;
        }
        // In the sliding input mode. {@link MotionEvent} should be forwarded to
        // {@link MoreSuggestionsView}.
        final int index = me.getActionIndex();
        final int x = mMoreSuggestionsView.translateX((int)me.getX(index));
        final int y = mMoreSuggestionsView.translateY((int)me.getY(index));
        me.setLocation(x, y);
        if (!mNeedsToTransformTouchEventToHoverEvent) {
            mMoreSuggestionsView.onTouchEvent(me);
            return true;
        }
        // In sliding suggestion mode with accessibility mode on, a touch event should be
        // transformed to a hover event.
        final int width = mMoreSuggestionsView.getWidth();
        final int height = mMoreSuggestionsView.getHeight();
        final boolean onMoreSuggestions = (x >= 0 && x < width && y >= 0 && y < height);
        if (!onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Just drop this touch event because dispatching hover event isn't started yet and
            // the touch event isn't on {@link MoreSuggestionsView}.
            return true;
        }
        final int hoverAction;
        if (onMoreSuggestions && !mIsDispatchingHoverEventToMoreSuggestions) {
            // Transform this touch event to a hover enter event and start dispatching a hover
            // event to {@link MoreSuggestionsView}.
            mIsDispatchingHoverEventToMoreSuggestions = true;
            hoverAction = MotionEvent.ACTION_HOVER_ENTER;
        } else if (me.getActionMasked() == MotionEvent.ACTION_UP) {
            // Transform this touch event to a hover exit event and stop dispatching a hover event
            // after this.
            mIsDispatchingHoverEventToMoreSuggestions = false;
            mNeedsToTransformTouchEventToHoverEvent = false;
            hoverAction = MotionEvent.ACTION_HOVER_EXIT;
        } else {
            // Transform this touch event to a hover move event.
            hoverAction = MotionEvent.ACTION_HOVER_MOVE;
        }
        me.setAction(hoverAction);
        mMoreSuggestionsView.onHoverEvent(me);
        return true;
    }

    @Override
    public void onClick(final View view) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
                Constants.CODE_UNSPECIFIED, this);
        if (view == mVoiceKey) {
            mListener.onCodeInput(Constants.CODE_SHORTCUT,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false /* isKeyRepeat */);
            return;
        }
        if (view == mClipboardKey) {
            mListener.onCodeInput(Constants.CODE_CLIPBOARD,
                    Constants.SUGGESTION_STRIP_COORDINATE, Constants.SUGGESTION_STRIP_COORDINATE,
                    false /* isKeyRepeat */);
            return;
        }
        if (view == mCipherKey) {
            showCipherDialog();
            return;
        }

        final Object tag = view.getTag();
        // {@link Integer} tag is set at
        // {@link SuggestionStripLayoutHelper#setupWordViewsTextAndColor(SuggestedWords,int)} and
        // {@link SuggestionStripLayoutHelper#layoutPunctuationSuggestions(SuggestedWords,ViewGroup}
        if (tag instanceof Integer) {
            final int index = (Integer) tag;
            if (index >= mSuggestedWords.size()) {
                return;
            }
            final SuggestedWordInfo wordInfo = mSuggestedWords.getInfo(index);
            mListener.pickSuggestionManually(wordInfo);
        }
    }

    public boolean handleCipherCodeInput(final int codePoint) {
        if (mCipherPopupWindow == null || !mCipherPopupWindow.isShowing()
                || mCipherFocusedInput == null) {
            return false;
        }
        final Editable editable = mCipherFocusedInput.getText();
        final int selectionStart = Math.max(0, mCipherFocusedInput.getSelectionStart());
        final int selectionEnd = Math.max(0, mCipherFocusedInput.getSelectionEnd());
        final int start = Math.min(selectionStart, selectionEnd);
        final int end = Math.max(selectionStart, selectionEnd);
        if (codePoint == Constants.CODE_DELETE) {
            if (start != end) {
                editable.delete(start, end);
            } else if (start > 0) {
                final int deleteStart = Character.offsetByCodePoints(editable, start, -1);
                editable.delete(deleteStart, start);
            }
            return true;
        }
        final String text;
        if (codePoint == Constants.CODE_ENTER) {
            text = "\n";
        } else if (codePoint == Constants.CODE_SPACE) {
            text = " ";
        } else if (codePoint > 0) {
            text = new String(Character.toChars(codePoint));
        } else {
            return false;
        }
        editable.replace(start, end, text);
        return true;
    }

    private void showCipherDialog() {
        if (mCipherPopupWindow != null && mCipherPopupWindow.isShowing()) {
            mCipherPopupWindow.dismiss();
        }
        final Context context = getContext();
        final SharedPreferences prefs = Settings.getInstance().getSharedPreferences();

        final ScrollView scrollView = new ScrollView(context);
        final LinearLayout container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        scrollView.addView(container);
        final int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 16,
                getResources().getDisplayMetrics());
        container.setPadding(padding, padding / 2, padding, 0);

        final Button closeButton = new Button(context);
        closeButton.setText(R.string.close);
        container.addView(closeButton);

        final CheckBox directInputToggle = new CheckBox(context);
        directInputToggle.setText(R.string.cipher_direct_input);
        directInputToggle.setChecked(prefs.getBoolean(Settings.PREF_CIPHER_DIRECT_INPUT, false));
        directInputToggle.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                prefs.edit().putBoolean(Settings.PREF_CIPHER_DIRECT_INPUT,
                        directInputToggle.isChecked()).apply();
            }
        });
        container.addView(directInputToggle);

        final TextView directModeLabel = new TextView(context, null, R.attr.suggestionWordStyle);
        directModeLabel.setText(R.string.cipher_direct_mode);
        container.addView(directModeLabel);

        final Spinner directModeSpinner = new Spinner(context);
        final String[] directModeLabels = getResources().getStringArray(
                R.array.cipher_direct_mode_labels);
        final ArrayAdapter<String> directModeAdapter = new ArrayAdapter<>(context,
                android.R.layout.simple_spinner_item, directModeLabels);
        directModeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        directModeSpinner.setAdapter(directModeAdapter);
        final String[] directModeValues = getResources().getStringArray(
                R.array.cipher_direct_mode_values);
        final String currentMode = prefs.getString(Settings.PREF_CIPHER_DIRECT_MODE,
                Settings.CIPHER_MODE_CAESAR);
        for (int i = 0; i < directModeValues.length; i++) {
            if (directModeValues[i].equals(currentMode)) {
                directModeSpinner.setSelection(i);
                break;
            }
        }
        directModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putString(Settings.PREF_CIPHER_DIRECT_MODE,
                        directModeValues[position]).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        container.addView(directModeSpinner);

        final Button caesarButton = new Button(context);
        caesarButton.setText(R.string.caesar_cipher);
        container.addView(caesarButton);

        final LinearLayout caesarSettings = new LinearLayout(context);
        caesarSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(caesarSettings);
        caesarSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        caesarSettings.addView(messageInput);

        final EditText shiftInput = new EditText(context);
        shiftInput.setHint(R.string.caesar_shift);
        shiftInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        shiftInput.setText(String.valueOf(prefs.getInt(Settings.PREF_CAESAR_CIPHER_SHIFT, 3)));
        styleCipherInput(shiftInput);
        caesarSettings.addView(shiftInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        caesarSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        caesarSettings.addView(decryptButton);
        container.addView(caesarSettings);

        addEnigmaPanel(context, container, prefs, false);
        addEnigmaPanel(context, container, prefs, true);
        addSimpleCipherPanel(context, container, R.string.baconian_cipher, new BaconianCipher());
        addSimpleCipherPanel(context, container, R.string.morse_code, new MorseCipher());
        addQuagmirePanel(context, container, prefs, R.string.quagmire_i, QuagmireCipher.Variant.I);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_ii, QuagmireCipher.Variant.II);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_iii, QuagmireCipher.Variant.III);
        addQuagmirePanel(context, container, prefs, R.string.quagmire_iv, QuagmireCipher.Variant.IV);
        addSimpleCipherPanel(context, container, R.string.atbash_cipher, new AtbashCipher());
        addVigenerePanel(context, container, prefs);
        addAffinePanel(context, container, prefs);
        addSimpleCipherPanel(context, container, R.string.a1z26_cipher, new A1Z26Cipher());
        addRailFencePanel(context, container, prefs);
        addColumnarTranspositionPanel(context, container, prefs);
        addPolybiusPanel(context, container, prefs);
        addParameterizedCipherPanel(context, container, prefs, R.string.autokey_cipher,
                R.string.cipher_keyword_hint, Settings.PREF_AUTOKEY_KEYWORD, "QUEENLY",
                InputType.TYPE_CLASS_TEXT, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new AutokeyCipher(parameter);
                    }
                });
        addParameterizedCipherPanel(context, container, prefs, R.string.beaufort_cipher,
                R.string.cipher_keyword_hint, Settings.PREF_BEAUFORT_KEYWORD, "FORT",
                InputType.TYPE_CLASS_TEXT, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new BeaufortCipher(parameter);
                    }
                });
        addParameterizedCipherPanel(context, container, prefs, R.string.gronsfeld_cipher,
                R.string.gronsfeld_key_hint, Settings.PREF_GRONSFELD_KEY, "31415",
                InputType.TYPE_CLASS_NUMBER, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new GronsfeldCipher(parameter);
                    }
                });
        addParameterizedCipherPanel(context, container, prefs, R.string.scytale_cipher,
                R.string.scytale_columns_hint, Settings.PREF_SCYTALE_COLUMNS, "4",
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED,
                new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new ScytaleCipher(Math.max(2, readInt(parameter, 4)));
                    }
                });
        addSimpleCipherPanel(context, container, R.string.rot47_cipher, new Rot47Cipher());
        addSimpleCipherPanel(context, container, R.string.trithemius_cipher,
                new TrithemiusCipher());
        addParameterizedCipherPanel(context, container, prefs, R.string.playfair_cipher,
                R.string.cipher_keyword_hint, Settings.PREF_PLAYFAIR_KEYWORD, "PLAYFAIR",
                InputType.TYPE_CLASS_TEXT, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new PlayfairCipher(parameter);
                    }
                });
        addParameterizedCipherPanel(context, container, prefs, R.string.bifid_cipher,
                R.string.cipher_keyword_hint, Settings.PREF_BIFID_KEYWORD, "BIFID",
                InputType.TYPE_CLASS_TEXT, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new BifidCipher(parameter);
                    }
                });
        addParameterizedCipherPanel(context, container, prefs, R.string.zalgo_cipher,
                R.string.zalgo_intensity_hint, Settings.PREF_ZALGO_INTENSITY, "9",
                InputType.TYPE_CLASS_NUMBER, new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new ZalgoCipher(Math.max(0, readInt(parameter, 9)));
                    }
                });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            addPasswordCipherPanel(context, container, R.string.aes_gcm_cipher,
                    new ParameterCipherFactory() {
                        @Override public MessageCipher create(String parameter) {
                            return new AesGcmCipher(parameter);
                        }
                    });
        }
        addPasswordCipherPanel(context, container, R.string.aes_cbc_hmac_cipher,
                new ParameterCipherFactory() {
                    @Override public MessageCipher create(String parameter) {
                        return new AesCbcHmacCipher(parameter);
                    }
                });
        addDiplomaticRedPanel(context, container, prefs);
        addPurplePanel(context, container, prefs);

        shiftInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                saveCaesarShift(prefs, shiftInput);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        caesarButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                caesarSettings.setVisibility(
                        caesarSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputCaesarText(prefs, messageInput, shiftInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputCaesarText(prefs, messageInput, shiftInput, true);
            }
        });

        final int rootHeight = getRootView() == null ? 0 : getRootView().getHeight();
        final int maxPopupHeight = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                360, getResources().getDisplayMetrics());
        final int popupHeight = rootHeight <= 0 ? ViewGroup.LayoutParams.WRAP_CONTENT
                : Math.min(maxPopupHeight, Math.max(getHeight(), rootHeight / 2));
        final PopupWindow popupWindow = new PopupWindow(scrollView,
                ViewGroup.LayoutParams.MATCH_PARENT, popupHeight, false);
        mCipherPopupWindow = popupWindow;
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.setOutsideTouchable(false);
        popupWindow.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        popupWindow.setOnDismissListener(new PopupWindow.OnDismissListener() {
            @Override public void onDismiss() {
                mCipherTaskGeneration++;
                mCipherFocusedInput = null;
                if (mCipherPopupWindow == popupWindow) {
                    mCipherPopupWindow = null;
                }
            }
        });
        closeButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                popupWindow.dismiss();
            }
        });
        popupWindow.showAsDropDown(this, 0, -getHeight() - popupHeight);
    }



    private void prefillMessageInput(final EditText messageInput) {
        if (mListener == null) {
            return;
        }
        final CharSequence selectedText = mListener.getSelection();
        if (selectedText != null && selectedText.length() > 0) {
            messageInput.setText(selectedText);
        }
    }

    private void styleCipherPanel(final LinearLayout panel) {
        final int padding = (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8,
                getResources().getDisplayMetrics());
        panel.setPadding(padding, padding, padding, padding);
        panel.setBackgroundColor(0xFFEFEFEF);
    }

    private void styleCipherInput(final EditText input) {
        input.setTextColor(Color.BLACK);
        input.setHintTextColor(Color.DKGRAY);
        input.setBackgroundColor(Color.WHITE);
        input.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                mCipherFocusedInput = input;
                input.requestFocus();
            }
        });
        input.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    mCipherFocusedInput = input;
                } else if (mCipherFocusedInput == input) {
                    mCipherFocusedInput = null;
                }
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            input.setShowSoftInputOnFocus(false);
        }
    }

    private void addSimpleCipherPanel(final Context context, final LinearLayout container,
            final int titleResId, final MessageCipher cipher) {
        final Button cipherButton = new Button(context);
        cipherButton.setText(titleResId);
        container.addView(cipherButton);

        final LinearLayout cipherSettings = new LinearLayout(context);
        cipherSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(cipherSettings);
        cipherSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        cipherSettings.addView(messageInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        cipherSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        cipherSettings.addView(decryptButton);
        container.addView(cipherSettings);

        cipherButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                cipherSettings.setVisibility(
                        cipherSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputText(cipher.encrypt(messageInput.getText().toString()));
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputText(cipher.decrypt(messageInput.getText().toString()));
            }
        });
    }

    private void addQuagmirePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs, final int titleResId, final QuagmireCipher.Variant variant) {
        final Button quagmireButton = new Button(context);
        quagmireButton.setText(titleResId);
        container.addView(quagmireButton);

        final LinearLayout quagmireSettings = new LinearLayout(context);
        quagmireSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(quagmireSettings);
        quagmireSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        quagmireSettings.addView(messageInput);

        final EditText plainKeywordInput = new EditText(context);
        plainKeywordInput.setHint(R.string.quagmire_plain_keyword_hint);
        plainKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_PLAIN_KEYWORD, "KEYWORD"));
        styleCipherInput(plainKeywordInput);
        quagmireSettings.addView(plainKeywordInput);

        final EditText cipherKeywordInput = new EditText(context);
        cipherKeywordInput.setHint(R.string.quagmire_cipher_keyword_hint);
        cipherKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_CIPHER_KEYWORD, "CIPHER"));
        styleCipherInput(cipherKeywordInput);
        quagmireSettings.addView(cipherKeywordInput);

        final EditText indicatorKeywordInput = new EditText(context);
        indicatorKeywordInput.setHint(R.string.quagmire_indicator_keyword_hint);
        indicatorKeywordInput.setText(prefs.getString(Settings.PREF_QUAGMIRE_INDICATOR_KEYWORD, "KEY"));
        styleCipherInput(indicatorKeywordInput);
        quagmireSettings.addView(indicatorKeywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        quagmireSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        quagmireSettings.addView(decryptButton);
        container.addView(quagmireSettings);

        quagmireButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                quagmireSettings.setVisibility(
                        quagmireSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputQuagmireText(prefs, variant, messageInput, plainKeywordInput,
                        cipherKeywordInput, indicatorKeywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputQuagmireText(prefs, variant, messageInput, plainKeywordInput,
                        cipherKeywordInput, indicatorKeywordInput, true);
            }
        });
    }

    private void outputQuagmireText(final SharedPreferences prefs,
            final QuagmireCipher.Variant variant, final EditText messageInput,
            final EditText plainKeywordInput, final EditText cipherKeywordInput,
            final EditText indicatorKeywordInput, final boolean decrypt) {
        prefs.edit()
                .putString(Settings.PREF_QUAGMIRE_PLAIN_KEYWORD,
                        plainKeywordInput.getText().toString())
                .putString(Settings.PREF_QUAGMIRE_CIPHER_KEYWORD,
                        cipherKeywordInput.getText().toString())
                .putString(Settings.PREF_QUAGMIRE_INDICATOR_KEYWORD,
                        indicatorKeywordInput.getText().toString())
                .apply();
        final QuagmireCipher cipher = new QuagmireCipher(variant,
                plainKeywordInput.getText().toString(), cipherKeywordInput.getText().toString(),
                indicatorKeywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void outputText(final String output) {
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }


    private void addVigenerePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button vigenereButton = new Button(context);
        vigenereButton.setText(R.string.vigenere_cipher);
        container.addView(vigenereButton);

        final LinearLayout vigenereSettings = new LinearLayout(context);
        vigenereSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(vigenereSettings);
        vigenereSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        vigenereSettings.addView(messageInput);

        final EditText keywordInput = new EditText(context);
        keywordInput.setHint(R.string.cipher_keyword_hint);
        keywordInput.setText(prefs.getString(Settings.PREF_VIGENERE_KEYWORD, "KEY"));
        styleCipherInput(keywordInput);
        vigenereSettings.addView(keywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        vigenereSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        vigenereSettings.addView(decryptButton);
        container.addView(vigenereSettings);

        vigenereButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                vigenereSettings.setVisibility(
                        vigenereSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputVigenereText(prefs, messageInput, keywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputVigenereText(prefs, messageInput, keywordInput, true);
            }
        });
    }

    private void outputVigenereText(final SharedPreferences prefs, final EditText messageInput,
            final EditText keywordInput, final boolean decrypt) {
        prefs.edit().putString(Settings.PREF_VIGENERE_KEYWORD,
                keywordInput.getText().toString()).apply();
        final VigenereCipher cipher = new VigenereCipher(keywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void addAffinePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button affineButton = new Button(context);
        affineButton.setText(R.string.affine_cipher);
        container.addView(affineButton);

        final LinearLayout affineSettings = new LinearLayout(context);
        affineSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(affineSettings);
        affineSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        affineSettings.addView(messageInput);

        final EditText aInput = new EditText(context);
        aInput.setHint(R.string.affine_a_hint);
        aInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        aInput.setText(String.valueOf(prefs.getInt(Settings.PREF_AFFINE_A, 5)));
        styleCipherInput(aInput);
        affineSettings.addView(aInput);

        final EditText bInput = new EditText(context);
        bInput.setHint(R.string.affine_b_hint);
        bInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        bInput.setText(String.valueOf(prefs.getInt(Settings.PREF_AFFINE_B, 8)));
        styleCipherInput(bInput);
        affineSettings.addView(bInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        affineSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        affineSettings.addView(decryptButton);
        container.addView(affineSettings);

        affineButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                affineSettings.setVisibility(
                        affineSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputAffineText(prefs, messageInput, aInput, bInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputAffineText(prefs, messageInput, aInput, bInput, true);
            }
        });
    }

    private void outputAffineText(final SharedPreferences prefs, final EditText messageInput,
            final EditText aInput, final EditText bInput, final boolean decrypt) {
        final int a = readInt(aInput, 5);
        final int b = readInt(bInput, 8);
        prefs.edit().putInt(Settings.PREF_AFFINE_A, a).putInt(Settings.PREF_AFFINE_B, b).apply();
        final AffineCipher cipher = new AffineCipher(a, b);
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private int readInt(final EditText input, final int defaultValue) {
        try {
            return Integer.parseInt(input.getText().toString());
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }


    private interface ParameterCipherFactory {
        MessageCipher create(String parameter);
    }

    private void addPasswordCipherPanel(final Context context, final LinearLayout container,
            final int titleResId, final ParameterCipherFactory cipherFactory) {
        final Button cipherButton = new Button(context);
        cipherButton.setText(titleResId);
        container.addView(cipherButton);

        final LinearLayout cipherSettings = new LinearLayout(context);
        cipherSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(cipherSettings);
        cipherSettings.setVisibility(GONE);

        final TextView notice = new TextView(context);
        notice.setText(R.string.modern_cipher_notice);
        notice.setTextColor(Color.DKGRAY);
        cipherSettings.addView(notice);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        cipherSettings.addView(messageInput);

        final EditText passwordInput = new EditText(context);
        passwordInput.setHint(R.string.cipher_password_hint);
        passwordInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        styleCipherInput(passwordInput);
        cipherSettings.addView(passwordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        cipherSettings.addView(encryptButton);
        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        cipherSettings.addView(decryptButton);
        container.addView(cipherSettings);

        cipherButton.setOnClickListener(v -> cipherSettings.setVisibility(
                cipherSettings.getVisibility() == VISIBLE ? GONE : VISIBLE));
        encryptButton.setOnClickListener(v -> outputPasswordCipherText(
                messageInput, passwordInput, cipherFactory, false,
                encryptButton, decryptButton));
        decryptButton.setOnClickListener(v -> outputPasswordCipherText(
                messageInput, passwordInput, cipherFactory, true,
                encryptButton, decryptButton));
    }

    private void outputPasswordCipherText(final EditText messageInput,
            final EditText passwordInput, final ParameterCipherFactory cipherFactory,
            final boolean decrypt, final Button encryptButton, final Button decryptButton) {
        final String password = passwordInput.getText().toString();
        final String input = messageInput.getText().toString();
        final PopupWindow originatingPopup = mCipherPopupWindow;
        final int taskGeneration = mCipherTaskGeneration;
        encryptButton.setEnabled(false);
        decryptButton.setEnabled(false);
        getCipherExecutor().execute(() -> {
            String output = null;
            String error = null;
            try {
                final MessageCipher cipher = cipherFactory.create(password);
                output = decrypt ? cipher.decrypt(input) : cipher.encrypt(input);
            } catch (RuntimeException exception) {
                error = exception.getMessage() == null
                        ? exception.getClass().getSimpleName() : exception.getMessage();
            }
            final String result = output;
            final String failure = error;
            mCipherMainHandler.post(() -> {
                encryptButton.setEnabled(true);
                decryptButton.setEnabled(true);
                if (!isAttachedToWindow() || taskGeneration != mCipherTaskGeneration
                        || originatingPopup == null || originatingPopup != mCipherPopupWindow
                        || !originatingPopup.isShowing()) {
                    return;
                }
                if (failure != null) {
                    Toast.makeText(getContext(), failure, Toast.LENGTH_LONG).show();
                } else {
                    outputText(result);
                }
            });
        });
    }

    private ExecutorService getCipherExecutor() {
        if (mCipherExecutor == null || mCipherExecutor.isShutdown()) {
            mCipherExecutor = Executors.newSingleThreadExecutor();
        }
        return mCipherExecutor;
    }

    private void addParameterizedCipherPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs, final int titleResId, final int parameterHintResId,
            final String preferenceKey, final String defaultValue, final int parameterInputType,
            final ParameterCipherFactory cipherFactory) {
        final Button cipherButton = new Button(context);
        cipherButton.setText(titleResId);
        container.addView(cipherButton);

        final LinearLayout cipherSettings = new LinearLayout(context);
        cipherSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(cipherSettings);
        cipherSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        cipherSettings.addView(messageInput);

        final EditText parameterInput = new EditText(context);
        parameterInput.setHint(parameterHintResId);
        parameterInput.setInputType(parameterInputType);
        parameterInput.setText(prefs.getString(preferenceKey, defaultValue));
        styleCipherInput(parameterInput);
        cipherSettings.addView(parameterInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        cipherSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        cipherSettings.addView(decryptButton);
        container.addView(cipherSettings);

        cipherButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                cipherSettings.setVisibility(
                        cipherSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputParameterizedCipherText(prefs, preferenceKey, messageInput, parameterInput,
                        cipherFactory, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputParameterizedCipherText(prefs, preferenceKey, messageInput, parameterInput,
                        cipherFactory, true);
            }
        });
    }

    private void outputParameterizedCipherText(final SharedPreferences prefs,
            final String preferenceKey, final EditText messageInput, final EditText parameterInput,
            final ParameterCipherFactory cipherFactory, final boolean decrypt) {
        final String parameter = parameterInput.getText().toString();
        prefs.edit().putString(preferenceKey, parameter).apply();
        final MessageCipher cipher = cipherFactory.create(parameter);
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private int readInt(final String input, final int defaultValue) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException ignored) {
            return defaultValue;
        }
    }

    private void addRailFencePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button railFenceButton = new Button(context);
        railFenceButton.setText(R.string.rail_fence_cipher);
        container.addView(railFenceButton);

        final LinearLayout railFenceSettings = new LinearLayout(context);
        railFenceSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(railFenceSettings);
        railFenceSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        railFenceSettings.addView(messageInput);

        final EditText railsInput = new EditText(context);
        railsInput.setHint(R.string.rail_fence_rails_hint);
        railsInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        railsInput.setText(String.valueOf(prefs.getInt(Settings.PREF_RAIL_FENCE_RAILS, 3)));
        styleCipherInput(railsInput);
        railFenceSettings.addView(railsInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        railFenceSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        railFenceSettings.addView(decryptButton);
        container.addView(railFenceSettings);

        railFenceButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                railFenceSettings.setVisibility(
                        railFenceSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputRailFenceText(prefs, messageInput, railsInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputRailFenceText(prefs, messageInput, railsInput, true);
            }
        });
    }

    private void outputRailFenceText(final SharedPreferences prefs, final EditText messageInput,
            final EditText railsInput, final boolean decrypt) {
        final int rails = Math.max(2, readInt(railsInput, 3));
        prefs.edit().putInt(Settings.PREF_RAIL_FENCE_RAILS, rails).apply();
        final RailFenceCipher cipher = new RailFenceCipher(rails);
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void addColumnarTranspositionPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button columnarButton = new Button(context);
        columnarButton.setText(R.string.columnar_transposition_cipher);
        container.addView(columnarButton);

        final LinearLayout columnarSettings = new LinearLayout(context);
        columnarSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(columnarSettings);
        columnarSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        columnarSettings.addView(messageInput);

        final EditText keywordInput = new EditText(context);
        keywordInput.setHint(R.string.cipher_keyword_hint);
        keywordInput.setText(prefs.getString(Settings.PREF_COLUMNAR_KEYWORD, "COLUMN"));
        styleCipherInput(keywordInput);
        columnarSettings.addView(keywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        columnarSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        columnarSettings.addView(decryptButton);
        container.addView(columnarSettings);

        columnarButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                columnarSettings.setVisibility(
                        columnarSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputColumnarTranspositionText(prefs, messageInput, keywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputColumnarTranspositionText(prefs, messageInput, keywordInput, true);
            }
        });
    }

    private void outputColumnarTranspositionText(final SharedPreferences prefs,
            final EditText messageInput, final EditText keywordInput, final boolean decrypt) {
        prefs.edit().putString(Settings.PREF_COLUMNAR_KEYWORD,
                keywordInput.getText().toString()).apply();
        final ColumnarTranspositionCipher cipher =
                new ColumnarTranspositionCipher(keywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void addPolybiusPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button polybiusButton = new Button(context);
        polybiusButton.setText(R.string.polybius_square_cipher);
        container.addView(polybiusButton);

        final LinearLayout polybiusSettings = new LinearLayout(context);
        polybiusSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(polybiusSettings);
        polybiusSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        polybiusSettings.addView(messageInput);

        final EditText keywordInput = new EditText(context);
        keywordInput.setHint(R.string.polybius_keyword_hint);
        keywordInput.setText(prefs.getString(Settings.PREF_POLYBIUS_KEYWORD, ""));
        styleCipherInput(keywordInput);
        polybiusSettings.addView(keywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        polybiusSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        polybiusSettings.addView(decryptButton);
        container.addView(polybiusSettings);

        polybiusButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                polybiusSettings.setVisibility(
                        polybiusSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputPolybiusText(prefs, messageInput, keywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputPolybiusText(prefs, messageInput, keywordInput, true);
            }
        });
    }

    private void outputPolybiusText(final SharedPreferences prefs, final EditText messageInput,
            final EditText keywordInput, final boolean decrypt) {
        prefs.edit().putString(Settings.PREF_POLYBIUS_KEYWORD,
                keywordInput.getText().toString()).apply();
        final PolybiusSquareCipher cipher =
                new PolybiusSquareCipher(keywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }


    private void addDiplomaticRedPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button redButton = new Button(context);
        redButton.setText(R.string.diplomatic_red_cipher);
        container.addView(redButton);

        final LinearLayout redSettings = new LinearLayout(context);
        redSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(redSettings);
        redSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        redSettings.addView(messageInput);

        final EditText keywordInput = new EditText(context);
        keywordInput.setHint(R.string.cipher_keyword_hint);
        keywordInput.setText(prefs.getString(Settings.PREF_DIPLOMATIC_RED_KEYWORD,
                "DIPLOMATICRED"));
        styleCipherInput(keywordInput);
        redSettings.addView(keywordInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        redSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        redSettings.addView(decryptButton);
        container.addView(redSettings);

        redButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                redSettings.setVisibility(redSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputDiplomaticRedText(prefs, messageInput, keywordInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputDiplomaticRedText(prefs, messageInput, keywordInput, true);
            }
        });
    }

    private void outputDiplomaticRedText(final SharedPreferences prefs, final EditText messageInput,
            final EditText keywordInput, final boolean decrypt) {
        prefs.edit().putString(Settings.PREF_DIPLOMATIC_RED_KEYWORD,
                keywordInput.getText().toString()).apply();
        final DiplomaticRedCipher cipher = new DiplomaticRedCipher(keywordInput.getText().toString());
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void addPurplePanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs) {
        final Button purpleButton = new Button(context);
        purpleButton.setText(R.string.purple_cipher);
        container.addView(purpleButton);

        final LinearLayout purpleSettings = new LinearLayout(context);
        purpleSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(purpleSettings);
        purpleSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        purpleSettings.addView(messageInput);

        final EditText plugboardInput = new EditText(context);
        plugboardInput.setHint(R.string.enigma_plugboard_hint);
        plugboardInput.setText(prefs.getString(Settings.PREF_PURPLE_PLUGBOARD, ""));
        styleCipherInput(plugboardInput);
        purpleSettings.addView(plugboardInput);

        final EditText positionInput = new EditText(context);
        positionInput.setHint(R.string.purple_position_hint);
        positionInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        positionInput.setText(String.valueOf(prefs.getInt(Settings.PREF_PURPLE_POSITION, 0)));
        styleCipherInput(positionInput);
        purpleSettings.addView(positionInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        purpleSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        purpleSettings.addView(decryptButton);
        container.addView(purpleSettings);

        purpleButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                purpleSettings.setVisibility(
                        purpleSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputPurpleText(prefs, messageInput, plugboardInput, positionInput, false);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputPurpleText(prefs, messageInput, plugboardInput, positionInput, true);
            }
        });
    }

    private void outputPurpleText(final SharedPreferences prefs, final EditText messageInput,
            final EditText plugboardInput, final EditText positionInput, final boolean decrypt) {
        final int position = readInt(positionInput, 0);
        prefs.edit()
                .putString(Settings.PREF_PURPLE_PLUGBOARD, plugboardInput.getText().toString())
                .putInt(Settings.PREF_PURPLE_POSITION, position)
                .apply();
        final PurpleCipher cipher = new PurpleCipher(plugboardInput.getText().toString(), position);
        final String input = messageInput.getText().toString();
        outputText(decrypt ? cipher.decrypt(input) : cipher.encrypt(input));
    }

    private void addEnigmaPanel(final Context context, final LinearLayout container,
            final SharedPreferences prefs, final boolean m4) {
        final Button enigmaButton = new Button(context);
        enigmaButton.setText(m4 ? R.string.enigma_m4 : R.string.enigma_m3);
        container.addView(enigmaButton);

        final LinearLayout enigmaSettings = new LinearLayout(context);
        enigmaSettings.setOrientation(LinearLayout.VERTICAL);
        styleCipherPanel(enigmaSettings);
        enigmaSettings.setVisibility(GONE);

        final EditText messageInput = new EditText(context);
        messageInput.setHint(R.string.cipher_message_hint);
        messageInput.setMinLines(3);
        messageInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        styleCipherInput(messageInput);
        prefillMessageInput(messageInput);
        enigmaSettings.addView(messageInput);

        final EditText thinRotorInput = new EditText(context);
        if (m4) {
            thinRotorInput.setHint(R.string.enigma_thin_rotor_hint);
            thinRotorInput.setText(prefs.getString(Settings.PREF_ENIGMA_M4_THIN_ROTOR, "Beta"));
            styleCipherInput(thinRotorInput);
            enigmaSettings.addView(thinRotorInput);
        }

        final EditText rotorsInput = new EditText(context);
        rotorsInput.setHint(R.string.enigma_rotors_hint);
        rotorsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_ROTORS
                : Settings.PREF_ENIGMA_M3_ROTORS, "I II III"));
        styleCipherInput(rotorsInput);
        enigmaSettings.addView(rotorsInput);

        final EditText reflectorInput = new EditText(context);
        reflectorInput.setHint(R.string.enigma_reflector_hint);
        reflectorInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_REFLECTOR
                : Settings.PREF_ENIGMA_M3_REFLECTOR, m4 ? "B Thin" : "B"));
        styleCipherInput(reflectorInput);
        enigmaSettings.addView(reflectorInput);

        final EditText positionsInput = new EditText(context);
        positionsInput.setHint(R.string.enigma_positions_hint);
        positionsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_POSITIONS
                : Settings.PREF_ENIGMA_M3_POSITIONS, m4 ? "AAAA" : "AAA"));
        styleCipherInput(positionsInput);
        enigmaSettings.addView(positionsInput);

        final EditText ringsInput = new EditText(context);
        ringsInput.setHint(R.string.enigma_rings_hint);
        ringsInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_RINGS
                : Settings.PREF_ENIGMA_M3_RINGS, m4 ? "AAAA" : "AAA"));
        styleCipherInput(ringsInput);
        enigmaSettings.addView(ringsInput);

        final EditText plugboardInput = new EditText(context);
        plugboardInput.setHint(R.string.enigma_plugboard_hint);
        plugboardInput.setText(prefs.getString(m4 ? Settings.PREF_ENIGMA_M4_PLUGBOARD
                : Settings.PREF_ENIGMA_M3_PLUGBOARD, ""));
        styleCipherInput(plugboardInput);
        enigmaSettings.addView(plugboardInput);

        final Button encryptButton = new Button(context);
        encryptButton.setText(R.string.encrypt);
        enigmaSettings.addView(encryptButton);

        final Button decryptButton = new Button(context);
        decryptButton.setText(R.string.decrypt);
        enigmaSettings.addView(decryptButton);
        container.addView(enigmaSettings);

        enigmaButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                enigmaSettings.setVisibility(
                        enigmaSettings.getVisibility() == VISIBLE ? GONE : VISIBLE);
            }
        });
        encryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputEnigmaText(prefs, m4, messageInput, thinRotorInput, rotorsInput,
                        reflectorInput, positionsInput, ringsInput, plugboardInput);
            }
        });
        decryptButton.setOnClickListener(new OnClickListener() {
            @Override public void onClick(View v) {
                outputEnigmaText(prefs, m4, messageInput, thinRotorInput, rotorsInput,
                        reflectorInput, positionsInput, ringsInput, plugboardInput);
            }
        });
    }

    private void outputEnigmaText(final SharedPreferences prefs, final boolean m4,
            final EditText messageInput, final EditText thinRotorInput, final EditText rotorsInput,
            final EditText reflectorInput, final EditText positionsInput, final EditText ringsInput,
            final EditText plugboardInput) {
        saveEnigmaSettings(prefs, m4, thinRotorInput, rotorsInput, reflectorInput, positionsInput,
                ringsInput, plugboardInput);
        final EnigmaCipher cipher = m4
                ? EnigmaCipher.createM4(thinRotorInput.getText().toString(),
                        rotorsInput.getText().toString(), reflectorInput.getText().toString(),
                        positionsInput.getText().toString(), ringsInput.getText().toString(),
                        plugboardInput.getText().toString())
                : EnigmaCipher.createM3(rotorsInput.getText().toString(),
                        reflectorInput.getText().toString(), positionsInput.getText().toString(),
                        ringsInput.getText().toString(), plugboardInput.getText().toString());
        final String output = cipher.encrypt(messageInput.getText().toString());
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }

    private void saveEnigmaSettings(final SharedPreferences prefs, final boolean m4,
            final EditText thinRotorInput, final EditText rotorsInput, final EditText reflectorInput,
            final EditText positionsInput, final EditText ringsInput, final EditText plugboardInput) {
        final SharedPreferences.Editor editor = prefs.edit();
        if (m4) {
            editor.putString(Settings.PREF_ENIGMA_M4_THIN_ROTOR,
                    thinRotorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_ROTORS, rotorsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_REFLECTOR,
                    reflectorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_POSITIONS,
                    positionsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_RINGS, ringsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M4_PLUGBOARD,
                    plugboardInput.getText().toString());
        } else {
            editor.putString(Settings.PREF_ENIGMA_M3_ROTORS, rotorsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_REFLECTOR,
                    reflectorInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_POSITIONS,
                    positionsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_RINGS, ringsInput.getText().toString());
            editor.putString(Settings.PREF_ENIGMA_M3_PLUGBOARD,
                    plugboardInput.getText().toString());
        }
        editor.apply();
    }

    private void outputCaesarText(final SharedPreferences prefs, final EditText messageInput,
            final EditText shiftInput, final boolean decrypt) {
        final CaesarCipher cipher = new CaesarCipher(saveCaesarShift(prefs, shiftInput));
        final String input = messageInput.getText().toString();
        final String output = decrypt ? cipher.decrypt(input) : cipher.encrypt(input);
        if (!output.isEmpty()) {
            mListener.onTextInput(output);
        }
    }

    private int saveCaesarShift(final SharedPreferences prefs, final EditText shiftInput) {
        int shift = 3;
        try {
            shift = Integer.parseInt(shiftInput.getText().toString());
        } catch (NumberFormatException ignored) {
        }
        prefs.edit().putInt(Settings.PREF_CAESAR_CIPHER_SHIFT, shift).apply();
        return shift;
    }


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        dismissCipherPopup();
        dismissMoreSuggestionsPanel();
        if (mCipherExecutor != null) {
            mCipherExecutor.shutdownNow();
            mCipherExecutor = null;
        }
    }

    public void dismissCipherPopup() {
        mCipherTaskGeneration++;
        if (mCipherPopupWindow != null) {
            mCipherPopupWindow.dismiss();
            mCipherPopupWindow = null;
        }
        mCipherFocusedInput = null;
    }

    @Override
    protected void onSizeChanged(final int w, final int h, final int oldw, final int oldh) {
        // Called by the framework when the size is known. Show the important notice if applicable.
        // This may be overriden by showing suggestions later, if applicable.
    }
}
