package org.anddev.andengine.engine.camera;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.engine.camera.hud.HUD;
import org.anddev.andengine.entity.IUpdateHandler;
import org.anddev.andengine.entity.StaticEntity;
import org.anddev.andengine.opengl.GLHelper;
import org.anddev.andengine.physics.collision.CollisionChecker;

import android.opengl.GLU;
import android.util.DisplayMetrics;
import android.view.MotionEvent;

/**
 * @author Nicolas Gramlich
 * @since 10:24:18 - 25.03.2010
 */
public class Camera implements IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private float mMinX;
	private float mMaxX;
	private float mMinY;
	private float mMaxY;

	private HUD mHUD;

	private boolean mFlipped;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Camera(final float pX, final float pY, final float pWidth, final float pHeight) {
		this.set(pX, pX + pWidth, pY, pY + pHeight);
	}
	
	public static Camera createFromDisplayMetrics(final float pCenterX, final float pCenterY, final DisplayMetrics pDisplayMetrics) {
		final float width = pDisplayMetrics.widthPixels;
		final float height = pDisplayMetrics.widthPixels;
		return new Camera(pCenterX - width / 2, pCenterY - height / 2, width, height);
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public float getMinX() {
		return this.mMinX;
	}

	public float getMaxX() {
		return this.mMaxX;
	}

	public float getMinY() {
		return this.mMinY;
	}

	public float getMaxY() {
		return this.mMaxY;
	}

	public float getWidth() {
		return this.mMaxX - this.mMinX;
	}

	public float getHeight() {
		return this.mMaxY - this.mMinY;
	}

	public float getCenterX() {
		return this.mMinX + (this.mMaxX - this.mMinX) / 2;
	}

	public float getCenterY() {
		return this.mMinY + (this.mMaxY - this.mMinY) / 2;
	}

	public void setCenter(final float pCenterX, final float pCenterY) {
		final float dX = pCenterX - this.getCenterX();
		final float dY = pCenterY - this.getCenterY();

		this.mMinX += dX;
		this.mMaxX += dX;
		this.mMinY += dY;
		this.mMaxY += dY;
	}

	private void set(final float pMinX, final float pMaxX, final float pMinY, final float pMaxY) {
		this.mMinX = pMinX;
		this.mMaxX = pMaxX;
		this.mMinY = pMinY;
		this.mMaxY = pMaxY;
	}

	public HUD getHUD() {
		return this.mHUD;
	}

	public void setHUD(final HUD pHUD) {
		this.mHUD = pHUD;
	}

	public boolean hasHUD() {
		return this.mHUD != null;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		if(this.mHUD != null) {
			this.mHUD.onUpdate(pSecondsElapsed);
		}
	}

	// ===========================================================
	// Methods
	// ===========================================================

	private float relativeToAbsoluteX(final float pRelativeX) {
		return this.mMinX + pRelativeX * (this.mMaxX - this.mMinX);
	}

	private float relativeToAbsoluteY(final float pRelativeY) {
		return this.mMinY + pRelativeY * (this.mMaxY - this.mMinY);
	}

	public void flip() {
		this.mFlipped = !this.mFlipped;
	}

	public void onDrawHUD(final GL10 pGL) {
		if(this.mHUD != null) {
			this.onApplyHUDMatrix(pGL);
			GLHelper.setModelViewIdentityMatrix(pGL);
			this.mHUD.onDraw(pGL);
		}
	}

	public boolean isEntityVisible(final StaticEntity pStaticEntity) {
		final float otherLeft = pStaticEntity.getX();
		final float otherTop = pStaticEntity.getY();
		final float otherRight = pStaticEntity.getWidth() + otherLeft;
		final float otherBottom = pStaticEntity.getHeight() + otherTop;

		return CollisionChecker.checkAxisAlignedBoxCollision(this.mMinX, this.mMinY, this.mMaxX, this.mMaxY, otherLeft, otherTop, otherRight, otherBottom);
	}

	public void onApplyMatrix(final GL10 pGL) {
		pGL.glMatrixMode(GL10.GL_PROJECTION);
		pGL.glLoadIdentity();

		GLU.gluOrtho2D(pGL, this.mMinX, this.mMaxX, this.mMaxY, this.mMinY);

		if(this.mFlipped) {
			this.rotateHalfAround(pGL, this.getCenterX(), this.getCenterY());
		}
	}

	public void onApplyHUDMatrix(final GL10 pGL) {
		pGL.glMatrixMode(GL10.GL_PROJECTION);
		pGL.glLoadIdentity();

		GLU.gluOrtho2D(pGL, 0, this.getWidth(), this.getHeight(), 0);

		if(this.mFlipped) {
			this.rotateHalfAround(pGL, this.getWidth() / 2, this.getHeight() / 2);
		}
	}

	private void rotateHalfAround(final GL10 pGL, final float pCenterX, final float pCenterY) {
		pGL.glTranslatef(pCenterX, pCenterY, 0);
		pGL.glRotatef(180, 0, 0, 1);
		pGL.glTranslatef(-pCenterX, -pCenterY, 0);
	}

	public void convertSceneToHUDMotionEvent(final MotionEvent pSceneMotionEvent) {
		final float x = pSceneMotionEvent.getX() - this.mMinX;
		final float y = pSceneMotionEvent.getY() - this.mMinY;
		pSceneMotionEvent.setLocation(x, y);
	}

	public void convertHUDToSceneMotionEvent(final MotionEvent pHUDMotionEvent) {
		final float x = pHUDMotionEvent.getX() + this.mMinX;
		final float y = pHUDMotionEvent.getY() + this.mMinY;
		pHUDMotionEvent.setLocation(x, y);
	}

	public void convertSurfaceToSceneMotionEvent(final MotionEvent pSurfaceMotionEvent, final int pSurfaceWidth, final int pSurfaceHeight) {
		final float x;
		final float y;

		if(this.mFlipped) {
			x = this.relativeToAbsoluteX(1 - (pSurfaceMotionEvent.getX() / pSurfaceWidth));
			y = this.relativeToAbsoluteY(1 - (pSurfaceMotionEvent.getY() / pSurfaceHeight));
		} else {
			x = this.relativeToAbsoluteX(pSurfaceMotionEvent.getX() / pSurfaceWidth);
			y = this.relativeToAbsoluteY(pSurfaceMotionEvent.getY() / pSurfaceHeight);
		}

		pSurfaceMotionEvent.setLocation(x, y);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}