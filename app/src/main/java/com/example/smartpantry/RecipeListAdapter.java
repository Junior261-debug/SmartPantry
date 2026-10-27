package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Adapter for the "All Recipes" tab: every recipe, with a one-line readiness status. */
public class RecipeListAdapter extends RecyclerView.Adapter<RecipeListAdapter.VH> {

    public interface Listener { void onRecipeClick(Recipe recipe); }

    private List<MatchResult> items = new ArrayList<>();
    private final Listener listener;

    public RecipeListAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<MatchResult> newItems) {
        items = newItems;
        notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView name, meta, note;
        VH(View v) {
            super(v);
            name = v.findViewById(R.id.tvName);
            meta = v.findViewById(R.id.tvMeta);
            note = v.findViewById(R.id.tvNote);
        }
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final VH h = new VH(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recipe, parent, false));
        h.itemView.setOnClickListener(v -> {
            int pos = h.getBindingAdapterPosition();
            if (pos != RecyclerView.NO_POSITION) listener.onRecipeClick(items.get(pos).recipe);
        });
        return h;
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        MatchResult m = items.get(position);
        Recipe r = m.recipe;
        Context c = h.itemView.getContext();
        h.name.setText(r.custom ? r.name + "  • My recipe" : r.name);
        h.meta.setText(r.minutes + " min  •  " + r.ingredients.size() + " ingredients");
        h.note.setVisibility(View.VISIBLE);
        if (m.isReady()) {
            h.note.setText("You can cook this now");
            h.note.setTextColor(ContextCompat.getColor(c, R.color.expiry_ok));
        } else {
            int n = m.gaps.size();
            h.note.setText(n == 1 ? "Missing: " + m.gaps.get(0).describe() : "Missing " + n + " ingredients");
            h.note.setTextColor(ContextCompat.getColor(c, R.color.text_secondary));
        }
    }

    @Override
    public int getItemCount() { return items.size(); }
}
