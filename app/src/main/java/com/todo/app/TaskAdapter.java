package com.todo.app;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.ViewHolder> {

    private Context context;
    private List<Task> tasks;
    private OnTaskClickListener listener;
    private int activeTimerTaskId = -1;

    public interface OnTaskClickListener {
        void onTaskClick(Task task);
        void onCheckClick(Task task);
        void onStarClick(Task task);
        void onPlayClick(Task task);
    }

    public TaskAdapter(Context context, List<Task> tasks, OnTaskClickListener listener) {
        this.context = context;
        this.tasks = tasks;
        this.listener = listener;
    }

    public void setActiveTimerTaskId(int id) {
        this.activeTimerTaskId = id;
        notifyDataSetChanged();
    }

    public void updateTasks(List<Task> newTasks) {
        this.tasks = newTasks;
        notifyDataSetChanged();
    }

    public void updateTask(Task updatedTask) {
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getId() == updatedTask.getId()) {
                tasks.set(i, updatedTask);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Task task = tasks.get(position);

        // Title
        holder.tvTitle.setText(task.getTitle());
        if (task.isCompleted()) {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.parseColor("#A19F9D"));
        } else {
            holder.tvTitle.setPaintFlags(
                    holder.tvTitle.getPaintFlags() & ~Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(Color.parseColor("#201F1E"));
        }

        // Check circle
        if (task.isCompleted()) {
            holder.ivCheck.setBackgroundResource(R.drawable.bg_circle_checked);
        } else {
            holder.ivCheck.setBackgroundResource(R.drawable.bg_circle_outline);
        }

        // Priority
        if (!task.getPriority().equals("normal")) {
            holder.tvPriority.setVisibility(View.VISIBLE);
            holder.tvPriority.setText("🚩 " + task.getPriorityLabel());
            holder.tvPriority.setTextColor(Color.parseColor(task.getPriorityColor()));
        } else {
            holder.tvPriority.setVisibility(View.GONE);
        }

        // Duration
        holder.tvDuration.setText("⏱ " + task.getDuration() + "m");

        // Star
        if (task.isStarred()) {
            holder.tvStar.setText("⭐");
            holder.tvStar.setTextColor(Color.parseColor("#F9A825"));
        } else {
            holder.tvStar.setText("☆");
            holder.tvStar.setTextColor(Color.parseColor("#A19F9D"));
        }

        // Timer running
        boolean isRunning = activeTimerTaskId == task.getId();
        if (isRunning) {
            holder.tvTimer.setVisibility(View.VISIBLE);
            holder.tvTimer.setText(task.getFormattedTimeLeft() + " left");
            holder.tvPlay.setText("⏸");
            holder.pbProgress.setVisibility(View.VISIBLE);
            holder.pbProgress.setProgress(Math.round(task.getProgress() * 100));
        } else if (task.getElapsed() > 0 && !task.isCompleted()) {
            holder.tvTimer.setVisibility(View.GONE);
            holder.tvPlay.setText("▶");
            holder.pbProgress.setVisibility(View.VISIBLE);
            holder.pbProgress.setProgress(Math.round(task.getProgress() * 100));
        } else {
            holder.tvTimer.setVisibility(View.GONE);
            holder.tvPlay.setText("▶");
            holder.pbProgress.setVisibility(View.GONE);
        }

        if (task.isCompleted()) {
            holder.tvPlay.setVisibility(View.GONE);
            holder.pbProgress.setVisibility(View.GONE);
        } else {
            holder.tvPlay.setVisibility(View.VISIBLE);
        }

        // Click listeners
        holder.itemView.setOnClickListener(v -> listener.onTaskClick(task));
        holder.ivCheck.setOnClickListener(v -> listener.onCheckClick(task));
        holder.tvStar.setOnClickListener(v -> listener.onStarClick(task));
        holder.tvPlay.setOnClickListener(v -> listener.onPlayClick(task));
    }

    @Override
    public int getItemCount() {
        return tasks.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCheck;
        TextView tvTitle, tvPriority, tvDuration, tvTimer, tvStar, tvPlay;
        ProgressBar pbProgress;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCheck = itemView.findViewById(R.id.iv_check);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvDuration = itemView.findViewById(R.id.tv_duration);
            tvTimer = itemView.findViewById(R.id.tv_timer);
            tvStar = itemView.findViewById(R.id.tv_star);
            tvPlay = itemView.findViewById(R.id.tv_play);
            pbProgress = itemView.findViewById(R.id.pb_progress);
        }
    }
}