package com.example.licenseplateidentify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.example.licenseplateidentify.databinding.ActivityMainBinding;
import com.hyperai.hyperlpr3.HyperLPR3;
import com.hyperai.hyperlpr3.bean.HyperLPRParameter;
import com.hyperai.hyperlpr3.bean.Plate;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> resultLauncherCamera;
    private ActivityResultLauncher<Intent> resultLauncherFile;

    private ActivityMainBinding vb;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = DataBindingUtil.setContentView(this, R.layout.activity_main);

        HyperLPR3.getInstance().init(this, new HyperLPRParameter());

        resultLauncherCamera = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                assert result.getData() != null;
                Bundle bundle = result.getData().getExtras();
                // 转换图片的二进制流
                assert bundle != null;
                Bitmap bitmap = (Bitmap) bundle.get("data");
                // 设置图片
                vb.ivImage.setImageBitmap(bitmap);
                handleImage(bitmap);
            }
        });

        resultLauncherFile = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == Activity.RESULT_OK) {
                Intent intent = result.getData();
                assert intent != null;
                Uri uri = intent.getData();
                assert uri != null;
                String filePath = getPathFromUri(this, uri);
                if (!TextUtils.isEmpty(filePath)) {
                    File execlFile = new File(filePath);
                    if (execlFile.exists()) {
                        try {
                            Workbook workbook = WorkbookFactory.create(execlFile);
                            Sheet sheet = workbook.getSheetAt(0);
                            Row row = sheet.getRow(0);

                            Log.w("PWDebug", "name = " + row.getCell(0).getStringCellValue());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        });

        vb.tvScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                resultLauncherCamera.launch(intent);
            }
        });

        vb.tvInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                resultLauncherFile.launch(intent);
            }
        });
    }

    public void handleImage(Bitmap bitmap) {
        Plate[] plates = HyperLPR3.getInstance()
                .plateRecognition(bitmap, HyperLPR3.CAMERA_ROTATION_0, HyperLPR3.STREAM_BGRA);
        if (plates != null) {
            for (Plate plate : plates) {
                vb.tvLicensePlate.setText(plate.getCode());
            }
        }
    }

    public static String getPathFromUri(Context context, Uri uri) {
        String filePath = null;
        String[] projection = {MediaStore.Files.FileColumns.DATA};
        Cursor cursor = context.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA);
            cursor.moveToFirst();
            filePath = cursor.getString(columnIndex);
            cursor.close();
        }
        return filePath;
    }

}