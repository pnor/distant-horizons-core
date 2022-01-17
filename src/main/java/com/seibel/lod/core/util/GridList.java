package com.seibel.lod.core.util;

import java.util.ArrayList;
import java.util.List;

public class GridList<T> extends ArrayList<T> implements List<T> {

	public static class Pos {
		public final int x;
		public final int y;

		public Pos(int xx, int yy) {
			x = xx;
			y = yy;
		}
	}

	private static final long serialVersionUID = 1585978374811888116L;
	public final int gridCentreToEdge;
	public final int gridSize;

	public GridList(int gridCentreToEdge) {
		super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
		gridSize = gridCentreToEdge * 2 + 1;
		this.gridCentreToEdge = gridCentreToEdge;
	}

	public final T getOffsetOf(int index, int x, int y) {
		return get(index + x + y * gridSize);
	}

	public final int offsetOf(int index, int x, int y) {
		return index + x + y * gridSize;
	}

	public final Pos posOf(int index) {
		return new Pos(index % gridSize, index / gridSize);
	}

	public final int calculateOffset(int x, int y) {
		return x + y * gridSize;
	}

	public final GridList<T> subGrid(int gridCentreToEdge) {
		int centreIndex = size() / 2;
		GridList<T> subGrid = new GridList<T>(gridCentreToEdge);
		for (int oy = -gridCentreToEdge; oy <= gridCentreToEdge; oy++) {
			int begin = offsetOf(centreIndex, -gridCentreToEdge, oy);
			int end = offsetOf(centreIndex, gridCentreToEdge, oy);
			subGrid.addAll(this.subList(begin, end + 1));
		}
		// System.out.println("========================================\n"+
		// this.toDetailString() + "\nTOOOOOOOOOOOOO\n"+subGrid.toDetailString()+
		// "==========================================\n");
		return subGrid;
	}

	@Override
	public String toString() {
		return "GridList " + gridSize + "*" + gridSize + "[" + size() + "]";
	}

	public String toDetailString() {
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		for (T t : this) {
			str.append(t.toString());
			str.append(", ");
			i++;
			if (i % gridSize == 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}
}
