package com.airplanescheduler;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    private List<Schedule> schedules;
    private OnScheduleListener listener;

    public interface OnScheduleListener {
        void onToggleEnabled(int position, boolean enabled);
        void onDelete(int position);
    }

    public ScheduleAdapter(List<Schedule> schedules, OnScheduleListener listener) {
        this.schedules = schedules != null ? schedules : new ArrayList<>();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_schedule, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Schedule schedule = schedules.get(position);
        holder.timeText.setText(schedule.getTimeString());
        holder.actionText.setText(schedule.getActionString());
        
        int color = schedule.isTurnOn() ? 
                holder.itemView.getContext().getColor(android.R.color.holo_green_light) :
                holder.itemView.getContext().getColor(android.R.color.holo_red_light);
        holder.actionText.setTextColor(color);
        
        holder.enableSwitch.setOnCheckedChangeListener(null);
        holder.enableSwitch.setChecked(schedule.isEnabled());
        holder.enableSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (listener != null) {
                listener.onToggleEnabled(holder.getAdapterPosition(), isChecked);
            }
        });

        holder.deleteButton.setOnClickListener(v -> {
            if (listener != null) {
                listener.onDelete(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return schedules.size();
    }

    public void updateList(List<Schedule> newList) {
        this.schedules = newList != null ? newList : new ArrayList<>();
        notifyDataSetChanged();
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        TextView actionText;
        SwitchCompat enableSwitch;
        ImageButton deleteButton;

        ViewHolder(View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.time_text);
            actionText = itemView.findViewById(R.id.action_text);
            enableSwitch = itemView.findViewById(R.id.enable_switch);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }
    }
}
