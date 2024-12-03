import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

public class ForkEdgeDetection extends RecursiveAction {
    private static final int sThreshold = 10000; // Threshold for splitting tasks
    private final int[] mSource;
    private final int mStart;
    private final int mLength;
    private final int[] mDestination;
    private final int mWidth;
    private final int mHeight;

    public ForkEdgeDetection(int[] src, int start, int length, int[] dst, int width, int height) {
        mSource = src;
        mStart = start;
        mLength = length;
        mDestination = dst;
        mWidth = width;
        mHeight = height;
    }

    @Override
    protected void compute() {
        if (mLength < sThreshold) {
            computeDirectly();
            return;
        }

        int split = mLength / 2;

        invokeAll(
                new ForkEdgeDetection(mSource, mStart, split, mDestination, mWidth, mHeight),
                new ForkEdgeDetection(mSource, mStart + split, mLength - split, mDestination, mWidth, mHeight)
        );
    }

    protected void computeDirectly() {
        // Kernels
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

        for (int index = mStart; index < mStart + mLength; index++) {
            int x = index % mWidth;
            int y = index / mWidth;

            // Skip edges
            if (x == 0 || x == mWidth - 1 || y == 0 || y == mHeight - 1) {
                mDestination[index] = 0xff000000; // black
                continue;
            }

            float gxRed = 0, gxGreen = 0, gxBlue = 0;
            float gyRed = 0, gyGreen = 0, gyBlue = 0;

            // Apply Sobel filter
            for (int ky = -1; ky <= 1; ky++) {
                for (int kx = -1; kx <= 1; kx++) {
                    int pixelX = x + kx;
                    int pixelY = y + ky;
                    int pixel = mSource[pixelY * mWidth + pixelX];

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

            // compute gradient magnitude
            int edgeRed = (int) Math.min(255, Math.sqrt(gxRed * gxRed + gyRed * gyRed));
            int edgeGreen = (int) Math.min(255, Math.sqrt(gxGreen * gxGreen + gyGreen * gyGreen));
            int edgeBlue = (int) Math.min(255, Math.sqrt(gxBlue * gxBlue + gyBlue * gyBlue));

            // combine channels into single pixel
            int edgePixel = (0xff000000) |
                    (edgeRed << 16) |
                    (edgeGreen << 8) |
                    edgeBlue;

            mDestination[index] = edgePixel;
        }
    }

    public static void main(String[] args) throws Exception {
        // Load the source image
        BufferedImage image = ImageIO.read(new File("source.jpg"));
        int width = image.getWidth();
        int height = image.getHeight();

        int[] src = image.getRGB(0, 0, width, height, null, 0, width);
        int[] dst = new int[src.length];

        System.out.println("Image loaded. Starting edge detection...");

        // Create the ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();

        // Start the ForkEdgeDetection task
        ForkEdgeDetection task = new ForkEdgeDetection(src, 0, src.length, dst, width, height);
        long startTime = System.currentTimeMillis();
        pool.invoke(task);
        long endTime = System.currentTimeMillis();

        System.out.println("Edge detection completed in " + (endTime - startTime) + " ms.");

        // Write image
        BufferedImage edgeDetectedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        edgeDetectedImage.setRGB(0, 0, width, height, dst, 0, width);
        ImageIO.write(edgeDetectedImage, "png", new File("edge_detected.png"));

        System.out.println("Edge-detected image saved as edge_detected.png.");
    }
}
