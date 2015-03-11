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
package org.panbox.mobile.android.gui.activity;

import org.panbox.mobile.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class FixedAspectLayout extends FrameLayout {

    private float aspect = 1.0f;

    // .. alternative constructors omitted

    public FixedAspectLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.FixedAspectLayout);
        aspect = a.getFloat(R.styleable.FixedAspectLayout_aspectRatio, 1.0f);
        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int w = MeasureSpec.getSize(widthMeasureSpec);
        int h = MeasureSpec.getSize(heightMeasureSpec);

        if (w == 0) {
            h = 0;
        } 
        else if (h < w ) {	// if given ratio is lower than that the one I want then set the ratio I want 
            w = (int)(h / aspect);
        } else {
            h = (int)(w * aspect);
        }
  
/*        
        else if (h / w < aspect) {	// if given ratio is lower than that the one I want then set the ratio I want 
            w = (int)(h / aspect);
        } else {
            h = (int)(w * aspect);
        }
        
*/

        super.onMeasure(
                MeasureSpec.makeMeasureSpec(w,
                        MeasureSpec.getMode(widthMeasureSpec)),
                MeasureSpec.makeMeasureSpec(h,
                        MeasureSpec.getMode(heightMeasureSpec)));
    }
}
