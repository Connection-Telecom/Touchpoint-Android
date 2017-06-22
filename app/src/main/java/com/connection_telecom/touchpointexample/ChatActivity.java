package com.connection_telecom.touchpointexample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;

import com.connection_telecom.touchpoint.TouchpointClient;
import com.connection_telecom.touchpoint.TouchpointEventListener;

public class ChatActivity extends AppCompatActivity {

    private TouchpointClient touchpoint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        EditText messageText = (EditText)findViewById(R.id.messageText);
        messageText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEND) {
                    sendMessage();
                    return true;
                }
                return false;
            }
        });

        messageText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                touchpoint.setUserTyping(s.length() > 0);
            }
        });

        ImageButton sendButton = (ImageButton)findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        touchpoint = new TouchpointClient(getApplicationContext(), getString(R.string.customer), "Android test", getString(R.string.team));
        touchpoint.addEventListener(new EventListener()).connect();
    }

    private class EventListener implements TouchpointEventListener {
        @Override
        public void onChatCreated(String chatId) {
            ((EditText)findViewById(R.id.messageText)).setEnabled(true);
            ((ImageButton)findViewById(R.id.sendButton)).setEnabled(true);
        }

        @Override
        public void onAgentTyping(boolean isTyping) {
            View agentIsTyping = findViewById(R.id.agentIsTyping);
            if (isTyping) {
                agentIsTyping.setVisibility(View.VISIBLE);
                scrollToBottom();
            } else {
                agentIsTyping.setVisibility(View.GONE);
            }
        }

        @Override
        public void onMessage(String type, String message) {
            int layoutId = (type.equals("agentMsg") ? R.layout.agent_message : R.layout.system_message);
            addMessageView(layoutId, message);
        }

        @Override
        public void onChatClosed() {
            close();
        }

        @Override
        public void onError(String error) {
            addMessageView(R.layout.system_message, "Sorry, an error occurred");
            close();
        }

        @Override
        public void onDisconnect() {
            close();
        }
    }

    private void close() {
        ((EditText)findViewById(R.id.messageText)).setEnabled(false);
        ((ImageButton)findViewById(R.id.sendButton)).setEnabled(false);
    }

    private void addMessageView(int layoutId, String message) {
        View view = getLayoutInflater().inflate(layoutId, null);
        ((TextView)view.findViewById(R.id.text)).setText(message);
        ViewGroup insertPoint = (ViewGroup)findViewById(R.id.messages);
        insertPoint.addView(view, insertPoint.getChildCount(), new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        scrollToBottom();
    }

    private void scrollToBottom() {
        final ScrollView scroll = (ScrollView)findViewById(R.id.scroll);
        scroll.post(new Runnable() {
            @Override
            public void run() {
                scroll.fullScroll(View.FOCUS_DOWN);
            }
        });
    }

    private void sendMessage() {
        EditText messageText = (EditText)findViewById(R.id.messageText);
        String message = messageText.getText().toString().trim();
        if (!message.equals("")) {
            addMessageView(R.layout.user_message, message);
            touchpoint.sendMessage(message);
            messageText.setText("");
        }
    }

    @Override
    protected void onDestroy() {
        touchpoint.destroy();
        super.onDestroy();
    }
}
