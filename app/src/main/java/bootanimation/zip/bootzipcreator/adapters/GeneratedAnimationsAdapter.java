package bootanimation.zip.bootzipcreator.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.imageview.ShapeableImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import bootanimation.zip.bootzipcreator.R;
import bootanimation.zip.bootzipcreator.models.BootAnimation;
import bootanimation.zip.bootzipcreator.others.CustomMethods;

public class GeneratedAnimationsAdapter extends RecyclerView.Adapter<GeneratedAnimationsAdapter.MyViewHolder> implements Filterable {

    private final Context context;
    private final List<BootAnimation> bootAnimationsFull;
    private final List<BootAnimation> bootAnimations;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(int position);
        void onItemLongClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }


    public GeneratedAnimationsAdapter(Context context, List<BootAnimation> bootAnimations) {
        this.context = context;
        this.bootAnimations = new ArrayList<>(bootAnimations);
        this.bootAnimationsFull = new ArrayList<>(bootAnimations);
    }


    @NonNull
    @Override
    public GeneratedAnimationsAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_generated_animations_history, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GeneratedAnimationsAdapter.MyViewHolder holder, int position) {

        BootAnimation bootAnimation = bootAnimations.get(position);
        holder.fileNameTV.setText(bootAnimation.getZipFileName());
        holder.timestampTV.setText(CustomMethods.formatTimestamp(bootAnimation.getCreationTime(), false));

        String videoPath = bootAnimation.getVideoPath();
        if (videoPath != null && !videoPath.isEmpty()) {
            Glide.with(context)
                    .load(Uri.fromFile(new File(videoPath)))
                    .centerCrop()
                    .placeholder(R.color.dark) // Optional: a placeholder color
                    .error(R.drawable.baseline_error_24) // Optional: an error image
                    .into(holder.thumbnail);
        }
    }

    @Override
    public int getItemCount() {
        return bootAnimations.size();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void updateList(List<BootAnimation> newAnimations) {
        bootAnimations.clear();
        bootAnimations.addAll(newAnimations);
        bootAnimationsFull.clear();
        bootAnimationsFull.addAll(newAnimations);
        notifyDataSetChanged();
    }

    @Override
    public Filter getFilter() {
        return animationFilter;
    }

    private final Filter animationFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<BootAnimation> filteredList = new ArrayList<>();

            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(bootAnimationsFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();

                for (BootAnimation item : bootAnimationsFull) {
                    String formattedDate = CustomMethods.formatTimestamp(item.getCreationTime(), false).toLowerCase();
                    String fileName = item.getZipFileName().toLowerCase();

                    if (fileName.contains(filterPattern) || formattedDate.contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }

            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @SuppressLint("NotifyDataSetChanged")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            bootAnimations.clear();
            bootAnimations.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };

    public class MyViewHolder extends RecyclerView.ViewHolder {

        private final ShapeableImageView thumbnail;
        private final TextView fileNameTV;
        private final TextView timestampTV;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.item_thumbnail);
            fileNameTV = itemView.findViewById(R.id.item_title_tv);
            timestampTV = itemView.findViewById(R.id.item_timestamp_tv);

            // Set a listener for the entire item view
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAbsoluteAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) { // Ensure position is valid
                        listener.onItemClick(position);
                    }
                }
            });

            // For long click
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    int position = getAbsoluteAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onItemLongClick(position);
                    }
                }
                return true;
            });
        }
    }
}

