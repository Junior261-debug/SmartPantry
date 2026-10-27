package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Custom RecyclerView adapter for the pantry list. Each row shows name, quantity and a colour-coded
 * expiry line (green = fine, orange = expires within 3 days, red = already expired).
 */
public class PantryAdapter extends RecyclerView.Adapter<PantryAdapter.VH> {

    /** Callbacks so the Activity (not the adapter) decides what editing / deleting means. */
    public interface Listener {
        void onEdit(PantryItem item);
        void onDelete(PantryItem item);
    }

    private List<PantryItem> items;
    private boolean highlightExpiry;
    private final Listener listener;

    public PantryAdapter(List<PantryItem> items, boolean highlightExpiry, Listener listener) {
        this.items = items;
        this.highlightExpiry = highlightExpiry;
        this.listener = listener;
    }

    /** Replaces the data and redraws the list. */
    public void submit(List<PantryItem> newItems, boolean highlightExpiry) {
        this.items = newItems;
        this.highlightExpiry = highlightExpiry;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, qty, expiry;
        final ImageButton delete;

        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            qty = v.findViewById(R.id.tvQty);
            expiry = v.findViewById(R.id.tvExpiry);
            delete = v.findViewById(R.id.btnDelete);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pantry, parent, false);
        final VH holder = new VH(v);
        v.setOnClickListener(x -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onEdit(items.get(pos));
        });
        holder.delete.setOnClickListener(x -> {
            int pos = holder.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onDelete(items.get(pos));
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        PantryItem p = items.get(position);
        Context c = h.itemView.getContext();
        h.name.setText(p.name);
        h.qty.setText(Units.describe(p.quantity, p.unit));

        Long days = DateUtil.daysUntil(p.expiry, DateUtil.today());
        if (days == null) {
            h.expiry.setText("No expiry date");
            h.expiry.setTextColor(ContextCompat.getColor(c, R.color.text_secondary));
        } else if (!highlightExpiry) {
            h.expiry.setText("Expires " + p.expiry);                 // alerts off: plain date, no colours
            h.expiry.setTextColor(ContextCompat.getColor(c, R.color.text_secondary));
        } else {
            h.expiry.setText(DateUtil.relative(days) + "  (" + p.expiry + ")");
            int colour = days < 0 ? R.color.expiry_expired
                    : days <= RecipeMatcher.SOON_DAYS ? R.color.expiry_soon : R.color.expiry_ok;
            h.expiry.setTextColor(ContextCompat.getColor(c, colour));
        }
        h.delete.setContentDescription("Delete " + p.name);
    }

    @Override
    public int getItemCount() { return items.size(); }
}
