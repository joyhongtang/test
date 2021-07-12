package com.joyhong.test.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;

import com.blankj.utilcode.util.ToastUtils;
import com.joyhong.test.R;
import com.joyhong.test.util.DisplayUtils;

import static com.joyhong.test.util.ZxingUtils.Create2DCode;
import static com.joyhong.test.util.ZxingUtils.addLogo;
import static com.joyhong.test.util.ZxingUtils.changeBitmapSize;

public class MyCreateQRViewDialog extends Dialog implements View.OnClickListener {
    //在构造方法里提前加载了样式
    private Context context;//上下文
    private int layoutResID;//布局文件id
    private EditText editText;
    private ImageView qr_image;

    private int dialogWidht = 0;
    public MyCreateQRViewDialog(Context context, int layoutResID) {
        super(context, R.style.MyDialog);//加载dialog的样式
        this.context = context;
        this.layoutResID = layoutResID;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //提前设置Dialog的一些样式
        Window dialogWindow = getWindow();
        dialogWindow.setGravity(Gravity.CENTER);//设置dialog显示居中
        //dialogWindow.setWindowAnimations();设置动画效果
        setContentView(layoutResID);


        WindowManager windowManager = ((Activity) context).getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.width = display.getWidth() * 2 / 5;// 设置dialog宽度为屏幕的4/5
        dialogWidht = lp.width;
        lp.height = dialogWidht;
        getWindow().setAttributes(lp);
        setCanceledOnTouchOutside(true);//点击外部Dialog消失
        //遍历控件id添加点击注册
        qr_image = findViewById(R.id.qr_image);
    }

    private OnCenterItemClickListener listener;

    public interface OnCenterItemClickListener {
        void OnCenterItemClick(MyCreateQRViewDialog dialog, int view);
    }

    //很明显我们要在这里面写个接口，然后添加一个方法
    public void setOnCenterItemClickListener(OnCenterItemClickListener listener) {
        this.listener = listener;
    }


    @Override
    public void onClick(View v) {
//        if(v.getId() != R.id.scan_qr){
//            dismiss();//注意：我在这里加了这句话，表示只要按任何一个控件的id,弹窗都会消失，不管是确定还是取消。
//        }
        listener.OnCenterItemClick(this, v.getId());
    }
    public void setEditTextContent(String content){
        HPEditText activity_create_qrcode_dialog = findViewById(R.id.connect_code);
        activity_create_qrcode_dialog.setText(content);
    }
    public void createQRCode(String content){
        if (TextUtils.isEmpty(content)) {
            ToastUtils.showLong(getContext().getResources().getString(R.string.scan_qr_code_errror));
            return;
        }
//        setEditTextContent(content);
        //将drawable里面的图片bitmap化
        Bitmap logo = BitmapFactory.decodeResource(getContext().getResources(), R.drawable.ic_launcher);
        Bitmap logoScale = changeBitmapSize(logo, DisplayUtils.INSTANCE.dp2px(getContext(),50),DisplayUtils.INSTANCE.dp2px(getContext(),50));
        if(null != logo && !logo.isRecycled()){
            logo.recycle();
        }
        //生成二维码显示在imageView上
        qr_image.setImageBitmap(addLogo(Create2DCode(content, dialogWidht , dialogWidht ),logoScale));
    }

}