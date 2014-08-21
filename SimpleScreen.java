package com.mrmiguu;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.IndexOutOfBoundsException;
import java.util.List;
import java.util.ArrayList;
import javax.imageio.ImageIO;

public final class SimpleScreen {
  public final int
    width,
    height,
    resolutionBit;

  private static final int
    DEFAULT_WIDTH = 1024,
    DEFAULT_HEIGHT = 576,
    DEFAULT_RESOLUTION_BIT = 0, // 2^0 = 1, value * 1 equals value << 0
    DEFAULT_BUFFER_COUNT = 2,
    DEFAULT_IMAGE_FILE_COUNT = 128,
    DEFAULT_PICTURE_COUNT = 512,
    DEFAULT_SURFACE_COUNT = 16;
  private static final String
    DEFAULT_TITLE = "";

  private final BufferStrategy
    bufferStrategy;
  private final Canvas
    canvas;
  private final Frame
    frame;
  private final ImageFile[]
    imageFiles;
  private final Picture[]
    pictures;
  private final Surface[]
    surfaces;

  private Graphics
    graphics;
  //````````````````````````````````````````````````````````````````````````````````````````````````

  private static <T> int getOpenIndex(final T[] array) {
    for (int i = array.length; --i >= 0;) {
      if (array[i] != null) continue; // we assume we'll be moving on (correct guess = faster)
      else return i;
    }

    return -1; // no index found
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public SimpleScreen(final String title,
      final Dimension size,
      final int resolution,
      final int bufferCount,
      final int imageFileCount,
      final int pictureCount,
      final int surfaceCount) {

    final Dimension
      size_ = (size == null ? new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT) : size);

    width = size_.width;
    height = size_.height;
    resolutionBit = (resolution == -1
      ? DEFAULT_RESOLUTION_BIT
      : (int)(Math.log(resolution) / Math.log(2)));

    (canvas = new Canvas()).setSize(width, height);

    frame = new Frame(title == null ? DEFAULT_TITLE : title);
    frame.add(canvas);
    frame.setResizable(false);
    frame.pack();
    frame.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        System.exit(0);
      }
    });

    canvas.setIgnoreRepaint(true);
    canvas.createBufferStrategy(bufferCount == -1 ? DEFAULT_BUFFER_COUNT : bufferCount);

    bufferStrategy = canvas.getBufferStrategy();

    frame.setLocationRelativeTo(null);
    frame.setVisible(true);

    imageFiles = new ImageFile[imageFileCount == -1 ? DEFAULT_IMAGE_FILE_COUNT : imageFileCount];
    pictures = new Picture[pictureCount == -1 ? DEFAULT_PICTURE_COUNT : pictureCount];
    surfaces = new Surface[surfaceCount == -1 ? DEFAULT_SURFACE_COUNT : surfaceCount];
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void setTitle(final String title) {
    frame.setTitle(title);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public Point getMouseLocation() {
    return new Point(
      (int)(MouseInfo.getPointerInfo().getLocation().getX() -
            canvas.getLocationOnScreen().getX()) >> resolutionBit,
      (int)(MouseInfo.getPointerInfo().getLocation().getY() -
            canvas.getLocationOnScreen().getY()) >> resolutionBit);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void beginDrawing() {
    graphics = bufferStrategy.getDrawGraphics();
    graphics.clearRect(0, 0, width, height);
    graphics.setClip(0, 0, width, height);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void drawSurface(final int index)
      throws IndexOutOfBoundsException {

    graphics.drawImage(
      surfaces[index].image,
      surfaces[index].location.x << resolutionBit,
      surfaces[index].location.y << resolutionBit,
      surfaces[index].image.getWidth() << resolutionBit,
      surfaces[index].image.getHeight() << resolutionBit,
      null);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void drawPicture(final int index)
      throws IndexOutOfBoundsException {

    graphics.drawImage(
      pictures[index].image,
      (pictures[index].location.x + pictures[index].xOffset) << resolutionBit,
      (pictures[index].location.y + pictures[index].yOffset) << resolutionBit,
      pictures[index].image.getWidth() << resolutionBit,
      pictures[index].image.getHeight() << resolutionBit,
      null);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void endDrawing() {
    graphics.dispose();
    bufferStrategy.show();
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int loadImageFile(final String path)
      throws IOException {

    final int
      index = getOpenIndex(imageFiles);
    imageFiles[index] = new ImageFile(path);

    return index;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int createPicture(final int imageFileIndex,
      final Point location,
      final Rectangle frame,
      final Point offset)
          throws IndexOutOfBoundsException {

    final int
      index = getOpenIndex(pictures);
    pictures[index] = new Picture(imageFileIndex, location, frame, offset);

    return index;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int createSurface(final Point location,
      final Dimension size) {

    final int
      index = getOpenIndex(surfaces);
    surfaces[index] = new Surface(location, size);

    return index;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void setPictureLocation(final int index,
      final Point location)
          throws IndexOutOfBoundsException {

    pictures[index].location = location;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int imageFileCount() {
    return imageFiles.length;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int pictureCount() {
    return pictures.length;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void addPictureToSurface(final int pictureIndex,
      final int surfaceIndex)
          throws IndexOutOfBoundsException {

    if (surfaces[surfaceIndex] != null) {
      surfaces[surfaceIndex].addPicture(pictureIndex);
      pictures[pictureIndex] = null;
    }
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void dispose() {
    for (Picture p : pictures) if (p != null) p.dispose();
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  /**
   * The literal image files loaded into memory from a location.
   */
  private final class ImageFile {
    public final BufferedImage
      image;
    public final String
      path;
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public ImageFile(final String path)
        throws IOException {

      image = ImageIO.read(new File(this.path = path));
    }
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  /**
   * The actual image drawn to the screen (there should be as few of these as possible).
   */
  private final class Picture {
    public final int
      xOffset,
      yOffset;
    public final BufferedImage
      image;

    public Point
      location; // references an ever-changing point
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public Picture(final int imageFileIndex,
        final Point location,
        final Rectangle frame,
        final Point offset)
            throws IndexOutOfBoundsException {

      this.location = (location == null ? new Point() : location);

      final Rectangle
        frame_ = (frame == null
          ? new Rectangle(
            0,
            0,
            imageFiles[imageFileIndex].image.getWidth(),
            imageFiles[imageFileIndex].image.getHeight())
          : frame);

      image = new BufferedImage(frame_.width, frame_.height, BufferedImage.TYPE_INT_ARGB);

      image.getGraphics().drawImage(
        imageFiles[imageFileIndex].image,
        0,
        0,
        image.getWidth(),
        image.getHeight(),
        frame_.x,
        frame_.y,
        frame_.x + frame_.width,
        frame_.y + frame_.height,
        null);

      final Point
        offset_ = (offset == null ? new Point() : offset);

      xOffset = offset_.x;
      yOffset = offset_.y;
    }
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public void dispose() {
      location = null;
    }
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  private final class Surface {
    public final BufferedImage
      image;
    public final int
      width,
      height;

    public Point
      location;
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public Surface(final Point location,
        final Dimension size) {

      this.location = (location == null ? new Point() : location);

      final Dimension
        size_ = (size == null
          ? new Dimension(
            SimpleScreen.this.width >> resolutionBit,
            SimpleScreen.this.height >> resolutionBit)
          : size);
      width = size_.width;
      height = size_.height;
      image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public void addPicture(final int index) {
      if (pictures[index] != null) {
        image.getGraphics().drawImage(
          pictures[index].image,
          pictures[index].location.x,
          pictures[index].location.y,
          null);

        pictures[index] = null;
      }
    }
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public void dispose() {
      location = null;
    }
  }
}