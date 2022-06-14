package com.seibel.lod.core.objects.a7.io.file;

import java.io.File;
import java.lang.ref.SoftReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.seibel.lod.core.objects.a7.data.LodDataSource;
import com.seibel.lod.core.objects.a7.io.MetaFile;
import com.seibel.lod.core.objects.a7.pos.DhSectionPos;
import com.seibel.lod.core.util.LodUtil;

public class DataMetaFile extends MetaFile {
	AtomicInteger localVersion = new AtomicInteger();
	
	// The '?' type should either be:
	//    SoftReference<LodDataSource>, or
	//    CompletableFuture<LodDataSource>, or
	//    null
	AtomicReference<Object> data = new AtomicReference<Object>(null);
	
	
	public DataMetaFile(File path, DhSectionPos pos) {
		super(path, pos);
		// TODO Auto-generated constructor stub
	}
	
	public boolean isValid(int version) {
		return (localVersion.get() == version);
	}
	
	private CompletableFuture<LodDataSource> readCached(Object obj) {
		// Has file cached in RAM and not freed yet.
		if (obj != null && (obj instanceof SoftReference<?>)) {
			Object inner = ((SoftReference<?>)obj).get();
			if (inner != null) {
				LodUtil.assertTrue(inner instanceof LodDataSource);
				return CompletableFuture.completedFuture((LodDataSource)inner);
			}
		}
		
		//==== Cached file out of scrope. ====
		// Someone is already trying to complete it. so just return the obj.
		if (obj != null && (obj instanceof CompletableFuture<?>)) {
			return (CompletableFuture<LodDataSource>)obj;
		}
		return null;
	}

	// Cause: Generic Type runtime casting cannot safety check it.
	// However, the Union type ensures the 'data' should only contain the listed type. 
	@SuppressWarnings("unchecked")
	public CompletableFuture<LodDataSource> loadOrGetCached(Executor fileReaderThreads) {
		Object obj = data.get();
		
		CompletableFuture<LodDataSource> cached = readCached(obj);
		if (cached != null) return cached;

		CompletableFuture<LodDataSource> future = new CompletableFuture<LodDataSource>();
		
		// Would use faster and non-nesting Compare and exchange. But java 8 doesn't have it! :(
		boolean worked = data.compareAndSet(obj, future);
		if (!worked) return loadOrGetCached(fileReaderThreads);
		
		// Would use ComplatableFuture.completeAsync(...), But, java 8 doesn't have it! :(
		//return future.completeAsync(this::loadFile, fileReaderThreads);
		
		CompletableFuture.supplyAsync(this::loadFile, fileReaderThreads).whenComplete((f, e) -> {
			if (e != null) future.completeExceptionally(e);
			future.complete(f);
		});
		return future;
	}
	
	private LodDataSource loadFile() {
		// TODO
		return null;
	}
	
	

}
