package com.seibel.lod.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public final class ComsumerOutputStream extends OutputStream
{
	final Consumer<String> func;
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
	
	public ComsumerOutputStream(Consumer<String> func) {
		this.func = func;
	}
	@Override
	public void write(int b)
	{
		buffer.write(b);
	}
	
	@Override
	public void write(byte[] b) throws IOException {
		buffer.write(b);
	}
	@Override
	public void write(byte[] b,
	         int off,
	         int len) {
		buffer.write(b, off, len);
	}
	
	@Override
	public void flush()
	{
		String str = buffer.toString();
		buffer.reset();
		func.accept(str);
	}
	
	@Override
	public void close() throws IOException
	{
		buffer.close();
	}
}
