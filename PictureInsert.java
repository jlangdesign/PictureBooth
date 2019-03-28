import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Superimposes the image on the postcard template.
 * 
 * Original code (Main.java) from www.java2s.com:
 * http://www.java2s.com/Tutorials/Java/Graphics_How_to/Image/Merge_an_image_over_another_in_the_specified_position_and_save_it_as_a_new_image.htm
 */
public class PictureInsert {
	
	/** Photo width in px */
	public static final int PHOTO_WIDTH = 1388;
	/** Photo height in px */
	public static final int PHOTO_HEIGHT = 1418;
	
	/**
	 * Inserts the image in the rectangle on the top half of the postcard. The image is centered.
	 * If the image is not the correct size (larger or smaller), it will scale and crop the photo
	 * to the correct size (210mm x 148.5mm or 2480px x 1754px).
	 * It uses BufferedImage.getScaledInstance() to scale and BufferedImage.getSubImage() to crop.
	 * 
	 * @param args
	 * 			command line arguments
	 * @throws IOException
	 * 			if there is an issue processing the file
	 * @throws IllegalArgumentException
	 * 			if the photo size is wrong
	 */
	public static void main(String[] args) throws IOException {
		// Does the program need a command line argument (the filename) for the image BufferedImage?
		// We can have a program that names the uploaded photo "photo-input.jpg"
		BufferedImage template = ImageIO.read(new File("template-side-1.png"));
		BufferedImage image = ImageIO.read(new File("photo-input.jpg"));
		Graphics2D g = template.createGraphics();
		
		// NOTE: Multi-layered conditional below may buggy! Need to test on different sizes!
		// If not correct size, need to scale and crop photo before insertion:
		if (image.getHeight() != PHOTO_HEIGHT && image.getWidth() != PHOTO_WIDTH) {
			// Scale, crop, and update image
			image = scaleAndCrop(image);
		}
		
		// AssertTrue: image is correct size (throw exception if not somehow)
		if (image.getHeight() != PHOTO_HEIGHT && image.getWidth() != PHOTO_WIDTH) {
			throw new IllegalArgumentException("Wrong size");
		}
		
		// image = makeRoundedCorner(image, 12);
		
		// Places the image in the top half of the card
		// (x,y) set to (0,0) so it will print starting from the top-left corner
		// Last parameter is an ImageObserver, which we shouldn't need
		g.drawImage(image, 0, 0, null);
		
		// Export result to new image file (which can be put into a PDF for printing by a different program)
		ImageIO.write(template, "jpeg", new File("side1.jpeg"));
		
		g.dispose();
		
		// Read result back in to manipulate
		// Horizontal image will be rotated and put on top of the rotated template, which serves as a canvas
		image = ImageIO.read(new File("side1.jpeg"));
		template = ImageIO.read(new File("template-side-1-rotated.png"));
		g = template.createGraphics();
		
		// Rotate postcard - see post by "Manitobahhh" in "Java: Rotating Images":
		// https://stackoverflow.com/questions/8639567/java-rotating-images
		AffineTransform backup = g.getTransform();
		AffineTransform a = AffineTransform.getRotateInstance(3 * Math.PI /2,
				image.getWidth() / 2, image.getHeight() / 2);
		g.setTransform(a);
		g.drawImage(image, -PHOTO_HEIGHT / 2 + 30, -PHOTO_WIDTH / 2 + 15, null);
		g.setTransform(backup);
		
		g.dispose();
		
		// HAS TO BE IN PNG TO WORK (JPG OUTPUTS BLACK RECTANGLE)
		ImageIO.write(template, "png", new File("side1-rotated.png"));
		
		// Place frame on top of picture and write to file again
		image = ImageIO.read(new File("frame.png"));
		template = ImageIO.read(new File("side1-rotated.png"));
		g = template.createGraphics();
		g.drawImage(image, 0, PHOTO_WIDTH, null);
		g.dispose();
		ImageIO.write(template, "png", new File("side1-rotated.png"));
	}
	
	/**
	 * Scales and crops the image if it's the wrong size.
	 * You cannot cast an Image to a BufferImage (it will generate a ClassCastException), so you must
	 * create an Image with getScaledInstance(), update newImage with the Image's width and height, and
	 * draw the Image via Graphics.drawImage().
	 * 
	 * See answer by "Hovercraft Full Of Eels" on the "How to get scaled instance of a bufferedImage" post on Stack Overflow:
	 * https://stackoverflow.com/questions/19506927/how-to-get-scaled-instance-of-a-bufferedimage
	 * See answer by "Ozzy" in "Java image resize, maintain aspect ratio" post on Stack Overflow:
	 * https://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio
	 * 
	 * @param image
	 * 			image to crop
	 * @return newImage
	 */
	private static BufferedImage scaleAndCrop(BufferedImage image) {
		BufferedImage newImage = null;
		Image im = null;

		int startW = image.getWidth();
		int startH = image.getHeight();
		int endW = startW;
		int endH = startH;
		int excessW = 0;
		int excessH = 0;
		
		// check if width needs to be scaled
		if (startW != PHOTO_WIDTH) {
			// scale width to fit
			endW = PHOTO_WIDTH;
			// scale height with same ratio
			endH = (endW * startH) / startW;
		}
		
		// check if height needs to be scaled
		if (endH < PHOTO_HEIGHT) {
			// scale height to fit
			endH = PHOTO_HEIGHT;
			// scale width with same ratio
			endW = (endH * startW) / startH;
		}
		
		// Get scaled version and convert Image to BufferedImage
		im = image.getScaledInstance(endW, endH, Image.SCALE_DEFAULT);
		newImage = convert(im);
		
		// Calculate excess width and height that needs to be trimmed off
		if (newImage.getWidth(null) >= PHOTO_WIDTH) {
			excessW = (newImage.getWidth(null) - PHOTO_WIDTH) / 2;
		} else {
			excessW = (PHOTO_WIDTH - newImage.getWidth(null)) / 2;
		}
		
		if (newImage.getHeight(null) >= PHOTO_HEIGHT) {
			excessH = (newImage.getHeight(null) - PHOTO_HEIGHT) / 2;
		} else {
			excessH = (PHOTO_HEIGHT - newImage.getHeight(null)) / 2;
		}
		
		// Get subimage to get a cropped version of the photo
		newImage = newImage.getSubimage(excessW, excessH, PHOTO_WIDTH, PHOTO_HEIGHT);
		
		return newImage;
	}
	
	/**
	 * Helps convert an Image (ToolkitImage) object to a BufferedImage.
	 * 
	 * See answer by "Hovercraft Full Of Eels" on the "How to get scaled instance of a bufferedImage" post on Stack Overflow:
	 * https://stackoverflow.com/questions/19506927/how-to-get-scaled-instance-of-a-bufferedimage
	 * 
	 * @param i
	 * 			Image object
	 * @return BufferedImage generated by Image
	 */
	private static BufferedImage convert(Image i) {
		int width = i.getWidth(null);
		int height = i.getHeight(null);
		
		BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		
		Graphics g = bi.getGraphics();
		
		g.drawImage(i, 0, 0, null);
		g.dispose();
		
		return bi;
	}
	
	/**
	 * https://stackoverflow.com/questions/7603400/how-to-make-a-rounded-corner-image-in-java
	 * 
	 * @param image
	 * @param cornerRadius
	 * @return
	 */
	public static BufferedImage makeRoundedCorner(BufferedImage image, int cornerRadius) {
	    int w = image.getWidth();
	    int h = image.getHeight();
	    BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

	    Graphics2D g2 = output.createGraphics();

	    // This is what we want, but it only does hard-clipping, i.e. aliasing
	    // g2.setClip(new RoundRectangle2D ...)

	    // so instead fake soft-clipping by first drawing the desired clip shape
	    // in fully opaque white with antialiasing enabled...
	    g2.setComposite(AlphaComposite.Src);
	    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	    g2.setColor(Color.WHITE);
	    g2.fill(new RoundRectangle2D.Float(0, 0, w, h, cornerRadius, cornerRadius));

	    // ... then compositing the image on top,
	    // using the white shape from above as alpha source
	    g2.setComposite(AlphaComposite.SrcAtop);
	    g2.drawImage(image, 0, 0, null);

	    g2.dispose();

	    return output;
	}
}
