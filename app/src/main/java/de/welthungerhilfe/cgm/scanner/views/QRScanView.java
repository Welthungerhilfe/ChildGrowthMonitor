/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import de.welthungerhilfe.cgm.scanner.helper.AppConstants;
import de.welthungerhilfe.cgm.scanner.utils.BitmapUtils;
import me.dm7.barcodescanner.core.BarcodeScannerView;
import me.dm7.barcodescanner.core.DisplayUtils;

/**
 * Created by Emerald on 2/21/2018.
 */

public class QRScanView extends BarcodeScannerView {
    private static final String TAG = QRScanView.class.getSimpleName();
    private MultiFormatReader mMultiFormatReader;
    public static final List<BarcodeFormat> ALL_FORMATS = new ArrayList();
    private List<BarcodeFormat> mFormats;
    private QRScanView.QRScanHandler mResultHandler;

    public QRScanView(Context context) {
        super(context);
        this.initMultiFormatReader();
    }

    public QRScanView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        this.initMultiFormatReader();
    }

    public void setFormats(List<BarcodeFormat> formats) {
        this.mFormats = formats;
        this.initMultiFormatReader();
    }

    public void setResultHandler(QRScanView.QRScanHandler resultHandler) {
        this.mResultHandler = resultHandler;
    }

    public Collection<BarcodeFormat> getFormats() {
        return this.mFormats == null?ALL_FORMATS:this.mFormats;
    }

    private void initMultiFormatReader() {
        Map<DecodeHintType, Object> hints = new EnumMap(DecodeHintType.class);
        hints.put(DecodeHintType.POSSIBLE_FORMATS, this.getFormats());
        this.mMultiFormatReader = new MultiFormatReader();
        this.mMultiFormatReader.setHints(hints);
    }

    public void onPreviewFrame(byte[] data, final Camera camera) {
        if(this.mResultHandler != null) {
            try {
                final byte[] finalData = data;

                Camera.Parameters parameters = camera.getParameters();
                Camera.Size size = parameters.getPreviewSize();
                int width = size.width;
                int height = size.height;
                if(DisplayUtils.getScreenOrientation(this.getContext()) == 1) {
                    byte[] rotatedData = new byte[data.length];
                    int y = 0;

                    while(true) {
                        if(y >= height) {
                            y = width;
                            width = height;
                            height = y;
                            data = rotatedData;
                            break;
                        }

                        for(int x = 0; x < width; ++x) {
                            rotatedData[x * height + height - y - 1] = data[x + y * width];
                        }

                        ++y;
                    }
                }

                Result rawResult = null;
                PlanarYUVLuminanceSource source = this.buildLuminanceSource(data, width, height);
                if(source != null) {
                    BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
                    try {
                        rawResult = this.mMultiFormatReader.decodeWithState(bitmap);
                    } catch (ReaderException var17) {
                        ;
                    } catch (NullPointerException var18) {
                        ;
                    } catch (ArrayIndexOutOfBoundsException var19) {
                        ;
                    } finally {
                        this.mMultiFormatReader.reset();
                    }
                }

                if(rawResult != null) {
                    Handler handler = new Handler(Looper.getMainLooper());
                    final Result finalRawResult = rawResult;
                    handler.post(new Runnable() {
                        public void run() {
                            QRScanView.QRScanHandler tmpResultHandler = QRScanView.this.mResultHandler;
                            QRScanView.this.mResultHandler = null;
                            QRScanView.this.stopCameraPreview();
                            if(tmpResultHandler != null) {
                                Camera.Parameters parameters = camera.getParameters();
                                int format = parameters.getPreviewFormat();

                                if (format == ImageFormat.NV21 || format == ImageFormat.YUY2 || format == ImageFormat.NV16) {
                                    int w = parameters.getPreviewSize().width;
                                    int h = parameters.getPreviewSize().height;
                                    YuvImage yuv_image = new YuvImage(finalData, format, w, h, null);

                                    Rect rect = new Rect(0, 0, w, h);
                                    ByteArrayOutputStream output_stream = new ByteArrayOutputStream();
                                    yuv_image.compressToJpeg(rect, 100, output_stream);
                                    byte[] byt = output_stream.toByteArray();
                                    Bitmap bmp = BitmapUtils.getAcceptableBitmap(BitmapFactory.decodeByteArray(byt, 0, byt.length));
                                    byte[] data = BitmapUtils.getByteData(bmp);
                                    tmpResultHandler.handleQRResult(finalRawResult.getText(), data);
                                } else {
                                    Bitmap bmp = BitmapUtils.getAcceptableBitmap(BitmapFactory.decodeByteArray(finalData, 0, finalData.length));
                                    byte[] data = BitmapUtils.getByteData(bmp);
                                    tmpResultHandler.handleQRResult(finalRawResult.getText(), data);
                                }
                            }
                        }
                    });
                } else {
                    camera.setOneShotPreviewCallback(this);
                }
            } catch (RuntimeException var21) {
                Log.e("QRScanView", var21.toString(), var21);
            }

        }
    }

    public void resumeCameraPreview(QRScanView.QRScanHandler resultHandler) {
        this.mResultHandler = resultHandler;
        super.resumeCameraPreview();
    }

    public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
        Rect rect = this.getFramingRectInPreview(width, height);
        if(rect == null) {
            return null;
        } else {
            PlanarYUVLuminanceSource source = null;

            try {
                source = new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top, rect.width(), rect.height(), false);
            } catch (Exception var7) {
                ;
            }

            return source;
        }
    }

    static {
        /*
        ALL_FORMATS.add(BarcodeFormat.UPC_A);
        ALL_FORMATS.add(BarcodeFormat.UPC_E);
        ALL_FORMATS.add(BarcodeFormat.EAN_13);
        ALL_FORMATS.add(BarcodeFormat.EAN_8);
        ALL_FORMATS.add(BarcodeFormat.RSS_14);
        ALL_FORMATS.add(BarcodeFormat.CODE_39);
        ALL_FORMATS.add(BarcodeFormat.CODE_93);
        ALL_FORMATS.add(BarcodeFormat.CODE_128);
        ALL_FORMATS.add(BarcodeFormat.ITF);
        ALL_FORMATS.add(BarcodeFormat.CODABAR);
        */
        ALL_FORMATS.add(BarcodeFormat.QR_CODE);
        /*
        ALL_FORMATS.add(BarcodeFormat.DATA_MATRIX);
        ALL_FORMATS.add(BarcodeFormat.PDF_417);
        */
    }

    public interface QRScanHandler {
        void handleQRResult(String qrCode, byte[] bitmap);
    }
}
