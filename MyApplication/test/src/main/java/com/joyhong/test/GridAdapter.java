package com.joyhong.test;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;

import java.util.ArrayList;

public class GridAdapter extends RecyclerView.Adapter<GridAdapter.GridViewHolder> {
    private Context context;
    ArrayList<TestEntity> testEntities;

    public GridAdapter(Context context, ArrayList<TestEntity> testEntities) {
        this.context = context;
        this.testEntities = testEntities;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new GridViewHolder(LayoutInflater.from(context).inflate(R.layout.activity_grid_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, final int position) {
        TestEntity testEntity = testEntities.get(position);
        holder.tv.setText(testEntities.get(position).getContent());
        holder.tv.setTag(testEntities.get(position).getTag());
        if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 2) {
            holder.main_v.setBackgroundResource(R.drawable.shape_actionsheet_top_normal);
//            holder.test_result_desp.setText("测试失败");
        } else if (SPUtils.getInstance().getInt(testEntity.getTag(), 0) == 1) {
            holder.main_v.setBackgroundResource(R.drawable.shape_actionsheet_green_normal);
//            holder.test_result_desp.setText("测试成功");
        } else {
            holder.main_v.setBackgroundResource(R.drawable.shape_actionsheet_top_test_noresult);
            holder.test_result.setVisibility(View.GONE);
        }
        holder.main_v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent intent = new Intent();
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setClassName(context, String.valueOf(testEntities.get(position).getTag()));
                    context.startActivity(intent);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
    }

    @Override
    public int getItemCount() {
        return testEntities.size();
    }

    class GridViewHolder extends RecyclerView.ViewHolder {

        private TextView tv, test_result_desp;
        View main_v, test_result;


        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            tv = itemView.findViewById(R.id.textView3);
            main_v = itemView.findViewById(R.id.main_v);
            test_result = itemView.findViewById(R.id.test_result);
            test_result_desp = itemView.findViewById(R.id.test_result_desp);
        }
    }
}