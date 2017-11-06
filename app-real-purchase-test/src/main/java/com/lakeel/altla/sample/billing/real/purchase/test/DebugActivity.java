package com.lakeel.altla.sample.billing.real.purchase.test;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import butterknife.BindView;
import butterknife.ButterKnife;

public final class DebugActivity extends AppCompatActivity {

    @BindView(R.id.textViewJson)
    TextView textViewJson;

    private static final String EXTRA_NAME_JSON = "json";

    public static Intent newIntent(@NonNull Context context, @NonNull String json) {
        Intent intent = new Intent(context, DebugActivity.class);
        intent.putExtra(EXTRA_NAME_JSON, json);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        ButterKnife.bind(this);

        setTitle(getString(R.string.title_response));

        Intent intent = getIntent();
        JSONObject json;
        try {
            json = new JSONObject(intent.getStringExtra(EXTRA_NAME_JSON));
            textViewJson.setText(json.toString(4));
        } catch (JSONException e) {
            throw new IllegalStateException("Invalid json.");
        }
    }
}
