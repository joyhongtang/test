package com.joyhong.test;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.blankj.utilcode.util.SPUtils;

import java.util.ArrayList;
import java.util.List;

public class HomeTvAdapter extends CustomRecyclerView.CustomAdapter<TestEntity> {

    private Context context;
    ArrayList<TestEntity> testEntities;
    public static int lastFocusPos = -1;

    public HomeTvAdapter(Context context, ArrayList<TestEntity> testEntities) {
        super(context, testEntities);
        this.context = context;
        this.testEntities = testEntities;
    }
    @Override
    protected RecyclerView.ViewHolder onSetViewHolder(View view) {
        return new GridViewHolder(view);
    }

    @NonNull
    @Override
    protected int onSetItemLayout() {
        return R.layout.activity_grid_item;
    }

    @Override
    protected void onSetItemData(RecyclerView.ViewHolder mholder, int position) {
        GridViewHolder holder = (GridViewHolder) mholder;
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
    protected void onItemFocus(View itemView, int position) {
//        TextView tvFocus = (TextView) itemView.findViewById(R.id.tv_focus);
        ImageView focusBg = (ImageView) itemView.findViewById(R.id.focus_bg);

//        tvFocus.setVisibility(View.VISIBLE);
        focusBg.setVisibility(View.VISIBLE);

        if (Build.VERSION.SDK_INT >= 21) {
            //抬高Z轴
            ViewCompat.animate(itemView).scaleX(1.20f).scaleY(1.20f).translationZ(1).start();
        } else {
            ViewCompat.animate(itemView).scaleX(1.20f).scaleY(1.20f).start();
            ViewGroup parent = (ViewGroup) itemView.getParent();
            parent.requestLayout();
            parent.invalidate();
        }
        lastFocusPos = position;
    }

    @Override
    protected void onItemGetNormal(View itemView, int position) {

//        TextView tvFocus = (TextView) itemView.findViewById(R.id.tv_focus);
        ImageView focusBg = (ImageView) itemView.findViewById(R.id.focus_bg);

//        tvFocus.setVisibility(View.VISIBLE);
        focusBg.setVisibility(View.INVISIBLE);

        if (Build.VERSION.SDK_INT >= 21) {
            ViewCompat.animate(itemView).scaleX(1.0f).scaleY(1.0f).translationZ(0).start();
        } else {
            ViewCompat.animate(itemView).scaleX(1.0f).scaleY(1.0f).start();
            ViewGroup parent = (ViewGroup) itemView.getParent();
            parent.requestLayout();
            parent.invalidate();
        }
    }

    @Override
    protected int getCount() {
        return mData.size();
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
