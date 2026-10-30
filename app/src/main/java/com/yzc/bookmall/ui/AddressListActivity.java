package com.yzc.bookmall.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.data.BookRepository;
import com.yzc.bookmall.model.Address;
import com.yzc.bookmall.ui.adapter.AddressAdapter;
import com.yzc.bookmall.ui.widget.StateView;
import com.yzc.bookmall.util.SessionManager;

import java.util.List;

/** 收货地址管理：新增、编辑、删除、设为默认；结算页可通过 EXTRA_PICK 复用为选择器。 */
public class AddressListActivity extends BaseActivity {
    public static final String EXTRA_PICK = "pick_mode";
    public static final String EXTRA_PICKED_ID = "address_id";

    private BookRepository repository;
    private SessionManager session;
    private AddressAdapter adapter;
    private StateView stateView;
    private boolean pickMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_address_list);
        applyInsets(findViewById(android.R.id.content));

        pickMode = getIntent().getBooleanExtra(EXTRA_PICK, false);
        repository = new BookRepository(this);
        session = new SessionManager(this);

        ((TextView) findViewById(R.id.tv_toolbar_title)).setText(R.string.address_title);
        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        RecyclerView recyclerView = findViewById(R.id.rv_addresses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AddressAdapter(new AddressAdapter.Listener() {
            @Override
            public void onEdit(Address address) {
                showAddressDialog(address);
            }

            @Override
            public void onSelect(Address address) {
                if (pickMode) {
                    Intent data = new Intent();
                    data.putExtra(EXTRA_PICKED_ID, address.getId());
                    setResult(RESULT_OK, data);
                    finish();
                } else {
                    repository.setDefaultAddress(session.getUserId(), address.getId());
                    Ui.toast(AddressListActivity.this, "已设为默认地址");
                    reload();
                }
            }
        });
        recyclerView.setAdapter(adapter);

        stateView = findViewById(R.id.state_view);
        stateView.bind(recyclerView);

        findViewById(R.id.btn_add_address).setOnClickListener(v -> showAddressDialog(null));

        if (!session.isLoggedIn()) {
            stateView.showEmpty("登录后可以管理收货地址");
            findViewById(R.id.btn_add_address).setEnabled(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (session.isLoggedIn()) reload();
    }

    private void reload() {
        try {
            List<Address> addresses = repository.getAddresses(session.getUserId());
            adapter.submit(addresses);
            if (addresses.isEmpty()) stateView.showEmpty(getString(R.string.address_empty));
            else stateView.showContent();
        } catch (Exception e) {
            stateView.showError("地址加载失败：" + e.getMessage(), this::reload);
        }
    }

    private void showAddressDialog(final Address editing) {
        if (!session.isLoggedIn()) return;
        int padding = Ui.dp(this, 22);
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(padding, Ui.dp(this, 8), padding, 0);

        final EditText nameInput = field(container, R.string.address_name, InputType.TYPE_CLASS_TEXT);
        final EditText phoneInput = field(container, R.string.address_phone, InputType.TYPE_CLASS_PHONE);
        final EditText regionInput = field(container, R.string.address_region, InputType.TYPE_CLASS_TEXT);
        final EditText detailInput = field(container, R.string.address_detail, InputType.TYPE_CLASS_TEXT);
        final CheckBox defaultBox = new CheckBox(this);
        defaultBox.setText(R.string.address_default);
        defaultBox.setTextSize(13);
        defaultBox.setChecked(editing != null && editing.isDefault());
        container.addView(defaultBox);

        if (editing != null) {
            nameInput.setText(editing.getName());
            phoneInput.setText(editing.getPhone());
            regionInput.setText(editing.getRegion());
            detailInput.setText(editing.getDetail());
        }

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(editing == null ? R.string.address_add : R.string.address_edit)
                .setView(container)
                .setNegativeButton(R.string.cancel, null)
                .setNeutralButton(R.string.delete, null)
                .setPositiveButton(R.string.confirm, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            if (editing == null) {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setVisibility(android.view.View.GONE);
            } else {
                dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> {
                    repository.deleteAddress(session.getUserId(), editing.getId());
                    Ui.toast(this, "地址已删除");
                    dialog.dismiss();
                    reload();
                });
            }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                try {
                    repository.saveAddress(session.getUserId(),
                            editing == null ? 0 : editing.getId(),
                            nameInput.getText().toString(),
                            phoneInput.getText().toString(),
                            regionInput.getText().toString(),
                            detailInput.getText().toString(),
                            defaultBox.isChecked());
                    Ui.toast(this, R.string.address_saved);
                    dialog.dismiss();
                    reload();
                } catch (Exception e) {
                    Ui.toast(this, e.getMessage());
                }
            });
        });
        dialog.show();
    }

    private EditText field(LinearLayout parent, int hintRes, int inputType) {
        EditText input = new EditText(this);
        input.setHint(hintRes);
        input.setTextSize(14);
        input.setInputType(inputType);
        input.setSingleLine(true);
        parent.addView(input);
        return input;
    }
}
