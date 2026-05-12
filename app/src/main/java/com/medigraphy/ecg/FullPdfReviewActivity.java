package com.medigraphy.ecg;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;

import java.io.File;

public class FullPdfReviewActivity extends Activity implements OnPageChangeListener, OnLoadCompleteListener {

    int pageNumber;
    String pdfFileName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_full_pdf);

        ImageView imgClose = findViewById(R.id.btnpdfClose);
        PDFView pdfPopup = findViewById(R.id.pdfDialogview);

        Intent i = getIntent();
        final String fileName = i.getStringExtra("selectedfile");

        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports" + "/vora/" + fileName);
        if (path.exists()) {
            pdfPopup.fromFile(path)
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .enableAnnotationRendering(true)
                    .scrollHandle(new DefaultScrollHandle(FullPdfReviewActivity.this))
                    .load();

        } else {
            Toast.makeText(FullPdfReviewActivity.this, "Pdf Not available ", Toast.LENGTH_SHORT).show();
        }

        imgClose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                startActivity(new Intent(FullPdfReviewActivity.this, SendReportViewActivity.class).putExtra("filename",fileName));
                finish();
            }
        });
    }

    @Override
    public void loadComplete(int nbPages) {

    }

    @Override
    public void onPageChanged(int page, int pageCount) {

        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));

    }
}