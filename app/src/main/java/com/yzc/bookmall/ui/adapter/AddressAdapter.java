package com.yzc.bookmall.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yzc.bookmall.R;
import com.yzc.bookmall.model.Address;

import java.util.ArrayList;
import java.util.List;

/** 收货地址列表。 */
public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressHolder> {

    public interface Listener {
        void onEdit(Address address);
        void onSelect(Address address);
    }

    private final List<Address> items = new ArrayList<>();
    private final Listener listener;

    public AddressAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<Address> addresses) {
        items.clear();
        if (addresses != null) items.addAll(addresses);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AddressHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_address, parent, false);
        return new AddressHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressHolder holder, int position) {
        holder.bind(items.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class AddressHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView phone;
        private final TextView defaultTag;
        private final TextView detail;
        private final TextView edit;

        AddressHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.tv_address_name);
            phone = itemView.findViewById(R.id.tv_address_phone);
            defaultTag = itemView.findViewById(R.id.tv_address_default);
            detail = itemView.findViewById(R.id.tv_address_detail);
            edit = itemView.findViewById(R.id.tv_address_edit);
        }

        void bind(Address address, Listener listener) {
            name.setText(address.getName());
            phone.setText(address.getPhone());
            defaultTag.setVisibility(address.isDefault() ? View.VISIBLE : View.GONE);
            detail.setText(address.getFullAddress());
            edit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(address);
            });
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onSelect(address);
            });
        }
    }
}
