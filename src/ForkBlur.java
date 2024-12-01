import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.ForkJoinPool;

public class ForkBlur extends RecursiveAction {
    private static final int sThreshold = 10000; // Threshold for splitting tasks
    private final int[] mSource;
    private final int mStart;
    private final int mLength;
    private final int[] mDestination;
    private final int mBlurWidth = 15; // Processing window size

    public ForkBlur(int[] src, int start, int length, int[] dst) {
        mSource = src;
        mStart = start;
        mLength = length;
        mDestination = dst;
    }

    @Override
    protected void compute() {
        if (mLength < sThreshold) {
            computeDirectly();
            return;
        }

        int split = mLength / 2;

        invokeAll(
                new ForkBlur(mSource, mStart, split, mDestination),
                new ForkBlur(mSource, mStart + split, mLength - split, mDestination)
        );
    }

    protected void computeDirectly() {
        int sidePixels = (mBlurWidth - 1) / 2;

        for (int index = mStart; index < mStart + mLength; index++) {
            // Calculate average
            float rt = 0, gt = 0, bt = 0;

            for (int mi = -sidePixels; mi <= sidePixels; mi++) {
                int mindex = Math.min(Math.max(mi + index, 0), mSource.length - 1);
                int pixel = mSource[mindex];
                rt += (float) ((pixel & 0x00ff0000) >> 16) / mBlurWidth;
                gt += (float) ((pixel & 0x0000ff00) >> 8) / mBlurWidth;
                bt += (float) ((pixel & 0x000000ff) >> 0) / mBlurWidth;
            }

            // Reassemble destination pixel
            int dpixel = (0xff000000) |
                    (((int) rt) << 16) |
                    (((int) gt) << 8) |
                    (((int) bt));
            mDestination[index] = dpixel;
        }
    }

    public static void main(String[] args) throws Exception {
        // Load the source image
        BufferedImage image = ImageIO.read(new File("source.jpg"));
        int width = image.getWidth();
        int height = image.getHeight();

        int[] src = image.getRGB(0, 0, width, height, null, 0, width);
        int[] dst = new int[src.length];

        System.out.println("Image loaded. Starting blur...");

        // Create the ForkJoinPool
        ForkJoinPool pool = new ForkJoinPool();

        // Start the ForkBlur task
        ForkBlur fb = new ForkBlur(src, 0, src.length, dst);
        long startTime = System.currentTimeMillis();
        pool.invoke(fb);
        long endTime = System.currentTimeMillis();

        System.out.println("Blur completed in " + (endTime - startTime) + " ms.");

        // Write the blurred image to file
        BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        blurredImage.setRGB(0, 0, width, height, dst, 0, width);
        ImageIO.write(blurredImage, "png", new File("blurred.png"));

        System.out.println("Blurred image saved as blurred.png.");
    }
}
