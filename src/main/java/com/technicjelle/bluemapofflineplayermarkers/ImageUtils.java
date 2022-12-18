package com.technicjelle.bluemapofflineplayermarkers;

import de.bluecolored.bluemap.api.AssetStorage;
import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
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
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;

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
				int alpha = (rgb >> 24) & 0xff;
				int red = (rgb >> 16) & 0xff;
				int green = (rgb >> 8) & 0xff;
				int blue = (rgb) & 0xff;

				//https://tannerhelland.com/2011/10/01/grayscale-image-algorithm-vb6.html
//				int grey = (red + green + blue) / 3; //average
				int grey = NumberConversions.round(red * 0.3 + green * 0.59 + blue * 0.11); //luma
				image.setRGB(x, y, alpha << 24 | grey << 16 | grey << 8 | grey);
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
	 * @param assetStorage The AssetStorage to load the image from.
	 * @return The image of the player or {@code null} if an error occurred.
	 */
	public static @Nullable BufferedImage GetBImgFromAPI(@NotNull OfflinePlayer player, @NotNull AssetStorage assetStorage) {
		try {
			Optional<InputStream> optIn = assetStorage.readAsset("playerheads/" + player.getUniqueId() + ".png");
			if (optIn.isPresent()) {
				try (InputStream in = optIn.get()) {
					return ImageIO.read(in);
				}
			}
		} catch (IOException ex) {
			Main.logger.log(Level.SEVERE, "Failed to load playerhead image from BlueMaps AssetStorage!", ex);
		}

		return null;
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
			String url = Main.config.skinURL.toString()
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
