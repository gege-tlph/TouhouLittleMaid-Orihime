package com.github.tartaricacid.touhoulittlemaid.api.client.decoder;


import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

/**
 * Class GifDecoder - Decodes a GIF file into one or more frames.
 * <p>
 * Example:
 *
 * <pre>
 * {@code
 *    GifDecoder d = new GifDecoder();
 *    d.read("sample.gif");
 *    int n = d.getFrameCount();
 *    for (int i = 0; i < n; i++) {
 *       BufferedImage frame = d.getFrame(i);  // frame i
 *       int t = d.getDelay(i);  // display duration of frame in milliseconds
 *       // do something with frame
 *    }
 * }
 * </pre>
 * No copyright asserted on the source code of this class.  May be used for
 * any purpose, however, refer to the Unisys LZW patent for any additional
 * restrictions.  Please forward any corrections to questions at fmsware.com.
 *
 * @author Kevin Weiner, FM Software; LZW decoder adapted from John Cristy's ImageMagick.
 * @version 1.03 November 2003
 */
@SuppressWarnings("all")
public class GifDecoder {

    /**
     * 文件读取状态：无错误。
     */
    public static final int STATUS_OK = 0;

    /**
     * 文件读取状态：解码文件时出错（可能部分解码）
     */
    public static final int STATUS_FORMAT_ERROR = 1;

    /**
     * 文件读取状态：无法开源。
     */
    public static final int STATUS_OPEN_ERROR = 2;

    protected BufferedInputStream in;
    protected int status;

    protected int width; // 全图像宽度
    protected int height; // 完整图像高度
    protected boolean gctFlag; // 使用的全局颜色表
    protected int gctSize; // 全局颜色表的大小
    protected int loopCount = 1; // 迭代； 0 = 永远重复

    protected int[] gct; // 全局颜色表
    protected int[] lct; // 局部颜色表
    protected int[] act; // 活动颜色表

    protected int bgIndex; // 背景颜色索引
    protected int bgColor; // 背景颜色
    protected int lastBgColor; // 之前的背景颜色
    protected int pixelAspect; // 像素长宽比

    protected boolean lctFlag; // 局部颜色表标志
    protected boolean interlace; // 交错标志
    protected int lctSize; // 局部颜色表大小

    protected int ix, iy, iw, ih; // 当前图像矩形
    protected Rectangle lastRect; // 最后一个图像矩形
    protected BufferedImage image; // 当前帧
    protected BufferedImage lastImage; // 前一帧

    protected byte[] block = new byte[256]; // 当前数据块
    protected int blockSize = 0; // 块大小

    // 最后的图形控制扩展信息
    protected int dispose = 0;

    protected int lastDispose = 0;
    protected boolean transparency = false; // 使用透明颜色
    protected int delay = 0; // 延迟（以毫秒为单位）
    protected int transIndex; // 透明色指数

    protected static final int MaxStackSize = 4096;
    // 最大解码器像素堆栈大小

    // LZW 解码器工作数组
    protected short[] prefix;
    protected byte[] suffix;
    protected byte[] pixelStack;
    protected byte[] pixels;

    protected ArrayList frames; // 从当前文件读取的帧
    protected int frameCount;

    static class GifFrame {
        public GifFrame(BufferedImage im, int del) {
            image = im;
            delay = del;
        }

        public BufferedImage image;
        public int delay;
    }

    /**
     * 获取指定帧的显示持续时间。
     *
     * @param n 帧的 int 索引
     * @return 延迟（以毫秒为单位）
     */
    public int getDelay(int n) {

        delay = -1;
        if ((n >= 0) && (n < frameCount)) {
            delay = ((GifFrame) frames.get(n)).delay;
        }
        return delay;
    }

    /**
     * 获取从文件中读取的帧数。
     *
     * @return 帧数
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * 获取读取的第一张（或唯一一张）图像。
     *
     * @return BufferedImage 包含第一帧，如果没有则为 null。
     */
    public BufferedImage getImage() {
        return getFrame(0);
    }

    /**
     * 获取“Netscape”迭代计数（如果有）。计数为 0 表示无限重复。
     *
     * @return 如果指定了迭代计数，则为 1。
     */
    public int getLoopCount() {
        return loopCount;
    }

    /**
     * 从当前数据（以及由其处理代码指定的先前帧）创建新的帧图像。
     */
    protected void setPixels() {
        // 将目标图像的像素公开为 int 数组
        int[] dest =
                ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        // 根据最后一张图片的处理代码填写起始图片内容
        if (lastDispose > 0) {
            if (lastDispose == 3) {
                // 使用最后一张之前的图像
                int n = frameCount - 2;
                if (n > 0) {
                    lastImage = getFrame(n - 1);
                } else {
                    lastImage = null;
                }
            }

            if (lastImage != null) {
                int[] prev =
                        ((DataBufferInt) lastImage.getRaster().getDataBuffer()).getData();
                System.arraycopy(prev, 0, dest, 0, width * height);
                // 复制像素

                if (lastDispose == 2) {
                    // 用背景颜色填充最后一个图像的矩形区域
                    Graphics2D g = image.createGraphics();
                    Color c = null;
                    if (transparency) {
                        c = new Color(0, 0, 0, 0);    // 假设背景是透明的
                    } else {
                        c = new Color(lastBgColor); // 使用给定的背景颜色
                    }
                    g.setColor(c);
                    g.setComposite(AlphaComposite.Src); // 替换区域
                    g.fill(lastRect);
                    g.dispose();
                }
            }
        }

        // 将每个源行复制到目标中的适当位置
        int pass = 1;
        int inc = 8;
        int iline = 0;
        for (int i = 0; i < ih; i++) {
            int line = i;
            if (interlace) {
                if (iline >= ih) {
                    pass++;
                    switch (pass) {
                        case 2:
                            iline = 4;
                            break;
                        case 3:
                            iline = 2;
                            inc = 4;
                            break;
                        case 4:
                            iline = 1;
                            inc = 2;
                    }
                }
                line = iline;
                iline += inc;
            }
            line += iy;
            if (line < height) {
                int k = line * width;
                int dx = k + ix; // 目的地行的开头
                int dlim = dx + iw; // 目标行结束
                if ((k + width) < dlim) {
                    dlim = k + width; // 过去目标边缘
                }
                int sx = i * iw; // 源代码中的行首
                while (dx < dlim) {
                    // 映射颜色并插入到目的地
                    int index = ((int) pixels[sx++]) & 0xff;
                    int c = act[index];
                    if (c != 0) {
                        dest[dx] = c;
                    }
                    dx++;
                }
            }
        }
    }

    /**
     * 获取第n帧的图像内容。
     *
     * @return BufferedImage 帧的表示，如果 n 无效则为 null。
     */
    public BufferedImage getFrame(int n) {
        BufferedImage im = null;
        if ((n >= 0) && (n < frameCount)) {
            im = ((GifFrame) frames.get(n)).image;
        }
        return im;
    }

    /**
     * 获取图像大小。
     *
     * @return GIF 图像尺寸
     */
    public Dimension getFrameSize() {
        return new Dimension(width, height);
    }

    /**
     * 从流中读取 GIF 图像
     *
     * @param is BufferedInputStream 包含 GIF 文件。
     * @return 读取状态代码（0 = 无错误）
     */
    public int read(BufferedInputStream is) {
        init();
        if (is != null) {
            in = is;
            readHeader();
            if (!err()) {
                readContents();
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
        } else {
            status = STATUS_OPEN_ERROR;
        }
        try {
            is.close();
        } catch (IOException e) {
        }
        return status;
    }

    /**
     * 从流中读取 GIF 图像
     *
     * @param is InputStream 包含 GIF 文件。
     * @return 读取状态代码（0 = 无错误）
     */
    public int read(InputStream is) {
        init();
        if (is != null) {
            if (!(is instanceof BufferedInputStream))
                is = new BufferedInputStream(is);
            in = (BufferedInputStream) is;
            readHeader();
            if (!err()) {
                readContents();
                if (frameCount < 0) {
                    status = STATUS_FORMAT_ERROR;
                }
            }
        } else {
            status = STATUS_OPEN_ERROR;
        }
        try {
            is.close();
        } catch (IOException e) {
        }
        return status;
    }

    /**
     * 从指定文件/URL源读取GIF文件（如果名称包含“：/”或“文件：”，则假定为URL）
     *
     * @param name 包含源的字符串
     * @return 读取状态代码（0 = 无错误）
     */
    public int read(String name) {
        status = STATUS_OK;
        try {
            name = name.trim().toLowerCase();
            if ((name.indexOf("file:") >= 0) ||
                (name.indexOf(":/") > 0)) {
                URL url = new URL(name);
                in = new BufferedInputStream(url.openStream());
            } else {
                in = new BufferedInputStream(new FileInputStream(name));
            }
            status = read(in);
        } catch (IOException e) {
            status = STATUS_OPEN_ERROR;
        }

        return status;
    }

    /**
     * 将 LZW 图像数据解码为像素数组。改编自约翰·克里斯蒂的ImageMagick。
     */
    protected void decodeImageData() {
        int NullCode = -1;
        int npix = iw * ih;
        int available,
                clear,
                code_mask,
                code_size,
                end_of_information,
                in_code,
                old_code,
                bits,
                code,
                count,
                i,
                datum,
                data_size,
                first,
                top,
                bi,
                pi;

        if ((pixels == null) || (pixels.length < npix)) {
            pixels = new byte[npix]; // 分配新的像素数组
        }
        if (prefix == null) prefix = new short[MaxStackSize];
        if (suffix == null) suffix = new byte[MaxStackSize];
        if (pixelStack == null) pixelStack = new byte[MaxStackSize + 1];

        // 初始化GIF数据流解码器。

        data_size = read();
        clear = 1 << data_size;
        end_of_information = clear + 1;
        available = clear + 2;
        old_code = NullCode;
        code_size = data_size + 1;
        code_mask = (1 << code_size) - 1;
        for (code = 0; code < clear; code++) {
            prefix[code] = 0;
            suffix[code] = (byte) code;
        }

        // 解码 GIF 像素流。

        datum = bits = count = first = top = pi = bi = 0;

        for (i = 0; i < npix; ) {
            if (top == 0) {
                if (bits < code_size) {
                    // 加载字节，直到有足够的位用于代码。
                    if (count == 0) {
                        // 读取一个新的数据块。
                        count = readBlock();
                        if (count <= 0)
                            break;
                        bi = 0;
                    }
                    datum += (((int) block[bi]) & 0xff) << bits;
                    bits += 8;
                    bi++;
                    count--;
                    continue;
                }

                // 获取下一个代码。

                code = datum & code_mask;
                datum >>= code_size;
                bits -= code_size;

                // 解释一下代码

                if ((code > available) || (code == end_of_information))
                    break;
                if (code == clear) {
                    // 重置解码器。
                    code_size = data_size + 1;
                    code_mask = (1 << code_size) - 1;
                    available = clear + 2;
                    old_code = NullCode;
                    continue;
                }
                if (old_code == NullCode) {
                    pixelStack[top++] = suffix[code];
                    old_code = code;
                    first = code;
                    continue;
                }
                in_code = code;
                if (code == available) {
                    pixelStack[top++] = (byte) first;
                    code = old_code;
                }
                while (code > clear) {
                    pixelStack[top++] = suffix[code];
                    code = prefix[code];
                }
                first = ((int) suffix[code]) & 0xff;

                // 将新字符串添加到字符串表中，

                if (available >= MaxStackSize) {
                    pixelStack[top++] = (byte) first;
                    continue;
                }
                pixelStack[top++] = (byte) first;
                prefix[available] = (short) old_code;
                suffix[available] = (byte) first;
                available++;
                if (((available & code_mask) == 0)
                    && (available < MaxStackSize)) {
                    code_size++;
                    code_mask += available;
                }
                old_code = in_code;
            }

            // 从像素堆栈中弹出一个像素。

            top--;
            pixels[pi++] = pixelStack[top];
            i++;
        }

        for (i = pi; i < npix; i++) {
            pixels[i] = 0; // 清除缺失像素
        }

    }

    /**
     * 如果在读取/解码过程中遇到错误，则返回 true
     */
    protected boolean err() {
        return status != STATUS_OK;
    }

    /**
     * 初始化或重新初始化阅读器
     */
    protected void init() {
        status = STATUS_OK;
        frameCount = 0;
        frames = new ArrayList();
        gct = null;
        lct = null;
    }

    /**
     * 从输入流中读取单个字节。
     */
    protected int read() {
        int curByte = 0;
        try {
            curByte = in.read();
        } catch (IOException e) {
            status = STATUS_FORMAT_ERROR;
        }
        return curByte;
    }

    /**
     * 从输入读取下一个可变长度块。
     *
     * @return “缓冲区”中存储的字节数
     */
    protected int readBlock() {
        blockSize = read();
        int n = 0;
        if (blockSize > 0) {
            try {
                int count = 0;
                while (n < blockSize) {
                    count = in.read(block, n, blockSize - n);
                    if (count == -1)
                        break;
                    n += count;
                }
            } catch (IOException e) {
            }

            if (n < blockSize) {
                status = STATUS_FORMAT_ERROR;
            }
        }
        return n;
    }

    /**
     * 将颜色表读取为 256 个 RGB 整数值
     *
     * @param ncolors int 要读取的颜色数
     * @return 包含 256 种颜色的 int 数组（包含完整 alpha 的 ARGB）
     */
    protected int[] readColorTable(int ncolors) {
        int nbytes = 3 * ncolors;
        int[] tab = null;
        byte[] c = new byte[nbytes];
        int n = 0;
        try {
            n = in.read(c);
        } catch (IOException e) {
        }
        if (n < nbytes) {
            status = STATUS_FORMAT_ERROR;
        } else {
            tab = new int[256]; // 避免边界检查的最大尺寸
            int i = 0;
            int j = 0;
            while (i < ncolors) {
                int r = ((int) c[j++]) & 0xff;
                int g = ((int) c[j++]) & 0xff;
                int b = ((int) c[j++]) & 0xff;
                tab[i++] = 0xff000000 | (r << 16) | (g << 8) | b;
            }
        }
        return tab;
    }

    /**
     * 主文件解析器。  读取 GIF 内容块。
     */
    protected void readContents() {
        // 读取 GIF 文件内容块
        boolean done = false;
        while (!(done || err())) {
            int code = read();
            switch (code) {

                case 0x2C: // 图像分离器
                    readImage();
                    break;

                case 0x21: // 延伸
                    code = read();
                    switch (code) {
                        case 0xf9: // 图形控制扩展
                            readGraphicControlExt();
                            break;

                        case 0xff: // 应用扩展
                            readBlock();
                            String app = "";
                            for (int i = 0; i < 11; i++) {
                                app += (char) block[i];
                            }
                            if (app.equals("NETSCAPE2.0")) {
                                readNetscapeExt();
                            } else
                                skip(); // 不在乎
                            break;

                        default: // 无趣的扩展
                            skip();
                    }
                    break;

                case 0x3b: // 终结者
                    done = true;
                    break;

                case 0x00: // 坏字节，但继续看看会发生什么
                    break;

                default:
                    status = STATUS_FORMAT_ERROR;
            }
        }
    }

    /**
     * 读取图形控制扩展值
     */
    protected void readGraphicControlExt() {
        read(); // 块大小
        int packed = read(); // 打包字段
        dispose = (packed & 0x1c) >> 2; // 处置方法
        if (dispose == 0) {
            dispose = 1; // 如果可以的话，选择保留旧图像
        }
        transparency = (packed & 1) != 0;
        delay = readShort() * 10; // 延迟（以毫秒为单位）
        transIndex = read(); // 透明色指数
        read(); // 块终止符
    }

    /**
     * 读取GIF文件头信息。
     */
    protected void readHeader() {
        String id = "";
        for (int i = 0; i < 6; i++) {
            id += (char) read();
        }
        if (!id.startsWith("GIF")) {
            status = STATUS_FORMAT_ERROR;
            return;
        }

        readLSD();
        if (gctFlag && !err()) {
            gct = readColorTable(gctSize);
            bgColor = gct[bgIndex];
        }
    }

    /**
     * 读取下一帧图像
     */
    protected void readImage() {
        ix = readShort(); // （子）图像位置和大小
        iy = readShort();
        iw = readShort();
        ih = readShort();

        int packed = read();
        lctFlag = (packed & 0x80) != 0; // 1 - 本地颜色表标志
        interlace = (packed & 0x40) != 0; // 2 - 交错标志 3 - 排序标志 4-5 - 保留
        lctSize = 2 << (packed & 7); // 6-8 - 局部颜色表大小

        if (lctFlag) {
            lct = readColorTable(lctSize); // 读表
            act = lct; // 激活本地表
        } else {
            act = gct; // 使全局表处于活动状态
            if (bgIndex == transIndex)
                bgColor = 0;
        }
        int save = 0;
        if (transparency) {
            save = act[transIndex];
            act[transIndex] = 0; // 如果指定则设置透明颜色
        }

        if (act == null) {
            status = STATUS_FORMAT_ERROR; // 没有定义颜色表
        }

        if (err()) return;

        decodeImageData(); // 解码像素数据
        skip();

        if (err()) return;

        frameCount++;

        // 创建新图像以接收帧数据
        image =
                new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);

        setPixels(); // 将像素数据传输到图像

        frames.add(new GifFrame(image, delay)); // 将图像添加到帧列表

        if (transparency) {
            act[transIndex] = save;
        }
        resetFrame();

    }

    /**
     * 读取逻辑屏幕描述符
     */
    protected void readLSD() {

        // 逻辑屏幕尺寸
        width = readShort();
        height = readShort();

        // 打包字段
        int packed = read();
        gctFlag = (packed & 0x80) != 0; // 1：全局颜色表标志 2-4：颜色分辨率 5：GCT 排序标志
        gctSize = 2 << (packed & 7); // 6-8：GCT尺寸

        bgIndex = read(); // 背景颜色索引
        pixelAspect = read(); // 像素长宽比
    }

    /**
     * 读取 Netscape 扩展以获取迭代计数
     */
    protected void readNetscapeExt() {
        do {
            readBlock();
            if (block[0] == 1) {
                // 循环计数子块
                int b1 = ((int) block[1]) & 0xff;
                int b2 = ((int) block[2]) & 0xff;
                loopCount = (b2 << 8) | b1;
            }
        } while ((blockSize > 0) && !err());
    }

    /**
     * 首先读取下一个 16 位值 LSB
     */
    protected int readShort() {
        // 首先读取 16 位值 LSB
        return read() | (read() << 8);
    }

    /**
     * 重置帧状态以读取下一个图像。
     */
    protected void resetFrame() {
        lastDispose = dispose;
        lastRect = new Rectangle(ix, iy, iw, ih);
        lastImage = image;
        lastBgColor = bgColor;
        int dispose = 0;
        boolean transparency = false;
        int delay = 0;
        lct = null;
    }

    /**
     * 跳过可变长度块直到并包括下一个零长度块。
     */
    protected void skip() {
        do {
            readBlock();
        } while ((blockSize > 0) && !err());
    }
}
