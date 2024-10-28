
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import java.awt.image.BufferedImage;


public class ImageDisplay {

	JFrame frame;
	JLabel lbIm1;
	BufferedImage imgOne;

	int outputWidth;   // Output width based on the command line parameter
	int outputHeight;  // Output height based on the command line parameter

	/** Read Image RGB
	 *  Reads the image of given width and height at the given imgPath into the provided BufferedImage.
	 */
	private void readImageRGB(int width, int height, String imgPath, BufferedImage img) {
		try {
			int frameLength = width * height * 3;
			File file = new File(imgPath);
			RandomAccessFile raf = new RandomAccessFile(file, "r");
			raf.seek(0);

			byte[] bytes = new byte[frameLength];
			raf.read(bytes);
			int ind = 0;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					byte r = bytes[ind];
					byte g = bytes[ind + height * width];
					byte b = bytes[ind + height * width * 2];

					int pix = 0xff000000 | ((r & 0xff) << 16) | ((g & 0xff) << 8) | (b & 0xff);
					img.setRGB(x, y, pix);
					ind++;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public void showIms(String[] args){
		// Parse command line arguments
		String imgPath = args[0];
		int inputWidth = Integer.parseInt(args[1]);
		int inputHeight = Integer.parseInt(args[2]);
		int resamplingMethod = Integer.parseInt(args[3]);
		String outputFormat = args[4];

		// Set output dimensions based on the output format
		switch (outputFormat) {
			case "O1":
				outputWidth = 1920;
				outputHeight = 1080;
				break;
			case "O2":
				outputWidth = 1280;
				outputHeight = 720;
				break;
			case "O3":
				outputWidth = 640;
				outputHeight = 480;
				break;
			default:
				throw new IllegalArgumentException("Invalid output format");
		}

		// Initialize images
		imgOne = new BufferedImage(inputWidth, inputHeight, BufferedImage.TYPE_INT_RGB);

		readImageRGB(inputWidth, inputHeight, imgPath, imgOne);

		// Resample the image based on the method
		BufferedImage resampledImage = resampleImage(imgOne, outputWidth, outputHeight, resamplingMethod);

		// Display image
		frame = new JFrame();
		lbIm1 = new JLabel(new ImageIcon(resampledImage));
		frame.getContentPane().add(lbIm1);
		frame.pack();
		frame.setVisible(true);
	}
	private BufferedImage resampleImage(BufferedImage inputImage, int width, int height, int method) {
		BufferedImage outputImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		// Get input dimensions
		int inputWidth = inputImage.getWidth();
		int inputHeight = inputImage.getHeight();
		if (method == 0){ //specific sampling for down-sampling

			double scaleX = (double) inputWidth / width;
			double scaleY = (double) inputHeight / height;

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int startX = (int) (x * scaleX);
					int startY = (int) (y * scaleY);
					int rgb = inputImage.getRGB(startX, startY);
					outputImage.setRGB(x, y, rgb);
				}
			}
		}else if (method == 1) {  // Average Sampling (typically for downsampling)

			double scaleX = (double) inputWidth / width;
			double scaleY = (double) inputHeight / height;

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int startX = (int) (x * scaleX);
					int startY = (int) (y * scaleY);
					int endX = Math.min(inputWidth, (int) Math.ceil(startX + scaleX));
					int endY = Math.min(inputHeight, (int) Math.ceil(startY + scaleY));

					long sumR = 0, sumG = 0, sumB = 0;
					int count = 0;

					for (int ix = startX; ix < endX; ix++) {
						for (int iy = startY; iy < endY; iy++) {
							if (ix < inputWidth && iy < inputHeight) {
								int rgb = inputImage.getRGB(ix, iy);
								sumR += (rgb >> 16) & 0xFF;
								sumG += (rgb >> 8) & 0xFF;
								sumB += rgb & 0xFF;
								count++;
							}
						}
					}

					if (count > 0) {
						int avgR = (int) (sumR / count);
						int avgG = (int) (sumG / count);
						int avgB = (int) (sumB / count);

						int avgRGB = (0xFF << 24) | (avgR << 16) | (avgG << 8) | avgB;
						outputImage.setRGB(x, y, avgRGB);
					}
				}
			}
		} else if (method == 2) {  // Nearest Neighbor (typically for upsampling)
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					int srcX = x * inputWidth / width;
					int srcY = y * inputHeight / height;
					int rgb = inputImage.getRGB(srcX, srcY);
					outputImage.setRGB(x, y, rgb);
				}
			}
		}  else if (method == 3){ // bilinearInterpolate for upsampling
			double scaleX = (double) inputWidth / width;
			double scaleY = (double) inputHeight / height;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double srcX = (x + 0.5) * scaleX - 0.5;
					double srcY = (y + 0.5) * scaleY - 0.5;

					outputImage.setRGB(x, y, bilinearInterpolate(inputImage, srcX, srcY));
				}
			}
		} else if (method == 4) { //bicubicInterpolate for upsampling
			double scaleX = (double) inputWidth / width;
			double scaleY = (double) inputHeight / height;
			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double srcX = (x + 0.5) * scaleX - 0.5;
					double srcY = (y + 0.5) * scaleY - 0.5;
					outputImage.setRGB(x, y, bicubicInterpolate(inputImage, srcX, srcY));

				}
			}
		} else if (method == 5) {

			double scaleX = (double) inputWidth / width;
			double scaleY = (double) inputHeight / height;

			int centerX = width / 2;
			int centerY = height / 2;

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					double distanceX = (x - centerX) / (double) centerX;
					double distanceY = (y - centerY) / (double) centerY;
					double distance = Math.sqrt(distanceX * distanceX + distanceY * distanceY);

					double stretchFactor = 0.5 + (Math.pow(distance, 0.6) * 0.5);
					stretchFactor = Math.min(1.05, stretchFactor);
					System.out.println(stretchFactor);

					double srcX = (x - centerX) / stretchFactor + centerX;
					double srcY = (y - centerY) / stretchFactor + centerY;

					srcX = srcX * scaleX;
					srcY = srcY * scaleY;
					System.out.println(srcX + " " + srcY+" "+inputWidth + " " + inputHeight);
					srcX = Math.max(0, Math.min(srcX, inputWidth - 1));
					srcY = Math.max(0, Math.min(srcY, inputHeight - 1));

					int rgb = bilinearInterpolate(inputImage, srcX, srcY);
					outputImage.setRGB(x, y, rgb);
				}
			}

		}
		return outputImage;
	}
	private int getPixel(BufferedImage image, int x, int y) {
		if (x < 0 || x >= image.getWidth() || y < 0 || y >= image.getHeight()) {
			return 0; // Out of bounds, return some default color or handle this case as needed
		}
		return image.getRGB(x, y);
	}


	private int bilinearInterpolate(BufferedImage image, double x, double y) {
		int x0 = (int) Math.floor(x);
		int x1 = x0 + 1;
		int y0 = (int) Math.floor(y);
		int y1 = y0 + 1;

		int Ia = getPixel(image, x0, y0);
		int Ib = getPixel(image, x1, y0);
		int Ic = getPixel(image, x0, y1);
		int Id = getPixel(image, x1, y1);

		double t = x - x0;
		double u = y - y0;

		// Interpolating in the x-direction
		int Iab = interpolate(Ia, Ib, t);
		int Icd = interpolate(Ic, Id, t);

		// Interpolating in the y-direction
		return interpolate(Iab, Icd, u);
	}
	private int interpolate(int start, int end, double t) {
		int sR = (start >> 16) & 0xFF;
		int sG = (start >> 8) & 0xFF;
		int sB = start & 0xFF;

		int eR = (end >> 16) & 0xFF;
		int eG = (end >> 8) & 0xFF;
		int eB = end & 0xFF;

		int R = (int) (sR + t * (eR - sR));
		int G = (int) (sG + t * (eG - sG));
		int B = (int) (sB + t * (eB - sB));

		return (0xFF << 24) | (R << 16) | (G << 8) | B;
	}
	private int bicubicInterpolate(BufferedImage image, double x, double y) {
		int[] pixels = new int[16];
		int ix = (int) Math.floor(x);
		int iy = (int) Math.floor(y);
		double dx = x - ix;
		double dy = y - iy;

		for (int m = -1; m <= 2; m++) {
			for (int n = -1; n <= 2; n++) {
				pixels[(m + 1) * 4 + (n + 1)] = getPixel(image, ix + n, iy + m);
			}
		}

		// Interpolate rows
		int[] rowResults = new int[4];
		for (int row = 0; row < 4; row++) {
			rowResults[row] = cubicInterpolate(
					pixels[row * 4],
					pixels[row * 4 + 1],
					pixels[row * 4 + 2],
					pixels[row * 4 + 3],
					dx);
		}

		// Interpolate column based on the results from row interpolation
		return cubicInterpolate(rowResults[0], rowResults[1], rowResults[2], rowResults[3], dy);
	}
	private int cubicInterpolate(int p0, int p1, int p2, int p3, double t) {
		int a0, a1, a2, a3;
		int[] output = new int[3];

		int[] v0 = extractColors(p0);
		int[] v1 = extractColors(p1);
		int[] v2 = extractColors(p2);
		int[] v3 = extractColors(p3);

		for (int i = 0; i < 3; i++) {  // Iterate over R, G, B components
			a0 = v3[i] - v2[i] - v0[i] + v1[i];
			a1 = v0[i] - v1[i] - a0;
			a2 = v2[i] - v0[i];
			a3 = v1[i];

			output[i] = (int) (a0 * (t * t * t) + a1 * (t * t) + a2 * t + a3);
			if (output[i] < 0) output[i] = 0;
			if (output[i] > 255) output[i] = 255;
		}

		return (0xFF << 24) | (output[0] << 16) | (output[1] << 8) | output[2];
	}
	private int[] extractColors(int rgb) {
		return new int[] {
				(rgb >> 16) & 0xFF,  // Red
				(rgb >> 8) & 0xFF,   // Green
				(rgb) & 0xFF         // Blue
		};
	}


	public static void main(String[] args) {
		ImageDisplay ren = new ImageDisplay();
		ren.showIms(args);
	}

}
