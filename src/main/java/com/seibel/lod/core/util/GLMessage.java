package com.seibel.lod.core.util;

import java.util.HashMap;
import java.util.function.Function;

public final class GLMessage {
	static final String HEADER = "[LWJGL] OpenGL debug message";
	public final GLMessage.Type type;
	public final GLMessage.Severity severity;
	public final GLMessage.Source source;
	public final String id;
	public final String message;
	GLMessage(GLMessage.Type t, GLMessage.Severity s, GLMessage.Source sr, String id, String ms) {
		this.type = t;
		this.source = sr;
		this.severity = s;
		this.id = id;
		this.message = ms;
	}
	@Override
	public String toString() {
		return "[level:"+severity+", type:"+type+", source:"+source+", id:"+id+", msg:{"+message+"}]";
	}
	
	
	public enum Type {
		ERROR,
		DEPRECATED_BEHAVIOR,
		UNDEFINED_BEHAVIOR,
		PORTABILITY,
		PERFORMANCE,
		MARKER,
		PUSH_GROUP,
		POP_GROUP,
		OTHER;
		public final String str;
		private Type() {
			str = super.toString().toUpperCase();
		}
		@Override
		public final String toString() {
			return str;
		}
	    static final HashMap<String, GLMessage.Type> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Type>();
	    	for (GLMessage.Type t : Type.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Type get(String str) {
	    	return toEnum.get(str);
	    }
	}
	public enum Source {
		API,
		WINDOW_SYSTEM,
		SHADER_COMPILER,
		THIRD_PARTY,
		APPLICATION,
		OTHER;
		public final String str;
		private Source() {
			str = super.toString().toUpperCase();
		}
	    static final HashMap<String, GLMessage.Source> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Source>();
	    	for (GLMessage.Source t : Source.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Source get(String str) {
	    	return toEnum.get(str);
	    }
	}
	public enum Severity {
		HIGH, MEDIUM, LOW, NOTIFICATION;
		public final String str;
		private Severity() {
			str = super.toString().toUpperCase();
		}
	    static final HashMap<String, GLMessage.Severity> toEnum;
	    static {
	    	toEnum = new HashMap<String, GLMessage.Severity>();
	    	for (GLMessage.Severity t : Severity.values()) {
	    		toEnum.put(t.str, t);
	    	}
	    }
	    public final static GLMessage.Severity get(String str) {
	    	return toEnum.get(str);
	    }
	}
	
	public static class Builder {
		GLMessage.Type type;
		GLMessage.Severity severity;
		GLMessage.Source source;
		Function<Type, Boolean> typeFilter;
		Function<Severity, Boolean> severityFilter;
		Function<Source, Boolean> sourceFilter;
		
		String id;
		String message;
		int stage = 0;
		public Builder() {
			this(null, null, null);
		}
		public Builder(
				Function<Type, Boolean> typeFilter,
				Function<Severity, Boolean> severityFilter,
				Function<Source, Boolean> sourceFilter) {
			this.typeFilter = typeFilter;
			this.severityFilter = severityFilter;
			this.sourceFilter = sourceFilter;
		}
		
		public GLMessage add(String str) {
			str = str.strip();
			if (str.isEmpty()) return null;
			boolean b = runStage(str);
			if (b && stage >= 16) {
				stage = 0;
				return new GLMessage(type, severity, source, id, message);
			} else {
				return null;
			}
		}
		
		public void setTypeFilter(Function<Type, Boolean> typeFilter) {
			this.typeFilter = typeFilter;
		}
		public void setSeverityFilter(Function<Severity, Boolean> severityFilter) {
			this.severityFilter = severityFilter;
		}
		public void setSourceFilter(Function<Source, Boolean> sourceFilter) {
			this.sourceFilter = sourceFilter;
		}
		
		private boolean runStage(String str) {
			switch (stage) {
			case 0:
				return checkAndIncStage(str, GLMessage.HEADER);
			case 1:
				return checkAndIncStage(str, "ID");
			case 2:
				return checkAndIncStage(str, ":");
			case 3:
				id = str;
				stage++;
				return true;
			case 4:
				return checkAndIncStage(str, "Source");
			case 5:
				return checkAndIncStage(str, ":");
			case 6:
				source = Source.get(str);
				if (sourceFilter!=null && !sourceFilter.apply(source)) stage = -1;
				stage++;
				return true;
			case 7:
				return checkAndIncStage(str, "Type");
			case 8:
				return checkAndIncStage(str, ":");
			case 9:
				type = Type.get(str);
				if (typeFilter!=null && !typeFilter.apply(type)) stage = -1;
				stage++;
				return true;
			case 10:
				return checkAndIncStage(str, "Severity");
			case 11:
				return checkAndIncStage(str, ":");
			case 12:
				severity = Severity.get(str);
				if (severityFilter!=null && !severityFilter.apply(severity)) stage = -1;
				stage++;
				return true;
			case 13:
				return checkAndIncStage(str, "Message");
			case 14:
				return checkAndIncStage(str, ":");
			case 15:
				message = str;
				stage++;
				return true;
			default: return false;
			}
		}

		private boolean checkAndIncStage(String str, String comp) {
			boolean result = str.equals(comp);
			if (result) stage++;
			return result;
		}
		
	};
}