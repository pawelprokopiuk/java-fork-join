import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SingleThreadEdgeDetection {
    public static void main(String[] args) throws Exception {
        // Load the source image
        BufferedImage image = ImageIO.read(new File("source.jpg"));
        int width = image.getWidth();
        int height = image.getHeight();

        int[] src = image.getRGB(0, 0, width, height, null, 0, width);
        int[] dst = new int[src.length];

        System.out.println("Image loaded. Starting edge detection...");

        // Perform the Sobel edge detection
        long startTime = System.currentTimeMillis();
        applySobelFilter(src, dst, width, height);
        long endTime = System.currentTimeMillis();

        System.out.println("Edge detection completed in " + (endTime - startTime) + " ms.");

        // Write the edge-detected image to file
        BufferedImage edgeDetectedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        edgeDetectedImage.setRGB(0, 0, width, height, dst, 0, width);
        ImageIO.write(edgeDetectedImage, "png", new File("edge_detected_single_thread.png"));

        System.out.println("Edge-detected image saved as edge_detected_single_thread.png.");
    }

    public static void applySobelFilter(int[] src, int[] dst, int width, int height) {
        // Sobel kernels
        int[][] Gx = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };
        int[][] Gy = {
                {-1, -2, -1},
                { 0,  0,  0},
                { 1,  2,  1}
        };

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                float gxRed = 0, gxGreen = 0, gxBlue = 0;
                float gyRed = 0, gyGreen = 0, gyBlue = 0;

                // Apply Sobel kernels
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int pixelX = x + kx;
                        int pixelY = y + ky;
                        int pixel = src[pixelY * width + pixelX];

                        int r = (pixel & 0x00ff0000) >> 16;
                        int g = (pixel & 0x0000ff00) >> 8;
                        int b = (pixel & 0x000000ff);

                        gxRed += r * Gx[ky + 1][kx + 1];
                        gxGreen += g * Gx[ky + 1][kx + 1];
                        gxBlue += b * Gx[ky + 1][kx + 1];

                        gyRed += r * Gy[ky + 1][kx + 1];
                        gyGreen += g * Gy[ky + 1][kx + 1];
                        gyBlue += b * Gy[ky + 1][kx + 1];
                    }
                }

                // Compute gradient magnitude
                int edgeRed = (int) Math.min(255, Math.sqrt(gxRed * gxRed + gyRed * gyRed));
                int edgeGreen = (int) Math.min(255, Math.sqrt(gxGreen * gxGreen + gyGreen * gyGreen));
                int edgeBlue = (int) Math.min(255, Math.sqrt(gxBlue * gxBlue + gyBlue * gyBlue));

                // Combine channels into a single ARGB pixel
                int edgePixel = (0xff000000) |
                        (edgeRed << 16) |
                        (edgeGreen << 8) |
                        edgeBlue;

                dst[y * width + x] = edgePixel;
            }
        }

        // Handle edges (set to black)
        for (int x = 0; x < width; x++) {
            dst[x] = 0xff000000; // Top row
            dst[(height - 1) * width + x] = 0xff000000; // Bottom row
        }
        for (int y = 0; y < height; y++) {
            dst[y * width] = 0xff000000; // Left column
            dst[y * width + (width - 1)] = 0xff000000; // Right column
        }
    }
}
