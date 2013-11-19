package me.timos.busyboxonrails;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import me.timos.br.Logcat;
import android.content.Context;
import android.os.Looper;
import android.text.SpannableStringBuilder;
import android.text.style.CharacterStyle;

import com.stericson.RootTools.RootTools;
import com.stericson.RootTools.execution.CommandCapture;

public class Utility {

	public static boolean checkFileIntegrity(Context context, File f,
			String resPath) {
		try {
			String pkgFile = context.getPackageCodePath();
			ZipFile zFile = null;
			try {
				zFile = new ZipFile(pkgFile);
				ZipEntry entry = zFile.getEntry(resPath);
				return entry.getCrc() == getFileCrc(f);
			} finally {
				zFile.close();
			}
		} catch (IOException e) {
			return false;
		}
	}

	public static void copyStream(InputStream input, OutputStream output)
			throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(input);
		final WritableByteChannel outputChannel = Channels.newChannel(output);
		// copy the channels
		fastChannelCopy(inputChannel, outputChannel);
	}

	public static void doEntry(Context context, ZipOutputStream zout,
			int resId, String dest) throws Exception {
		InputStream in = null;
		try {
			zout.putNextEntry(new ZipEntry(dest));
			copyStream(in = context.getResources().openRawResource(resId), zout);
		} finally {
			zout.closeEntry();
			in.close();
		}
	}

	public static void fastChannelCopy(final ReadableByteChannel src,
			final WritableByteChannel dest) throws IOException {
		final ByteBuffer buffer = ByteBuffer.allocateDirect(1 << 17);
		while (src.read(buffer) != -1) {
			// prepare the buffer to be drained
			buffer.flip();
			// write to the channel, may block
			dest.write(buffer);
			// If partial transfer, shift remainder down
			// If buffer is empty, same as doing clear()
			buffer.compact();
		}
		// EOF will leave buffer in fill state
		buffer.flip();
		// make sure the buffer is fully drained.
		while (buffer.hasRemaining()) {
			dest.write(buffer);
		}
	}

	public static long getFileCrc(File file) {
		CRC32 checkSummer = new CRC32();
		CheckedInputStream cis = null;
		try {
			try {
				cis = new CheckedInputStream(new FileInputStream(file),
						checkSummer);
				byte[] buf = new byte[4096];
				while (cis.read(buf) >= 0) {
				}
				return checkSummer.getValue();
			} finally {
				cis.close();
			}
		} catch (Exception e) {
			return -1;
		}
	}

	public static File installBinary(Context context, String name, int resId,
			String resPath) {
		File filesDir = context.getFilesDir();
		File file = new File(filesDir, name);
		if (file.exists()) {
			try {
				String pkgFile = context.getPackageCodePath();
				ZipFile zFile = null;
				try {
					zFile = new ZipFile(pkgFile);
					ZipEntry entry = zFile.getEntry(resPath);
					if (entry.getCrc() == getFileCrc(file)) {
						return file;
					}
				} finally {
					zFile.close();
				}
				file.delete();
			} catch (IOException e) {
			}
		}
		InputStream ris = context.getResources().openRawResource(resId);
		ReadableByteChannel rc = Channels.newChannel(ris);
		try {
			FileOutputStream fos = new FileOutputStream(file);
			FileChannel fc = fos.getChannel();
			try {
				long size = ris.available();
				for (long pos = 0; pos < size;) {
					pos += fc.transferFrom(rc, pos, size - pos);
				}
			} finally {
				fos.flush();
				fos.getFD().sync();
				fos.close();
			}
			Logcat.d("Write binary " + name);
			return file;
		} catch (Exception e) {
			Logcat.e("Couldn't write " + file, e);
			return null;
		}
	}

	/**
	 * Given either a Spannable String or a regular String and a token, apply
	 * the given CharacterStyle to the span between the tokens, and also remove
	 * tokens.
	 * <p>
	 * For example, {@code setSpanBetweenTokens("Hello ##world##!", "##",
	 * new ForegroundColorSpan(0xFFFF0000));} will return a CharSequence
	 * {@code "Hello world!"} with {@code world} in red.
	 * 
	 * @param text
	 *            The text, with the tokens, to adjust.
	 * @param token
	 *            The token string; there should be at least two instances of
	 *            token in text.
	 * @param cs
	 *            The style to apply to the CharSequence. WARNING: You cannot
	 *            send the same two instances of this parameter, otherwise the
	 *            second call will remove the original span.
	 * @return A Spannable CharSequence with the new style applied.
	 * 
	 * @see http 
	 *      ://developer.android.com/reference/android/text/style/CharacterStyle
	 *      .html
	 */
	public static CharSequence setSpanBetweenTokens(CharSequence text,
			String token, CharacterStyle... cs) {
		// Start and end refer to the points where the span will apply
		int tokenLen = token.length();
		int start = text.toString().indexOf(token) + tokenLen;
		int end = text.toString().indexOf(token, start);

		if (start > -1 && end > -1) {
			// Copy the spannable string to a mutable spannable string
			SpannableStringBuilder ssb = new SpannableStringBuilder(text);
			for (CharacterStyle c : cs) {
				ssb.setSpan(c, start, end, 0);
			}

			// Delete the tokens before and after the span
			ssb.delete(end, end + tokenLen);
			ssb.delete(start - tokenLen, start);

			text = ssb;
		}

		return text;
	}

	public synchronized static String shellExec(String workingDir,
			Integer timeout, String... commands) {
		if (Looper.myLooper() == Looper.getMainLooper()) {
			Logcat.e("Warning execute commands on main thread\n"
					+ Arrays.toString(commands));
		}
		try {
			if (workingDir != null) {
				String[] tmp = new String[commands.length + 1];
				tmp[0] = "cd \"" + workingDir + "\"";
				System.arraycopy(commands, 0, tmp, 1, commands.length);
				commands = tmp;
			}

			final StringBuilder sb = new StringBuilder(4096);

			CommandCapture cc = new CommandCapture(0, timeout == null ? 5000
					: timeout, commands) {
				@Override
				public void commandOutput(int id, String line) {
					sb.append("\n").append(line);
				}
			};

			RootTools.getShell(true).add(cc);
			synchronized (cc) {
				try {
					cc.wait();
				} catch (InterruptedException e) {
				}
			}
			return sb.length() > 0 ? sb.substring(1) : "";
		} catch (Exception e) {
			Logcat.e("Error execute command\n" + Arrays.toString(commands), e);
			return "";
		}
	}
}
