package com.example.smartpantry;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for the Suggested Recipes screen. One list, three kinds of row:
 * section headers, a plain message row, and recipe cards. "Ready to cook" and "Almost there"
 * live in the same list but under separate headers, so they are never mixed up.
 */
public class SuggestionAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    /** One row in the list. Use the factory methods. */
    public static class Row {
        static final int HEADER = 0, MESSAGE = 1, RECIPE = 2;
        final int type;
        final String title, sub;
        final MatchResult match;

        private Row(int type, String title, String sub, MatchResult match) {
            this.type = type; this.title = title; this.sub = sub; this.match = match;
        }
        public static Row header(String title, String sub) { return new Row(HEADER, title, sub, null); }
        public static Row message(String text) { return new Row(MESSAGE, text, null, null); }
        public static Row recipe(MatchResult m) { return new Row(RECIPE, null, null, m); }
    }

    public interface Listener { void onRecipeClick(Recipe recipe); }

    private List<Row> rows = new ArrayList<>();
    private final Listener listener;

    public SuggestionAdapter(Listener listener) { this.listener = listener; }

    public void submit(List<Row> newRows) {
        rows = newRows;
        notifyDataSetChanged();
    }

    @Override public int getItemViewType(int position) { return rows.get(position).type; }
    @Override public int getItemCount() { return rows.size(); }

    static class HeaderVH extends RecyclerView.ViewHolder {
        final TextView title, sub;
        HeaderVH(View v) {
            super(v);
            title = v.findViewById(R.id.tvHeaderTitle);
            sub = v.findViewById(R.id.tvHeaderSub);
        }
    }

    static class RecipeVH extends RecyclerView.ViewHolder {
        final MaterialCardView card;
        final TextView name, meta, note;
        RecipeVH(View v) {
            super(v);
            card = (MaterialCardView) v;
            name = v.findViewById(R.id.tvName);
            meta = v.findViewById(R.id.tvMeta);
            note = v.findViewById(R.id.tvNote);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inf = LayoutInflater.from(parent.getContext());
        if (viewType == Row.RECIPE) {
            final RecipeVH h = new RecipeVH(inf.inflate(R.layout.item_recipe, parent, false));
            h.itemView.setOnClickListener(v -> {
                int pos = h.getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION) listener.onRecipeClick(rows.get(pos).match.recipe);
            });
            return h;
        }
        return new HeaderVH(inf.inflate(R.layout.item_header, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Row row = rows.get(position);
        Context c = holder.itemView.getContext();

        if (row.type == Row.RECIPE) {
            RecipeVH h = (RecipeVH) holder;
            MatchResult m = row.match;
            Recipe r = m.recipe;
            h.name.setText(r.custom ? r.name + "  • My recipe" : r.name);
            h.meta.setText(r.minutes + " min  •  " + r.ingredients.size() + " ingredients");

            if (m.isReady()) {
                h.card.setCardBackgroundColor(ContextCompat.getColor(c, R.color.white));
                if (m.usesExpiring()) {
                    h.note.setVisibility(View.VISIBLE);
                    h.note.setTextColor(ContextCompat.getColor(c, R.color.expiry_soon));
                    h.note.setText("Uses " + m.expiringUsed + (m.expiringUsed == 1
                            ? " ingredient that expires soon" : " ingredients that expire soon"));
                } else {
                    h.note.setVisibility(View.GONE);
                }
            } else {                                              // "Almost there": exactly one gap
                h.card.setCardBackgroundColor(ContextCompat.getColor(c, R.color.almost_bg));
                h.note.setVisibility(View.VISIBLE);
                h.note.setTextColor(ContextCompat.getColor(c, R.color.expiry_soon));
                h.note.setText("Missing: " + m.gaps.get(0).describe());
            }
        } else {
            HeaderVH h = (HeaderVH) holder;
            h.title.setText(row.title);
            boolean isMessage = row.type == Row.MESSAGE;
            h.title.setTextColor(ContextCompat.getColor(c, isMessage ? R.color.text_secondary : R.color.green_primary));
            if (row.sub == null || row.sub.isEmpty()) {
                h.sub.setVisibility(View.GONE);
            } else {
                h.sub.setVisibility(View.VISIBLE);
                h.sub.setText(row.sub);
            }
        }
    }
}
