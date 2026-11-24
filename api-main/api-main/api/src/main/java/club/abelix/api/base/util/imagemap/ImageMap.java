package club.abelix.api.base.util.imagemap;

import club.abelix.api.FalconAPI;
import club.abelix.api.nms.entity.FakeEntityApi;
import club.abelix.api.nms.entity.type.object.FakeEntityItemFrame;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;

import java.awt.*;
import java.awt.image.BufferedImage;

public class ImageMap {

    private FakeEntityItemFrame[][] frames;
    private BufferedImage image;

    public ImageMap(Location loc, BlockFace face, BufferedImage image) {
        this(loc, face, image, 1);
    }

    /**
     * @param loc   Локация левого верхнего угла
     * @param face  Направление лица рамки (противоположно взгляду на рамку со стороны игрока)
     * @param image картинка
     */
    public ImageMap(Location loc, BlockFace face, BufferedImage image, double scale) {
        Location loc1 = loc.clone();

        image = resize(image, scale);

        this.image = image;

        int height = image.getHeight() / 128;
        int width = image.getWidth() / 128;

        this.frames = new FakeEntityItemFrame[width][height];

        loc = loc.clone();

        BlockFace build = getRightSide(face);

        FakeEntityApi fakeEntityApi = FalconAPI.getNmsApi().getFakeEntityApi();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                frames[x][y] = fakeEntityApi.createItemFrame(loc, face);
                frames[x][y].update();

                frames[x][y].setPublicStatus(true);

                loc.add(build.getModX(), 0, build.getModZ());
            }

            loc.setX(loc1.getX());
            loc.setZ(loc1.getZ());
            loc.add(0, -1, 0);
        }

        setImage(image);
    }

    private static BlockFace getRightSide(BlockFace face) {
        return switch (face) {
            case SOUTH -> BlockFace.EAST;
            case EAST -> BlockFace.NORTH;
            case NORTH -> BlockFace.WEST;
            case WEST -> BlockFace.SOUTH;
            default -> throw new IllegalStateException("Invalid face direction");
        };
    }

    private static BufferedImage resize(BufferedImage image, int width, int height) {
        BufferedImage scale = new BufferedImage(width, height, image.getType());
        Graphics2D graphics = scale.createGraphics();
        graphics.drawImage(image, 0, 0, scale.getWidth(), scale.getHeight(), null);
        graphics.dispose();

        return scale;
    }

    private static BufferedImage resize(BufferedImage image, double scale) {
        if (scale == 1) {
            return image;
        }

        Image scaled = image.getScaledInstance((int) Math.ceil(image.getWidth() * scale), (int) Math.ceil(image.getHeight() * scale), Image.SCALE_SMOOTH);

        BufferedImage other = new BufferedImage(scaled.getWidth(null), scaled.getHeight(null), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = other.createGraphics();
        graphics.drawImage(scaled, 0, 0, null);
        graphics.dispose();

        return other;
    }

    public void setImage(BufferedImage image) {
        if (image.getWidth() % 128 != 0 || image.getHeight() % 128 != 0) {
            int xSections = image.getWidth() / 128;
            int ySections = image.getHeight() / 128;

            image = resize(image, xSections * 128, ySections * 128);
        }

        this.image = image;

        update();
    }

    public void update() {
        int width = image.getWidth();
        int height = image.getHeight();

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int xs = x / 128;
                int ys = y / 128;
                int rgb = image.getRGB(x, y);

                FakeEntityItemFrame frame = frames[xs][ys];
                frame.getFakeMap().setPixel(x % 128, y % 128, rgb);

                if (x % 128 == 127 && y % 128 == 127) {
                    frame.update();
                }
            }
        }
    }

    public void clear() {
        this.frames = null;
        this.image = null;
    }
}
