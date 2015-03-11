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
package org.panbox.desktop.common.pairing.cam;

import java.awt.Color;
import java.awt.image.BufferedImage;

import org.bytedeco.javacpp.opencv_core.IplImage;
import org.bytedeco.javacv.OpenCVFrameGrabber;

@SuppressWarnings("serial")
public class OpenCVWebCam extends ImagePanel {

	private boolean running = false;
	private Thread runner = null;
	private OpenCVFrameGrabber grabber;
	private CVImageProcessor imageProcessor = null;

	public OpenCVWebCam(int device, int width, int height) {
		this.grabber = new OpenCVFrameGrabber(device);
		grabber.setImageWidth(width);
		grabber.setImageHeight(height);
		this.setBackground(Color.LIGHT_GRAY);
	}

	public void setImageProcessor(CVImageProcessor imageProcessor) {
		this.imageProcessor = imageProcessor;
	}

	public CVImageProcessor getImageProcessor() {
		return imageProcessor;
	}

	private void grabAndPaint() {
		try {
			BufferedImage out;

			// grab the raw image from the webcam
			IplImage frame = grabber.grab();

			// if an image processor has been defined, start processing the
			// image
			if (imageProcessor != null) {
				frame = imageProcessor.process(frame);
			}

			// output the final result as a bufferedimage
			out = frame.getBufferedImage();

			this.setImage(out);
		} catch (Exception ex) {
			// TODO Exception!
			ex.printStackTrace();
		}
		this.repaint();
	}

	/**
	 * Start grabbing frames from the webcam.
	 *
	 * @throws Exception
	 */
	public void start() throws Exception {
		if (running) {
			return;
		}

		grabber.start();

		running = true;
		runner = new Thread() {
			@Override
			public void run() {
				while (running) {
					grabAndPaint();
					Thread.yield();
				}
				try {
					grabber.stop();
				} catch (Exception ex) {
					// TODO Exception!
					ex.printStackTrace();
				}
				runner = null;
			}
		};
		runner.start();
	}

	public boolean isRunnning() {
		return runner != null;
	}

	public void stop() {
		running = false;
	}
}
