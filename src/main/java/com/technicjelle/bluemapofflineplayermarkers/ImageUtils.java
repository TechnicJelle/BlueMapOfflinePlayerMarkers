package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class ImageUtils {

	/**
	 * Converts an image to greyscale.
	 *
	 * @param image The image to modify.
	 */
	public static void Recolour(@NotNull BufferedImage image) {
		for (int y = 0; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y);
				int red = (rgb >> 16) & 0x0ff;
				int green = (rgb >> 8) & 0x0ff;
				int blue = (rgb) & 0x0ff;

				//https://tannerhelland.com/2011/10/01/grayscale-image-algorithm-vb6.html
//				int grey = (red + green + blue) / 3; //average
				int grey = NumberConversions.round(red * 0.3 + green * 0.59 + blue * 0.11); //luma
				Color c = new Color(grey, grey, grey);
				image.setRGB(x, y, c.getRGB());
			}
		}
	}

	/**
	 * Copies an image to a new one with new dimensions.
	 * <p>
	 * Scales using Nearest Neighbour interpolation. (Pixel-art)
	 *
	 * @param image The image to copy.
	 * @param newW  The new width of the image.
	 * @param newH  The new height of the image.
	 * @return A new, resized image.
	 */
	public static @NotNull BufferedImage Resize(@NotNull BufferedImage image, int newW, int newH) {
		Image scaled = image.getScaledInstance(newW, newH, Image.SCALE_FAST);
		BufferedImage result = new BufferedImage(newW, newH, BufferedImage.TYPE_BYTE_GRAY);

		Graphics2D g2d = result.createGraphics();
		g2d.drawImage(scaled, 0, 0, null);
		g2d.dispose();

		return result;
	}

	/**
	 * Gets the player image that BlueMap itself is using.
	 *
	 * @param player The player to get the image of.
	 * @param api    The BlueMapAPI to use.
	 * @return The image of the player or {@code null} if an error occurred.
	 */
	public static @Nullable BufferedImage GetBImgFromAPI(@NotNull OfflinePlayer player, @NotNull BlueMapAPI api) {
		BufferedImage result;
		File f = new File(api.getWebApp().getWebRoot() + "/assets/playerheads/" + player.getUniqueId() + ".png");
		if (f.exists()) {
			try {
				result = ImageIO.read(f);
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} else {
			Main.logger.warning("Marker for " + player.getName() + " couldn't be added!" + (Main.config.verboseErrors ? "" : " (config: verboseErrors)"));
			if (Main.config.verboseErrors) {
				Main.logger.warning(" Couldn't find the playerhead image file in BlueMap's resources");
				Main.logger.warning(" This is likely due to the fact that BlueMap was installed after they last logged off");
				Main.logger.warning(" Falling back to a Steve skin...");
			}
			try {
				result = ImageIO.read(new File(api.getWebApp().getWebRoot() + "/assets/steve.png"));
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}
		return result;
	}

	/**
	 * Gets a player image from an external API instead of getting it from BlueMap.
	 * <p>
	 * (API designated in the config)
	 *
	 * @param player The player to get the image of.
	 * @return The image of the player or {@code null} if an error occurred.
	 */
	public static @Nullable BufferedImage GetBImgFromURL(@NotNull OfflinePlayer player) {
		BufferedImage result;
		try {
			String url = Main.config.skinURL
					.replace("{UUID}", player.getUniqueId().toString())
					.replace("{USERNAME}", player.getName())
					.replace("{NAME}", player.getName());
			URL imageUrl = new URL(url);
			try {
				InputStream in = imageUrl.openStream();
				result = ImageIO.read(in);
				in.close();
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
		return result;
	}
}
