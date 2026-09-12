package com.chetan.assistant;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {
    private WebView webView;
    private SpeechRecognizer speechRecognizer;
    private boolean recognizerListening = false;

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ensureMicPermission();

        webView = new WebView(this);
        setContentView(webView);

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        // Speech bridge: WebView has no Web Speech API, so the page drives
        // mic input through window.AndroidSpeech instead.
        webView.addJavascriptInterface(new SpeechBridge(), "AndroidSpeech");

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    request.grant(request.getResources());
                }
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void ensureMicPermission() {
        // Request mic permission for voice input (native SpeechRecognizer)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    private class SpeechBridge {
        @JavascriptInterface
        public void startListening() {
            runOnUiThread(() -> startNativeRecognition());
        }

        @JavascriptInterface
        public void stopListening() {
            runOnUiThread(() -> stopNativeRecognition());
        }
    }

    private void startNativeRecognition() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Speech recognition is not available on this device.", Toast.LENGTH_SHORT).show();
            jsCallback("onNativeSpeechError", "not-available");
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ensureMicPermission();
            Toast.makeText(this, "Microphone permission is needed for voice input.", Toast.LENGTH_SHORT).show();
            jsCallback("onNativeSpeechError", "no-permission");
            return;
        }
        destroyRecognizer();
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {}
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {}
            @Override public void onError(int error) {
                recognizerListening = false;
                jsCallback("onNativeSpeechError", String.valueOf(error));
            }
            @Override public void onResults(Bundle results) {
                recognizerListening = false;
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                jsCallback("onNativeSpeechResult", (matches != null && !matches.isEmpty()) ? matches.get(0) : "");
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
        recognizerListening = true;
        try {
            speechRecognizer.startListening(intent);
        } catch (Exception e) {
            recognizerListening = false;
            jsCallback("onNativeSpeechError", "start-failed");
        }
    }

    private void stopNativeRecognition() {
        if (speechRecognizer != null && recognizerListening) {
            try { speechRecognizer.stopListening(); } catch (Exception ignored) {}
        }
        recognizerListening = false;
        jsCallback("onNativeSpeechEnd", "");
    }

    private void jsCallback(String fn, String value) {
        if (webView == null) return;
        String arg = JSONObject.quote(value == null ? "" : value);
        webView.evaluateJavascript("window." + fn + "(" + arg + ")", null);
    }

    private void destroyRecognizer() {
        if (speechRecognizer != null) {
            try { speechRecognizer.cancel(); } catch (Exception ignored) {}
            try { speechRecognizer.destroy(); } catch (Exception ignored) {}
            speechRecognizer = null;
        }
        recognizerListening = false;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        destroyRecognizer();
        if (webView != null) {
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
