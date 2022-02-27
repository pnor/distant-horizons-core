package com.seibel.lod.core.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public final class GLMessageOutputStream extends OutputStream
{
	final Consumer<GLMessage> func;
	final GLMessage.Builder builder;
	
	
	private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	public GLMessageOutputStream(Consumer<GLMessage> func, GLMessage.Builder builder)
	{
		this.func = func;
		this.builder = builder;
	}
	
	@Override
	public void write(int b)
	{
		buffer.write(b);
		if (b=='\n') flush();
	}
	
	@Override
	public void flush()
	{
		String str = buffer.toString();
		GLMessage msg = builder.add(str);
		if (msg != null) func.accept(msg);
		buffer.reset();
	}
	
	@Override
	public void close() throws IOException
	{
		flush();
		buffer.close();
	}
}
