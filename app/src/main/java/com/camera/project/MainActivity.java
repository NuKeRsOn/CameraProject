package com.camera.project;

import java.io.IOException;

import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.camera.project.R;

public class MainActivity extends Activity {

    SurfaceView surfaceView;
    SurfaceHolder holder;
    HolderCallback holderCallback;
    Camera camera;

    final int CAMERA_ID = 0;
    final int CAMERA_FRONT_ID = 1;
    final boolean FULL_SCREEN = true;
    private int currentCameraId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_camera);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Button switchButton = (Button) findViewById(R.id.switch_button);


        Button takePictureButton = (Button) findViewById(R.id.take_picture);
        surfaceView = (SurfaceView) findViewById(R.id.surface_camera);

        surfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.autoFocus(new Camera.AutoFocusCallback() {
                    @Override
                    public void onAutoFocus(boolean success, Camera camera) {

                    }
                });

            }
        });



        takePictureButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                camera.takePicture(null, null, new PhotoHandler(getBaseContext()));


            }
        });

       int numberOfCameras = Camera.getNumberOfCameras();

        if (numberOfCameras == 1){
            switchButton.setVisibility(View.GONE);
        }else{
            switchButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (camera != null) {
                        camera.stopPreview();
                        camera.release();
                                        camera = null;
                    }


                    if (currentCameraId == CameraInfo.CAMERA_FACING_BACK)
                        currentCameraId = CameraInfo.CAMERA_FACING_FRONT;
                    else
                        currentCameraId = CameraInfo.CAMERA_FACING_BACK;

                    try {
                        camera = Camera.open(currentCameraId);


                        camera.setDisplayOrientation(90);

                        camera.setPreviewDisplay(holder);

                        camera.startPreview();

                    }
                    catch (Exception e) { e.printStackTrace(); }


                }
            });
        }




    }


    @Override
    protected void onResume() {
        super.onResume();

        openCamera(CAMERA_ID);


    }

    void openCamera(int cameraId){
        currentCameraId = cameraId;
        holder = surfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        holderCallback = new HolderCallback();
        holder.addCallback(holderCallback);

        camera = Camera.open();
        setPreviewSize(false);

    }
    @Override
    protected void onPause() {
        super.onPause();
        closeCamera();
    }

    void closeCamera(){
        if (camera != null)
        {
            //todo после блока не работает
            holder.removeCallback(holderCallback);
            surfaceView.destroyDrawingCache();
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }
    class HolderCallback implements SurfaceHolder.Callback {

        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width,
                                   int height) {
            camera.stopPreview();
            setCameraDisplayOrientation(CAMERA_ID);
            try {
                camera.setPreviewDisplay(holder);
                camera.startPreview();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {

        }

    }

    void setPreviewSize(boolean fullScreen) {

        // получаем размеры экрана
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        // определяем размеры превью камеры
        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();

        // RectF экрана, соотвествует размерам экрана
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());

        // RectF первью
        if (widthIsMax) {
            // превью в горизонтальной ориентации
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            // превью в вертикальной ориентации
            rectPreview.set(0, 0, size.height, size.width);
        }

        Matrix matrix = new Matrix();
        // подготовка матрицы преобразования
        if (!fullScreen) {
            // если превью будет "втиснут" в экран (второй вариант из урока)
            matrix.setRectToRect(rectPreview, rectDisplay,
                    Matrix.ScaleToFit.START);
        } else {
            // если экран будет "втиснут" в превью (третий вариант из урока)
            matrix.setRectToRect(rectDisplay, rectPreview,
                    Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        // преобразование
        matrix.mapRect(rectPreview);

        // установка размеров surface из получившегося преобразования
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
    }

    void setCameraDisplayOrientation(int cameraId) {
        // определяем насколько повернут экран от нормального положения
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result = 0;

        // получаем инфо по камере cameraId
        CameraInfo info = new CameraInfo();
        Camera.getCameraInfo(cameraId, info);


        // задняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_BACK) {
            result = ((360 - degrees) + info.orientation);
        }else
        // передняя камера
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = ((360 - degrees) - info.orientation);
            result += 360;
        }
        result = result % 360;
        camera.setDisplayOrientation(result);
    }
}