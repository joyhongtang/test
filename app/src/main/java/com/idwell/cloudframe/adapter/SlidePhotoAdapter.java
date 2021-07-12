package com.idwell.cloudframe.adapter;

import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.idwell.cloudframe.entity.Photo;
import com.idwell.cloudframe.widget.banner.adapter.BannerAdapter;

import java.util.List;

/**
 * 自定义布局，图片
 */
public class SlidePhotoAdapter extends BannerAdapter<Photo, SlidePhotoHolder> {

//    public SlidePhotoAdapter() {
//        super();
//    }

    public SlidePhotoAdapter(List<Photo> mDatas) {
        //设置数据，也可以调用banner提供的方法,或者自己在adapter中实现
        super(mDatas);
    }

    //更新数据
    public void updateData(List<Photo> data){
        //这里的代码自己发挥，比如如下的写法等等
        mDatas.addAll(data);
        notifyDataSetChanged();
    }


    //创建ViewHolder，可以用viewType这个字段来区分不同的ViewHolder
    @Override
    public SlidePhotoHolder onCreateHolder(ViewGroup parent, int viewType) {
        ImageView zoomableDraweeView = new ImageView(parent.getContext());
        //注意，必须设置为match_parent，这个是viewpager2强制要求的
        zoomableDraweeView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        zoomableDraweeView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new SlidePhotoHolder(zoomableDraweeView);
    }

    @Override
    public void onBindView(SlidePhotoHolder holder, Photo data, int position, int size) {
        Uri uri = Uri.parse("file://" + data.getData());
        Glide.with(holder.itemView).load(uri).into(holder.imageView);
    }

}
