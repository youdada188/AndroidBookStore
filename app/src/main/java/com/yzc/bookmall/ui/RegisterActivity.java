package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;

/** 注册页：用户名唯一、密码长度与两次一致性校验。 */
public class RegisterActivity extends BaseActivity {
    private EditText usernameInput;
    private EditText passwordInput;
    private EditText confirmInput;
    private BookRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        applyInsets(findViewById(android.R.id.content));
        repository = new BookRepository(this);
        usernameInput = findViewById(R.id.et_username);
        passwordInput = findViewById(R.id.et_password);
        confirmInput = findViewById(R.id.et_confirm_password);
        ((Button) findViewById(R.id.btn_register)).setOnClickListener(v -> register());
        ((TextView) findViewById(R.id.tv_login)).setOnClickListener(v -> finish());
    }

    private void register() {
        String username = usernameInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        String confirm = confirmInput.getText().toString();
        if (username.isEmpty()) {
            usernameInput.setError(getString(R.string.error_username_required));
            return;
        }
        if (password.length() < 6) {
            passwordInput.setError(getString(R.string.error_password_length));
            return;
        }
        if (!password.equals(confirm)) {
            confirmInput.setError(getString(R.string.error_password_mismatch));
            return;
        }
        long userId = repository.registerUser(username, password);
        if (userId < 0) {
            usernameInput.setError(getString(R.string.error_username_exists));
            return;
        }
        Toast.makeText(this, R.string.register_success, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.putExtra("prefill_username", username);
        startActivity(intent);
        finish();
    }
}
