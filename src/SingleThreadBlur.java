import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class SingleThreadBlur {
    private static final int BLUR_WIDTH = 15; // Processing window size

    public static void main(String[] args) throws Exception {
        // Load the source image
        BufferedImage image = ImageIO.read(new File("source.jpg"));
        int width = image.getWidth();
        int height = image.getHeight();

        int[] src = image.getRGB(0, 0, width, height, null, 0, width);
        int[] dst = new int[src.length];

        System.out.println("Image loaded. Starting blur...");

        // Perform the blur
        long startTime = System.currentTimeMillis();
        blurImage(src, dst, width, height);
        long endTime = System.currentTimeMillis();

        System.out.println("Blur completed in " + (endTime - startTime) + " ms.");

        // Write the blurred image to file
        BufferedImage blurredImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        blurredImage.setRGB(0, 0, width, height, dst, 0, width);
        ImageIO.write(blurredImage, "png", new File("blurred_single.png"));

        System.out.println("Blurred image saved as blurred_single.png.");
    }

    public static void blurImage(int[] src, int[] dst, int width, int height) {
        int sidePixels = (BLUR_WIDTH - 1) / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float rt = 0, gt = 0, bt = 0;

                // Sum up the color values of surrounding pixels
                for (int ky = -sidePixels; ky <= sidePixels; ky++) {
                    for (int kx = -sidePixels; kx <= sidePixels; kx++) {
                        int pixelX = Math.min(Math.max(x + kx, 0), width - 1);
                        int pixelY = Math.min(Math.max(y + ky, 0), height - 1);
                        int pixel = src[pixelY * width + pixelX];

                        rt += (float) ((pixel & 0x00ff0000) >> 16) / (BLUR_WIDTH * BLUR_WIDTH);
                        gt += (float) ((pixel & 0x0000ff00) >> 8) / (BLUR_WIDTH * BLUR_WIDTH);
                        bt += (float) ((pixel & 0x000000ff)) / (BLUR_WIDTH * BLUR_WIDTH);
                    }
                }

                // Reassemble destination pixel
                int dpixel = (0xff000000) |
                        (((int) rt) << 16) |
                        (((int) gt) << 8) |
                        (((int) bt));
                dst[y * width + x] = dpixel;
            }
        }
    }
}
