package com.connection_telecom.touchpointexample;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.connection_telecom.touchpoint.ChannelAvailabilityCallback;
import com.connection_telecom.touchpoint.TouchpointClient;

import java.util.List;

public class MenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        findViewById(R.id.nativeButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TouchpointClient.getChannelAvailability(getString(R.string.customer), getString(R.string.team), new ChannelAvailabilityCallback() {
                    @Override
                    public void onSuccess(List<String> availableChannels, List<String> unavailableChannels) {
                        if (availableChannels.contains("text")) {
                            Intent intent = new Intent(MenuActivity.this, ChatActivity.class);
                            startActivity(intent);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.noAgentsMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Throwable error) {
                        Toast.makeText(getApplicationContext(), R.string.errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });

        findViewById(R.id.webViewButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    // Android pre-Lollipop does not support WebRTC in WebViews. So
                    // if the channel has voice available, use the browser instead
                    // of the WebView.
                    TouchpointClient.getChannelAvailability(getString(R.string.customer), getString(R.string.team), new ChannelAvailabilityCallback() {
                        @Override
                        public void onSuccess(List<String> availableChannels, List<String> unavailableChannels) {
                            if (availableChannels.contains("voice")) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.widgetUri)));
                                startActivity(intent);
                            } else {
                                Intent intent = new Intent(MenuActivity.this, WebViewActivity.class);
                                startActivity(intent);
                            }
                        }

                        @Override
                        public void onFailure(Throwable error) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.widgetUri)));
                            startActivity(intent);
                        }
                    });
                } else {
                    Intent intent = new Intent(MenuActivity.this, WebViewActivity.class);
                    startActivity(intent);
                }
            }
        });
    }
}
