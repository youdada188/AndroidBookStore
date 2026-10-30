package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.util.SessionManager;

/** 登录页：输入校验、密码错误提示、登录状态写入 SharedPreferences。 */
public class LoginActivity extends BaseActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private BookRepository repository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        repository = new BookRepository(this);
        sessionManager = new SessionManager(this);
        if (sessionManager.isLoggedIn()) {
            openMain();
            return;
        }
        setContentView(R.layout.activity_login);
        applyInsets(findViewById(android.R.id.content));

        usernameInput = findViewById(R.id.et_username);
        passwordInput = findViewById(R.id.et_password);
        String prefill = getIntent().getStringExtra("prefill_username");
        if (prefill != null) usernameInput.setText(prefill);

        ((Button) findViewById(R.id.btn_login)).setOnClickListener(v -> login());
        ((TextView) findViewById(R.id.tv_register)).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
        ((TextView) findViewById(R.id.tv_guest)).setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    private void login() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (username.isEmpty()) {
            usernameInput.setError(getString(R.string.error_username_required));
            return;
        }
        if (password.isEmpty()) {
            passwordInput.setError(getString(R.string.error_password_required));
            return;
        }
        long userId = repository.authenticate(username, password);
        if (userId < 0) {
            passwordInput.setError(getString(R.string.error_login_failed));
            return;
        }
        sessionManager.saveSession(userId, repository.getUsername(userId));
        openMain();
    }

    private void openMain() {
        Intent intent = new Intent(this, MainActivity.class);
        String tab = getIntent().getStringExtra("redirect_tab");
        if (tab != null) intent.putExtra("redirect_tab", tab);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
