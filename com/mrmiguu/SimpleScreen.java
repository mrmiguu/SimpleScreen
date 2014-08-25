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

public final class SimpleScreen
    implements AutoCloseable {

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
    DEFAULT_SURFACE_COUNT = 16,
    DEFAULT_STATIC_ANIMATION_COUNT = 128,
    DEFAULT_DYNAMIC_ANIMATION_COUNT = 512;
  private static final String
    DEFAULT_TITLE = "SimpleScreen";

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
  private final StaticAnimation[]
    staticAnimations;
  private final DynamicAnimation[]
    dynamicAnimations;
  private final Thread
    animationTimer; // for looping static animations via their intervals

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

  private static <T> boolean isEmpty(final T[] array) {
    for (int i = array.length; --i >= 0;) {
      if (array[i] == null) continue;
      else return false;
    }

    return true;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public SimpleScreen(final String title,
      final Dimension size,
      final int resolution,
      final int bufferCount,
      final int imageFileCount,
      final int pictureCount,
      final int surfaceCount,
      final int staticAnimationCount,
      final int dynamicAnimationCount) {

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
    staticAnimations = new StaticAnimation[staticAnimationCount == -1
      ? DEFAULT_STATIC_ANIMATION_COUNT
      : staticAnimationCount];
    dynamicAnimations = new DynamicAnimation[dynamicAnimationCount == -1
      ? DEFAULT_DYNAMIC_ANIMATION_COUNT
      : dynamicAnimationCount];

    /*
     * Handles the loop for occurring animations
     */
    (animationTimer = new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          for (;;) {
            final long
              time = System.currentTimeMillis();

            for (StaticAnimation s : staticAnimations) {
              if (s != null) {
                if (s.timer < time) {
                  s.frame = (s.frame + 1) % s.images.length;
                  s.timer = time + s.interval;
                }
              }
            }

            animationTimer.sleep(15); // nothing should animate fast than 60 fps
          }
        }
        catch (final InterruptedException e) {
          animationTimer.interrupt();
          e.printStackTrace();
        }
      }
    })).start();
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

  public void drawPicture(final int index)
      throws IndexOutOfBoundsException {

    /*
     * Remove resolution scaling code and scale images when saving them after creating them
     * (all I have to do now is store the pictures in relation to the resolution)
     */
    graphics.drawImage(
      pictures[index].image,
      (pictures[index].location.x + pictures[index].xOffset) << resolutionBit,
      (pictures[index].location.y + pictures[index].yOffset) << resolutionBit,
      null);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void drawSurface(final int index)
      throws IndexOutOfBoundsException {

    /*
     * Remove resolution scaling code and scale images when saving them after creating them
     * (all I have to do now is store the surfaces in relation to the resolution)
     */
    graphics.drawImage(
      surfaces[index].image,
      surfaces[index].location.x << resolutionBit,
      surfaces[index].location.y << resolutionBit,
      null);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void drawDynamicAnimation(final int index)
      throws IndexOutOfBoundsException {

    drawStaticAnimation(dynamicAnimations[index].animations[dynamicAnimations[index].state[0]]);
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void drawStaticAnimation(final int index)
      throws IndexOutOfBoundsException {

    final StaticAnimation
      staticAnim = staticAnimations[index];

    graphics.drawImage(
      staticAnim.images[staticAnim.frame],
      (staticAnim.location.x + staticAnim.xOffset) << resolutionBit,
      (staticAnim.location.y + staticAnim.yOffset) << resolutionBit,
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

  public int createStaticAnimation(final int imageFileIndex,
        final Rectangle[] frames,
        final int interval,
        final Point location,
        final Point offset)
            throws IndexOutOfBoundsException {

    final int
      index = getOpenIndex(staticAnimations);
    staticAnimations[index] = new StaticAnimation(
      imageFileIndex,
      frames,
      interval,
      location,
      offset);

    return index;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public int createDynamicAnimation(final int[] staticAnimIndices,
        final int[] state)
            throws IndexOutOfBoundsException {

    final int
      index = getOpenIndex(dynamicAnimations);
    dynamicAnimations[index] = new DynamicAnimation(staticAnimIndices, state);

    return index;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void setPictureLocation(final int index,
      final Point location)
          throws IndexOutOfBoundsException {

    pictures[index].location = location;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  public void setStaticAnimationLocation(final int index,
      final Point location)
          throws IndexOutOfBoundsException {

    staticAnimations[index].location = location;
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  // public void setDynamicAnimationLocation(final int index,
  //     final Point location)
  //         throws IndexOutOfBoundsException {

  //   dynamicAnimations[index].location = location;
  // }
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
      pictures[pictureIndex].dispose();
      pictures[pictureIndex] = null;
    }
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  @Override
  public void close()
      throws Exception {

    // stop our animations and wait for them to stop
    animationTimer.interrupt();
    animationTimer.join();

    for (int i = imageFiles.length; --i >= 0;) imageFiles[i] = null;
    for (int i = pictures.length; --i >= 0;) {
      if (pictures[i] != null) pictures[i].dispose();
      pictures[i] = null;
    }
    for (int i = surfaces.length; --i >= 0;) {
      if (surfaces[i] != null) surfaces[i].dispose();
      surfaces[i] = null;
    }
    for (int i = staticAnimations.length; --i >= 0;) {
      if (staticAnimations[i] != null) staticAnimations[i].dispose();
      staticAnimations[i] = null;
    }
    for (int i = dynamicAnimations.length; --i >= 0;) dynamicAnimations[i] = null;
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
   * The actual image drawn to the screen or an individual surface.
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

      // image = new BufferedImage(frame_.width, frame_.height, BufferedImage.TYPE_INT_ARGB);

      image = imageFiles[imageFileIndex].image.getSubimage(
        frame_.x,
        frame_.y,
        frame_.width,
        frame_.height);

      // image.getGraphics().drawImage(
      //   imageFiles[imageFileIndex].image,
      //   0,
      //   0,
      //   image.getWidth(),
      //   image.getHeight(),
      //   frame_.x,
      //   frame_.y,
      //   frame_.x + frame_.width,
      //   frame_.y + frame_.height,
      //   null);

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

  /**
   * An open field for drawing non-moving pictures onto.
   */
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
  //````````````````````````````````````````````````````````````````````````````````````````````````

  /**
   * An array of images that change at an interval.
   */
  private final class StaticAnimation {
    public final BufferedImage[]
      images;
    public final int
      interval,
      xOffset,
      yOffset;

    public Point
      location; // references an ever-changing point
    public long
      timer;
    public int
      frame;
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public StaticAnimation(final int imageFileIndex,
        final Rectangle[] frames,
        final int interval,
        final Point location,
        final Point offset)
            throws IndexOutOfBoundsException {

      images = new BufferedImage[frames.length];

      for (int i = 0; i < images.length; ++i) {
        images[i] = imageFiles[imageFileIndex].image.getSubimage(
          frames[i].x,
          frames[i].y,
          frames[i].width,
          frames[i].height);
      }

      this.interval = interval;
      this.location = (location == null ? new Point() : location);

      final Point
        offset_ = (offset == null ? new Point() : offset);
      xOffset = offset_.x;
      yOffset = offset_.y;
    }
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public void dispose() {
      for (int i = images.length; --i >= 0;) images[i] = null;
      location = null;
    }
  }
  //````````````````````````````````````````````````````````````````````````````````````````````````

  /**
   * An array of static animations that change depending on a state reference.
   */
  private final class DynamicAnimation {
    public final int[]
      animations,
      state;

    // public Point
    //   location; // references an ever-changing point
    //``````````````````````````````````````````````````````````````````````````````````````````````

    public DynamicAnimation(final int[] staticAnimIndices,
        final int[] state) {

      animations = staticAnimIndices;
      this.state = state;
    }
  }
}