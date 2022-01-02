package com.seibel.lod.core.util;

import java.util.ArrayList;
import java.util.List;

/*Layout:
 * 0,1,2,
 * 3,4,5,
 * 6,7,8
 */

public class MovableGridList<T> extends ArrayList<T> implements List<T> {

	private static final long serialVersionUID = 5366261085254591277L;

	public static class Pos {
		public int x;
		public int y;

		public Pos(int xx, int yy) {
			x = xx;
			y = yy;
		}
	}

	private int centerX;
	private int centerY;
	
	public final int gridCentreToEdge;
	public final int gridSize;

	public MovableGridList(int gridCentreToEdge, int centerX, int centerY) {
		super((gridCentreToEdge * 2 + 1) * (gridCentreToEdge * 2 + 1));
		gridSize = gridCentreToEdge * 2 + 1;
		this.gridCentreToEdge = gridCentreToEdge;
		this.centerX = centerX;
		this.centerY = centerY;
		clear();
	}
	
	@Override
	public void clear() {
		super.clear();
		super.ensureCapacity(gridSize*gridSize);
		for (int i=0; i<gridSize*gridSize; i++) {
			super.add(null);
		}
	}
	
	public int getCenterX() {return centerX;}
	public int getCenterY() {return centerY;}

	// return null if x,y is outside of the grid
	public T get(int x, int y) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _getDirect(x,y);
	}
	
	// return null if x,y is outside of the grid
	public T setAndGet(int x, int y, T t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _setDirect(x,y, t) ? t : null;
	}
	
	private final T _getDirect(int x, int y) {
		if (x<0 || x>=gridSize || y<0 || y>=gridSize) return null;
		return super.get(x + y * gridSize);
	}
	private final boolean _setDirect(int x, int y, T t) {
		if (x<0 || x>=gridSize || y<0 || y>=gridSize) return false;
		super.set(x + y * gridSize, t);
		return true;
	}
	
	public void move(int newCenterX, int newCenterY) {
		if (centerX == newCenterX && centerY == newCenterY) return;
		int deltaX = newCenterX - centerX;
		int deltaY = newCenterY - centerY;
		
		// if the x or z offset is equal to or greater than
		// the total width, just delete the current data
		// and update the centerX and/or centerZ
		if (Math.abs(deltaX) >= gridSize || Math.abs(deltaY) >= gridSize)
		{
			clear();
			// update the new center
			centerX = newCenterX;
			centerY = newCenterY;
			return;
		}
	
		// X
		if (deltaX >= 0 && deltaY >= 0)
		{
			// move everything over to the left-up (as the center moves to the right-down)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX < 0 && deltaY >= 0)
		{
			// move everything over to the right-up (as the center moves to the left-down)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = 0; y < gridSize; y++)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else if (deltaX >= 0 && deltaY < 0)
		{
			// move everything over to the left-down (as the center moves to the right-up)
			for (int x = 0; x < gridSize; x++)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		else //if (deltaX < 0 && deltaY < 0)
		{
			// move everything over to the right-down (as the center moves to the left-up)
			for (int x = gridSize - 1; x >= 0; x--)
			{
				for (int y = gridSize - 1; y >= 0; y--)
				{
					_setDirect(x, y, _getDirect(x+deltaX, y+deltaY));
				}
			}
		}
		centerX = newCenterX;
		centerY = newCenterY;
	}
	

	// TODO: This is unused but may be useful later on.
	/*
	public final MovableGridList<T> subGrid(int gridCentreToEdge, int newCenterX, int newCenterY) {
	}*/

	@Override
	public String toString() {
		return "MovableGridList[" + centerX + "," + centerY + "] " + gridSize + "*" + gridSize + "[" + size() + "]";
	}

	public String toDetailString() {
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		for (T t : this) {
			
			str.append(t!=null ? t.toString() : "NULL");
			str.append(", ");
			i++;
			if (i % gridSize == 0) {
				str.append("\n");
			}
		}
		return str.toString();
	}
}
