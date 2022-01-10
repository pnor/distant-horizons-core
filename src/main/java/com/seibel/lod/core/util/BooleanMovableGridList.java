package com.seibel.lod.core.util;

/*Layout:
 * 0,1,2,
 * 3,4,5,
 * 6,7,8
 */

public class BooleanMovableGridList {

	private int centerX;
	private int centerY;
	
	public final int gridCentreToEdge;
	public final int gridSize;
	private boolean[] b;

	public BooleanMovableGridList(int gridCentreToEdge, int centerX, int centerY) {
		gridSize = gridCentreToEdge * 2 + 1;
		this.gridCentreToEdge = gridCentreToEdge;
		this.centerX = centerX;
		this.centerY = centerY;
		clear();
	}
	
	public void clear() {
		b = new boolean[gridSize*gridSize];
	}
	
	public int getCenterX() {return centerX;}
	public int getCenterY() {return centerY;}
	
	private void assertIndex(int ix, int iy) {
		if (ix<0 || ix>=gridSize || iy<0 || iy>=gridSize)
			throw new IndexOutOfBoundsException("BooleanMovableGridList index position out of bound");
	}
	
	public boolean isInBound(int x, int y) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return !(x<0 || x>=gridSize || y<0 || y>=gridSize);
	}

	// return onFail if x,y is outside of the grid
	public boolean get(int x, int y) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _getDirect(x,y);
	}
	
	// return false if x,y is outside of the grid
	public void set(int x, int y, boolean t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		_setDirect(x,y,t);
	}
	
	// return onFail if x,y is outside of the grid
	// Otherwise, return the new value (for chaining)
	public boolean setAndGet(int x, int y, boolean t) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		_setDirect(x,y,t);
		return t;
	}
	// return null if x,y is outside of the grid
	// Otherwise, return the old value
	public boolean swap(int x, int y, boolean t, boolean onFail) {
		x = x-centerX+gridCentreToEdge;
		y = y-centerY+gridCentreToEdge;
		return _swapDirect(x,y, t);
	}
	
	private final boolean _getDirect(int x, int y) {
		assertIndex(x,y);
		return b[x + y * gridSize];
	}
	private final void _setDirect(int x, int y, boolean t) {
		assertIndex(x,y);
		b[x + y * gridSize] = t;
	}
	private final boolean _swapDirect(int x, int y, boolean t) {
		assertIndex(x,y);
		boolean r = b[x + y * gridSize];
		b[x + y * gridSize] = t;
		return r;
	}
	
	interface BoolTransformer {
		boolean transform(boolean oldValue, int x, int y);
	}
	
	// Transform the list via the function. The data can still be accessed
	// inside the function, and the returned value will not be applied
	// until all elements have done the transform.
	public void twoStageTransform(BoolTransformer transformer) {
		boolean[] nb = new boolean[b.length];
		int i=0;
		for (int y=0; y<gridSize; y++) {
			for (int x=0; x<gridSize; x++) {
				nb[i] = transformer.transform(b[i],
						x+centerX-gridCentreToEdge, y+centerY-gridCentreToEdge);
				i++;
			}
		}
		b = nb;
	}

	public void flipBorder(boolean valueToBeFlipped) {
		boolean t = valueToBeFlipped;
		BoolTransformer tran = (v, x, y) -> {
			if (v!=t) return v;
			boolean r = false;
			r |= (isInBound(x-1,y) ? get(x-1,y)==!t : false);
			r |= (isInBound(x,y-1) ? get(x,y-1)==!t : false);
			r |= (isInBound(x+1,y) ? get(x+1,y)==!t : false);
			r |= (isInBound(x,y+1) ? get(x,y+1)==!t : false);
			return r ? !t : t;
		};
		twoStageTransform(tran);
	}
	public void flipBorderCorner(boolean valueToBeFlipped) {
		boolean t = valueToBeFlipped;
		BoolTransformer tran = (v, x, y) -> {
			if (v!=t) return v;
			boolean r = false;
			r |= (isInBound(x-1,y) ? get(x-1,y)==!t : false);
			r |= (isInBound(x,y-1) ? get(x,y-1)==!t : false);
			r |= (isInBound(x+1,y) ? get(x+1,y)==!t : false);
			r |= (isInBound(x,y+1) ? get(x,y+1)==!t : false);
			r |= (isInBound(x-1,y-1) ? get(x-1,y-1)==!t : false);
			r |= (isInBound(x+1,y-1) ? get(x+1,y-1)==!t : false);
			r |= (isInBound(x+1,y+1) ? get(x+1,y+1)==!t : false);
			r |= (isInBound(x-1,y+1) ? get(x-1,y+1)==!t : false);
			return r ? !t : t;
		};
		twoStageTransform(tran);
	}
	public void flipBorderCorner(boolean valueToBeFlipped, int range) {
		boolean t = valueToBeFlipped;
		BoolTransformer tran = (v, x, y) -> {
			if (v!=t) return v;
			boolean r = false;
			for (int dx=-range;dx<=range;dx++)
				for (int dy=-range;dy<=range;dy++)
					r |= (isInBound(x+dx,y+dy) ? get(x+dx,y+dy)==!t : false);
			return r ? !t : t;
		};
		twoStageTransform(tran);
	}
	

	// Return false if haven't changed. Return true if it did
	public boolean move(int newCenterX, int newCenterY) {
		return move(newCenterX, newCenterY, false);
	}
	
	// Return false if haven't changed. Return true if it did
	public boolean move(int newCenterX, int newCenterY, boolean value) {
		if (centerX == newCenterX && centerY == newCenterY) return false;
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
			return true;
		}
		centerX = newCenterX;
		centerY = newCenterY;
	
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
		return true;
	}


	@Override
	public String toString() {
		return "MovableGridList[" + centerX + "," + centerY + "] " + gridSize + "*" + gridSize + "[" + b.length + "]";
	}

	public String toDetailString() {
		StringBuilder str = new StringBuilder("\n");
		int i = 0;
		str.append(toString());
		str.append("\n");
		for (boolean t : b) {
			str.append(t ? "#" : ".");
			i++;
			if (i % gridSize == 0) {
				str.append("\n");
			} else {
				//str.append(", ");
			}
		}
		return str.toString();
	}
}
