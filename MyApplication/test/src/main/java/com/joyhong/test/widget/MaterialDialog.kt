package com.joyhong.test.widget

import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.chad.library.adapter.base.BaseQuickAdapter
import com.joyhong.test.R

class MaterialDialog(context: Context, themeResId: Int) : Dialog(context, themeResId) {

    class Builder(val context: Context) {
        private var mThemeResId = R.style.MaterialDialog
        private var mBgResId: Int? = null
        private var mTitle: CharSequence? = null
        private var mTitleColor: Int? = null
        private var mTitleDividerColor: Int? = null
        private var mContent: CharSequence? = null
        private var mContentColor: Int? = null
        private var mEditContent: CharSequence? = null
        private var mAdapter: BaseQuickAdapter<*, *>? = null
        private var mItemDecoration: RecyclerView.ItemDecoration? = null
        private var mLayoutParamsHeight = 0
        private var mPositiveButtonText: CharSequence? = null
        private var mNegativeButtonText: CharSequence? = null
        private var mNeutralButtonText: CharSequence? = null
        private var mPositiveButtonListener: OnClickListener? = null
        private var mNegativeButtonListener: OnClickListener? = null
        private var mNeutralButtonListener: OnClickListener? = null
        private var mCancelable = true
        private var mOnCancelListener: OnCancelListener? = null

        constructor(context: Context, themeResId: Int) : this(context) {
            mThemeResId = themeResId
        }

        fun setDarkBg(): Builder {
            mBgResId = R.drawable.bg_dark_dialog
            mTitleColor = ContextCompat.getColor(context, R.color.text_dark_dialog_title)
            mTitleDividerColor = ContextCompat.getColor(context, R.color.divider_dark_dialog_title)
            mContentColor = ContextCompat.getColor(context, R.color.text_dark_dialog_content)
            return this
        }

        fun setTitle(titleId: Int): Builder {
            mTitle = context.getText(titleId)
            return this
        }

        fun setTitle(title: CharSequence): Builder {
            mTitle = title
            return this
        }

        fun setContent(messageId: Int): Builder {
            mContent = context.getText(messageId)
            return this
        }

        fun setContent(message: CharSequence): Builder {
            mContent = message
            return this
        }

        fun setEditContent(editMessageId: Int): Builder {
            mEditContent = context.getText(editMessageId)
            return this
        }

        fun setEditContent(editMessage: CharSequence): Builder {
            mEditContent = editMessage
            return this
        }

        fun setAdapter(adapter: BaseQuickAdapter<*, *>, onItemClickListener: BaseQuickAdapter.OnItemClickListener?): Builder {
            mAdapter = adapter
            mAdapter?.onItemClickListener = onItemClickListener
            return this
        }

        fun setAdapter(adapter: BaseQuickAdapter<*, *>, onItemClickListener: BaseQuickAdapter.OnItemClickListener, itemDecoration: RecyclerView.ItemDecoration): Builder {
            mAdapter = adapter
            mAdapter?.onItemClickListener = onItemClickListener
            mItemDecoration = itemDecoration
            return this
        }

        fun setLayoutParamsHeight(): Builder {
            mLayoutParamsHeight = 400
            return this
        }

        fun setPositiveButton(textId: Int, listener: OnClickListener?): Builder {
            mPositiveButtonText = context.getText(textId)
            mPositiveButtonListener = listener
            return this
        }

        fun setPositiveButton(text: CharSequence, listener: OnClickListener?): Builder {
            mPositiveButtonText = text
            mPositiveButtonListener = listener
            return this
        }

        fun setNegativeButton(textId: Int, listener: OnClickListener?): Builder {
            mNegativeButtonText = context.getText(textId)
            mNegativeButtonListener = listener
            return this
        }

        fun setNegativeButton(text: CharSequence, listener: OnClickListener?): Builder {
            mNegativeButtonText = text
            mNegativeButtonListener = listener
            return this
        }

        fun setNeutralButton(textId: Int, listener: OnClickListener): Builder {
            mNeutralButtonText = context.getText(textId)
            mNeutralButtonListener = listener
            return this
        }

        fun setNeutralButton(text: CharSequence, listener: OnClickListener): Builder {
            mNeutralButtonText = text
            mNeutralButtonListener = listener
            return this
        }

        fun setCancelable(cancelable: Boolean): Builder {
            mCancelable = cancelable
            return this
        }

        fun setOnCancelListener(onCancelListener: OnCancelListener): Builder {
            mOnCancelListener = onCancelListener
            return this
        }

        fun create(): MaterialDialog {
            val dialog = MaterialDialog(context, mThemeResId)
            val view = View.inflate(context, R.layout.dialog_material, null)
            mBgResId?.let { view.setBackgroundResource(it) }
            val editMessage = view.findViewById<EditText>(R.id.et_content_dialog_material)
            if (mTitle == null) {
                view.findViewById<ConstraintLayout>(R.id.cl_title_dialog_material)
                        .visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.tv_title_dialog_material).text = mTitle
                mTitleColor?.let {
                    view.findViewById<TextView>(R.id.tv_title_dialog_material).setTextColor(it)
                }
                mTitleDividerColor?.let {
                    view.findViewById<View>(R.id.v_title_dialog_material).setBackgroundColor(it)
                }
            }
            if (mContent == null) {
                view.findViewById<TextView>(R.id.tv_content_dialog_material).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.tv_content_dialog_material).text = mContent
                mContentColor?.let {
                    view.findViewById<TextView>(R.id.tv_content_dialog_material).setTextColor(it)
                }
            }
            if (mEditContent == null) {
                editMessage.visibility = View.GONE
            } else {
                editMessage.setText(mEditContent)
                editMessage.setSelection(mEditContent!!.length)
            }
            if (mAdapter == null) {
                view.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                        .visibility = View.GONE
            } else {
                view.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                        .layoutManager = LinearLayoutManager(context)
                if (mLayoutParamsHeight != 0) view.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                        .layoutParams.height = mLayoutParamsHeight
                mItemDecoration?.let {
                    view.findViewById<RecyclerView>(R.id.rv_content_dialog_material)
                            .addItemDecoration(it)
                }
                view.findViewById<RecyclerView>(R.id.rv_content_dialog_material).adapter = mAdapter
            }
            if (mPositiveButtonText == null) {
                view.findViewById<TextView>(R.id.tv_positive_dialog_material).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.tv_positive_dialog_material)
                        .text = mPositiveButtonText
            }
            if (mNegativeButtonText == null) {
                view.findViewById<TextView>(R.id.tv_negative_dialog_material).visibility = View.GONE
            } else {
                view.findViewById<TextView>(R.id.tv_negative_dialog_material)
                        .text = mNegativeButtonText
            }
            view.findViewById<TextView>(R.id.tv_positive_dialog_material).setOnClickListener {
                dialog.dismiss()
                mPositiveButtonListener?.onClick(dialog)
            }
            view.findViewById<TextView>(R.id.tv_negative_dialog_material).setOnClickListener {
                dialog.dismiss()
                mNegativeButtonListener?.onClick(dialog)
            }
            dialog.setContentView(view)
            dialog.setOnCancelListener {
                mOnCancelListener?.onCancel(it)
            }
            return dialog
        }

        fun show(): MaterialDialog {
            val dialog = create()
            dialog.show()
            return dialog
        }
    }

    interface OnClickListener {
        fun onClick(dialog: MaterialDialog)
    }

    interface OnCancelListener {
        fun onCancel(dialog: DialogInterface)
    }
}