import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/** Usage: java tools/ContactSheet.java <dir> <out.png> [scale] */
public class ContactSheet {
	public static void main(String[] a) throws Exception {
		File dir = new File(a[0]);
		int scale = a.length > 2 ? Integer.parseInt(a[2]) : 6;
		File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
		Arrays.sort(files);
		int cell = 16 * scale + 8, cols = 10, rows = (files.length + cols - 1) / cols;
		BufferedImage out = new BufferedImage(cols * cell, rows * (cell + 14), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = out.createGraphics();
		g.setColor(new Color(0x7f7f7f)); g.fillRect(0, 0, out.getWidth(), out.getHeight());
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g.setFont(new Font("SansSerif", Font.PLAIN, 10));
		for (int i = 0; i < files.length; i++) {
			BufferedImage im = ImageIO.read(files[i]);
			int w = im.getWidth(), h = Math.min(im.getHeight(), w);
			int x = (i % cols) * cell + 4, y = (i / cols) * (cell + 14) + 4;
			g.drawImage(im.getSubimage(0, 0, w, h), x, y, 16 * scale, 16 * scale, null);
			g.setColor(Color.BLACK);
			g.drawString(files[i].getName().replace(".png", ""), x, y + 16 * scale + 12);
		}
		ImageIO.write(out, "png", new File(a[1]));
	}
}
