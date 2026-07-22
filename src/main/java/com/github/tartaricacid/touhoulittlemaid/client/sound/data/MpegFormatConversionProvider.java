/*
 * MpegFormatConversionProvider.
 *
 * JavaZOOM：mp3spi@javazoom.net http://www.javazoom.net
 *
 * ------------------------------------------------------------------------------- 本程序是免费软件；您可以根据自由软件基金会发布的 GNU 库通用公共许可证的条款重新分发和/或修改它；许可证的版本 2，或（由您选择）任何更高版本。
 *
 * 分发此程序是希望它有用，但是 WITHOUT ANY WARRANTY;甚至没有 MERCHANTABILITY 或 FITNESS FOR A PARTICULAR PURPOSE 的默示保证。  有关更多详细信息，请参阅 GNU 图书馆通用公共许可证。
 *
 * 您应该随本程序一起收到一份 GNU 图书馆通用公共许可证的副本；如果没有，请写信给 Free Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA。 --------------------------------------------------------------------------------------
 */

package com.github.tartaricacid.touhoulittlemaid.client.sound.data;

import javazoom.spi.mpeg.sampled.convert.DecodedMpegAudioInputStream;
import javazoom.spi.mpeg.sampled.file.MpegEncoding;
import org.tritonus.share.TDebug;
import org.tritonus.share.sampled.Encodings;
import org.tritonus.share.sampled.convert.TEncodingFormatConversionProvider;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.util.Arrays;

public class MpegFormatConversionProvider extends TEncodingFormatConversionProvider {
    private static final AudioFormat.Encoding MPEG1L3 = Encodings.getEncoding("MPEG1L3");
    private static final AudioFormat.Encoding PCM_SIGNED = Encodings.getEncoding("PCM_SIGNED");

    private static final AudioFormat[] INPUT_FORMATS =
            {
                    // 单声道
                    new AudioFormat(MPEG1L3, -1.0F, -1, 1, -1, -1.0F, false),
                    new AudioFormat(MPEG1L3, -1.0F, -1, 1, -1, -1.0F, true),
                    // 立体声
                    new AudioFormat(MPEG1L3, -1.0F, -1, 2, -1, -1.0F, false),
                    new AudioFormat(MPEG1L3, -1.0F, -1, 2, -1, -1.0F, true),
            };


    private static final AudioFormat[] OUTPUT_FORMATS =
            {
                    // 单声道，16 位有符号
                    new AudioFormat(PCM_SIGNED, -1.0F, 16, 1, 2, -1.0F, false),
                    new AudioFormat(PCM_SIGNED, -1.0F, 16, 1, 2, -1.0F, true),
                    // 立体声，16 位有符号
                    new AudioFormat(PCM_SIGNED, -1.0F, 16, 2, 4, -1.0F, false),
                    new AudioFormat(PCM_SIGNED, -1.0F, 16, 2, 4, -1.0F, true),
            };

    /**
     * 构造函数。
     */
    public MpegFormatConversionProvider() {
        super(Arrays.asList(INPUT_FORMATS), Arrays.asList(OUTPUT_FORMATS));
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider()");
        }
    }

    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider.getAudioInputStream(AudioFormat targetFormat, AudioInputStream audioInputStream):");
        }
        return new DecodedMpegAudioInputStream(targetFormat, audioInputStream);
    }

    /**
     * 添加对 FrameRate 或 FrameSize 不为空的任何 MpegEncoding 源的转换支持。
     *
     * @param targetFormat
     * @param sourceFormat
     * @return
     */
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (TDebug.TraceAudioConverter) {
            TDebug.out(">MpegFormatConversionProvider.isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat):");
            TDebug.out("checking if conversion possible");
            TDebug.out("from: " + sourceFormat);
            TDebug.out("to: " + targetFormat);
        }

        boolean conversion = super.isConversionSupported(targetFormat, sourceFormat);
        if (!conversion) {
            AudioFormat.Encoding enc = sourceFormat.getEncoding();
            if (enc instanceof MpegEncoding) {
                if ((sourceFormat.getFrameRate() != AudioSystem.NOT_SPECIFIED) || (sourceFormat.getFrameSize() != AudioSystem.NOT_SPECIFIED)) {
                    conversion = true;
                }
            }
        }
        return conversion;
    }
}
