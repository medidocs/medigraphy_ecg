package com.medigraphy.ecg.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.medigraphy.ecg.R;

import java.util.ArrayList;

public class PdfListAdapter extends BaseAdapter {

    Context context;
    public ArrayList<String> arrPdfList;
    public ArrayList<String> arrDuplicatePDFList;
    OnDelete onDelete;
    boolean isShowDeleteIcon = true;

    public PdfListAdapter(Context context, ArrayList<String> arrPdfList, ArrayList<String> arrDuplicatePDFList, OnDelete onDelete) {
        this.context = context;
        this.arrPdfList = arrPdfList;
        this.onDelete = onDelete;
        this.arrDuplicatePDFList = arrDuplicatePDFList;
    }

    public interface OnDelete {
        public void onClickDelete(int position);
    }

    @Override
    public int getCount() {
        return arrPdfList.size();
    }

    @Override
    public Object getItem(int i) {
        return arrPdfList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {

        view = LayoutInflater.from(context).inflate(R.layout.list_item, viewGroup, false);
        TextView tv = view.findViewById(R.id.tv);
        RelativeLayout rlBgItem = view.findViewById(R.id.rlBgItem);
        ImageView imgDelete = view.findViewById(R.id.imgDelete);

        if (isShowDeleteIcon){
            imgDelete.setVisibility(View.VISIBLE);
        }else {
            imgDelete.setVisibility(View.GONE);
        }

        tv.setText(arrPdfList.get(i));

        if (arrDuplicatePDFList.size() > 0) {
            if (arrDuplicatePDFList.contains(arrPdfList.get(i))) {
                rlBgItem.setBackgroundColor(ContextCompat.getColor(context, R.color.select_items));
            }
        }
        imgDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onDelete.onClickDelete(i);
            }
        });
        return view;

    }

    public void deleteFunctionality(boolean isShowDelete){
        isShowDeleteIcon = isShowDelete;
    }

}