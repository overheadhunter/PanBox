/*
 * 
 *               Panbox - encryption for cloud storage 
 *      Copyright (C) 2014-2015 by Fraunhofer SIT and Sirrix AG 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Additonally, third party code may be provided with notices and open source
 * licenses from communities and third parties that govern the use of those
 * portions, and any licenses granted hereunder do not alter any rights and
 * obligations you may have under such open source licenses, however, the
 * disclaimer of warranty and limitation of liability provisions of the GPLv3 
 * will apply to all the product.
 * 
 */
package org.panbox.mobile.android.gui.webcam;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

public class QRCodeProcessor extends AbstractProcessor {

    private List<QRCodeListener> listeners = new ArrayList<QRCodeListener>();
    private long lastFoundTime = 0;
    private final static long CLEAR_RESULT_TIMEOUT = 5000;
    private String lastResult = "";

    public QRCodeProcessor() {
    }

    public void addSquareCodeListener(QRCodeListener sql) {
        listeners.add(sql);
    }

    public void removeSquareCodeListener(QRCodeListener sql) {
        listeners.remove(sql);
    }

    private void fire(String text) {
        List<QRCodeListener> tmp = new ArrayList<QRCodeListener>(listeners);
        for (QRCodeListener sql : tmp) {
            sql.qrCodeDetected(text);
        }
    }

    @Override
    public Bitmap processImage(Bitmap bMap) {
    	
    	int[] intArray = new int[bMap.getWidth()*bMap.getHeight()];  
    	//copy pixel data from the Bitmap into the 'intArray' array  
    	bMap.getPixels(intArray, 0, bMap.getWidth(), 0, 0, bMap.getWidth(), bMap.getHeight());  

    	LuminanceSource source = new RGBLuminanceSource(bMap.getWidth(), bMap.getHeight(),intArray);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
        Result result;
        try {
            result = new MultiFormatReader().decode(bitmap);
            lastResult = result.getText();
            lastFoundTime = System.currentTimeMillis();
            fire(result.getText());
        } catch (NotFoundException e) {
            //that's ok
            if (!lastResult.equals("")) //if result was not empty, clear old result
            {
                long mt1 = System.currentTimeMillis();
                if (mt1 - lastFoundTime > CLEAR_RESULT_TIMEOUT) {
                    lastFoundTime = mt1;
                    fire("");
                    lastResult = "";
                }
            }
        }

        return bMap;
    }
}
