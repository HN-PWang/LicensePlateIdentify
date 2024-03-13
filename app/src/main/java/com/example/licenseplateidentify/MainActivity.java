package com.example.licenseplateidentify;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
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

import org.apache.poi.ss.usermodel.Workbook;

public class MainActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> resultLauncher;

    private ActivityMainBinding vb;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vb = DataBindingUtil.setContentView(this, R.layout.activity_main);

        HyperLPR3.getInstance().init(this, new HyperLPRParameter());

        resultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), result -> {
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

        vb.tvScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                resultLauncher.launch(intent);
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

}